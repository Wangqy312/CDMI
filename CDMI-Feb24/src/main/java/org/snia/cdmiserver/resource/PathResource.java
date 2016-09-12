/*
 * Copyright (c) 2010, Sun Microsystems, Inc.
 * Copyright (c) 2010, The Storage Networking Industry Association.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of The Storage Networking Industry Association (SNIA) nor
 * the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 *  THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.snia.cdmiserver.resource;

import crypto.CryptoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.security.Key;
import java.util.Random;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;

import org.snia.cdmiserver.dao.ContainerDao;
import org.snia.cdmiserver.dao.DataObjectDao;
import org.snia.cdmiserver.model.Container;
import org.snia.cdmiserver.model.DataObject;
import org.snia.cdmiserver.util.JsonUtils;
import org.snia.cdmiserver.util.MediaTypes;
import org.snia.cdmiserver.util.ObjectID;
import org.snia.cdmiserver.util.RandomStringUtils;


/**
 * <p>
 * Access to objects by path.
 * </p>
 */
public class PathResource {
    private static final Logger LOG = LoggerFactory.getLogger(PathResource.class);

    //
    // Properties and Dependency Injection Methods
    //
    private ContainerDao containerDao;

    /**
     * <p>
     * Injected {@link ContainerDao} instance.
     * </p>
     */
    public void setContainerDao(
            ContainerDao containerDao) {
        this.containerDao = containerDao;
    }

    private DataObjectDao dataObjectDao;

    /**
     * <p>
     * Injected {@link DataObjectDao} instance.
     * </p>
     */
    public void setDataObjectDao(
            DataObjectDao dataObjectDao) {
        this.dataObjectDao = dataObjectDao;
    }

    //
    // Resource Methods
    //
    /**
     * <p>
     * [8.8] Delete a Data Object and
     * [9.7] Delete a Container Object
     * </p>
     *
     * @param path
     *            Path to the existing object
     */
    @DELETE
    @Path("/{path:.+}")
    public Response deleteDataObjectOrContainer(
            @PathParam("path") String path) {

        try {
            containerDao.deleteByPath(path);
            return Response.ok().header(
                    "X-CDMI-Specification-Version", "1.0.2").build();
        } catch (Exception ex) {
            LOG.error("Delete error", ex);
            return Response.status(Response.Status.BAD_REQUEST).tag(
                    "Object Delete Error : " + ex.toString()).build();
        }
    }

    /**
     * <p>
     * Trap to catch attempts to delete the root container
     * </p>
     *
     * @param path
     *            Path to the existing data object
     */
    @DELETE
    @Path("/")
    public Response deleteRootContainer(@PathParam("path") String path) {
        return Response.status(Response.Status.BAD_REQUEST).tag(
                "Can not delete root container").build();
    }

    /**
     * <p>
     * [9.4] Read a Container Object (CDMI Content Type)
     * [8.4] Read a Data Object (CDMI Content Type)
     * </p>
     *
     * @param path
     *            Path to the existing non-root container
     */

    //Use the method "getPlainDataObjectOrContainer"
    //abandoned
//    @GET
//    @Path("/{path:.+}")
//    @Consumes(MediaTypes.DATA_OBJECT)
    public Response getContainerOrDataObject(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {

        LOG.trace("In PathResource.getContainerOrObject, path={}", path);
        //print headers for debug
        if (LOG.isDebugEnabled()) {
            for (String hdr : headers.getRequestHeaders().keySet()) {
                LOG.debug("Hdr: {} - {}", hdr, headers.getRequestHeader(hdr));
            }
        }

        if (headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).isEmpty()) {  //to check 
          return getDataObjectOrContainer(path,headers);
        }

        // Check for container vs object
        if (containerDao.isContainer(path)) {
          // if container build container browser page
          try {
            Container container = containerDao.findByPath(path);
            if (container == null) {
              return Response.status(Response.Status.NOT_FOUND).build();
            } else {
              String respStr = container.toJson(false);
              return Response.ok(respStr).header(
                      "X-CDMI-Specification-Version", "1.0.2").build();
            }
          } catch (Exception ex) {
            LOG.error("Failed to find container", ex);
            return Response.status(Response.Status.NOT_FOUND).tag(
                    "Container Read Error : " + ex.toString()).build();
          }
        }
        try {
          DataObject dObj = dataObjectDao.findByPath(path);
          if (dObj == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
          } else {
            // make http response
            // build a JSON representation
            String respStr = dObj.toJson();
            //ResponseBuilder builder = Response.status(Response.Status.CREATED)
            return Response.ok(respStr).header(
                    "X-CDMI-Specification-Version", "1.0.2").build();
          } // if/else
        } catch (Exception ex) {
            LOG.error("Failed to find data object", ex);
          return Response.status(Response.Status.BAD_REQUEST).tag(
                  "Object Fetch Error : " + ex.toString()).build();
        }
    }

    //Use the method "getPlainDataObjectOrContainer"
    //abandoned
//    @GET
//    @Path("/{path:.+}")
//    @Consumes(MediaTypes.ENCRYPTED_OBJECT)
    public Response getEncContainerOrDataObject(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {

        LOG.trace("In PathResource.getContainerOrObject, path={}", path);
        //print headers for debug
        if (LOG.isDebugEnabled()) {
            for (String hdr : headers.getRequestHeaders().keySet()) {
                LOG.debug("Hdr: {} - {}", hdr, headers.getRequestHeader(hdr));
            }
        }

        if (headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).isEmpty()) {  //to check 
          return getDataObjectOrContainer(path,headers);
        }

        // Check for container vs object
        if (containerDao.isContainer(path)) {
          // if container build container browser page
          try {
            Container container = containerDao.findByPath(path);
            if (container == null) {
              return Response.status(Response.Status.NOT_FOUND).build();
            } else {
              String respStr = container.toJson(false);
              return Response.ok(respStr).header(
                      "X-CDMI-Specification-Version", "1.0.2").build();
            }
          } catch (Exception ex) {
            LOG.error("Failed to find container", ex);
            return Response.status(Response.Status.NOT_FOUND).tag(
                    "Container Read Error : " + ex.toString()).build();
          }
        }
        try {
          DataObject dObj = dataObjectDao.findByPath(path);
          if (dObj == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
          } else {
            // make http response
            // build a JSON representation
            String respStr = decryptData(dObj, path, false).toJson();
            //ResponseBuilder builder = Response.status(Response.Status.CREATED)
            return Response.ok(respStr).header(
                    "X-CDMI-Specification-Version", "1.0.2").build();
          } // if/else
        } catch (Exception ex) {
            LOG.error("Failed to find data object", ex);
          return Response.status(Response.Status.BAD_REQUEST).tag(
                  "Object Fetch Error : " + ex.toString()).build();
        }
    }
    
        
    @GET
    @Path("/{path:.+}")
    @Consumes(MediaTypes.DATA_OBJECT)
    public Response getPlainDataObjectOrContainer(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {
        if (headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).isEmpty()) {  //to check 
            return getDataObjectOrContainer(path, headers);
        } 
                // Check for container vs object
        if (containerDao.isContainer(path)) {
          // if container build container browser page
          try {
            Container container = containerDao.findByPath(path);
            if (container == null) {
              return Response.status(Response.Status.NOT_FOUND).build();
            } else {
              String respStr = container.toJson(false);
              return Response.ok(respStr).header(
                      "X-CDMI-Specification-Version", "1.0.2").build();
            }
          } catch (Exception ex) {
            LOG.error("Failed to find container", ex);
            return Response.status(Response.Status.NOT_FOUND).tag(
                        "Container Read Error : " + ex.toString()).build();
            }
        }

        try {
            DataObject dObj = dataObjectDao.findByPath(path);
            if (dObj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {
                //get the mimetype to judge whether it is needed to decrypt the object before reponse
                String mimeType = dObj.getMimetype();
                String respStr = new String();
                if(mimeType.equals("text/plain")){
                    respStr = dObj.toJson();
                }else if (mimeType.equals("application/jose+json")) {
                    respStr = decryptData(dObj, path, false).toJson();
                }
                // make http response
                // build a JSON representation
                //String respStr = dObj.toJson();
            //ResponseBuilder builder = Response.status(Response.Status.CREATED)
            return Response.ok(respStr).header(
                    "X-CDMI-Specification-Version", "1.0.2").build();
          } // if/else
        } catch (Exception ex) {
            LOG.error("Failed to find data object", ex);
          return Response.status(Response.Status.BAD_REQUEST).tag(
                  "Object Fetch Error : " + ex.toString()).build();
        }
    }

    
    
    /**
     * <p>
     * [9.4] Read a Container Object (CDMI Content Type).
     *       Catches request routing for root container in spite of CXF bug.
     * </p>
     *
     * @param path
     *            Path to the root container
     */
    @GET
    @Path("/")
    @Consumes(MediaTypes.CONTAINER)
    public Response getRootContainer(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {

        LOG.trace("In PathResource.getRootContainer");
        System.out.println("Xavier:debug get root");
         System.out.println("Im getRootContainer -------");
        //print headers for debug
        if (LOG.isDebugEnabled()) {
            for (String hdr : headers.getRequestHeaders().keySet()) {
                LOG.debug("Hdr: {} - {}", hdr, headers.getRequestHeader(hdr));
            }
        }
        System.out.println("getRootContainer path:" + path);
        return getContainerOrDataObject(path, headers);

    }

    /**
     * <p>
     * [8.5] Read a Data Object (Non-CDMI Content Type)
     * [9.5] Read a Container Object (Non-CDMI Content Type)
     *  or decrypt an encrypted data object
     * </p>
     *
     * <p>
     * IMPLEMENTATION NOTE - Consult <code>uriInfo.getQueryParameters()</code> to identify
     * restrictions on the returned information.
     * </p>
     *
     * <p>
     * IMPLEMENTATION NOTE - If the path points at a container,
     * the response content type must be"text/json".
     * </p>
     *
     * @param path
     *            Path to the existing data object or container
     * @param range
     *            Range header value (if specified), else empty string
     */
    //@GET
    //@Path("/{path:.+}")
    public Response getDataObjectOrContainer(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {

        LOG.trace("In PathResource.getDataObjectOrContainer, path: {}", path);

        boolean NonCDMI = true;
       
        // print headers for debug
        for (String hdr : headers.getRequestHeaders().keySet()) {
            if (hdr.equals("x-cdmi-specification-version")) {
                NonCDMI = false;
            }
            LOG.debug("Hdr: {} - {}", hdr, headers.getRequestHeader(hdr));
        }
        if (path == null && NonCDMI) {
            path = new String("/index.html");
        }
        // Check for container vs object
        if (containerDao.isContainer(path)) {
            // if container build container browser page
            try {
                Container container = containerDao.findByPath(path);
                if (container == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else {
                    String respStr = container.toJson(false);
                    return Response.ok(respStr).header(
                            "X-CDMI-Specification-Version", "1.0.2").build();
                }
            } catch (Exception ex) {
                LOG.error("Failed to find container", ex);
                return Response.status(Response.Status.NOT_FOUND)
                        .tag("Container Read Error : " + ex.toString()).build();
            }
        } else {
            // if object, send out the object in it's native form
            try {
                DataObject dObj = dataObjectDao.findByPath(path);
                if (dObj == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                } else {
                    
                    
                    //Xavier: handling encrypted data object
                    if("application/jose+json".equals(dObj.getMimetype()))
                    {
                         CryptoProvider cp = new CryptoProvider();
                    
                    
                         cp.setSymkey("{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2I\"}");
                         cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
                         cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);
        
                         String cipher = dObj.getValue();
                         String plain = cp.doJWEDecrypt(cipher);
                        //we change the content of the data object into an "encrypted" one here.
                        dObj.setMimetype("text/plain");
                    
                    
                        dObj.setValue(plain);
                        dataObjectDao.modifyDataObject(path, dObj);

                    
                    
                        return Response.ok(plain).type(dObj.getMimetype()).header(
                            "X-CDMI-Specification-Version", "1.0.2").build();
                      
                        
                    }
                    else  //stored data object is a text/plain
                    {   // make http response
                        // build a JSON representation
                        String respStr = dObj.getValue();// dObj.toJson();
                        LOG.trace("MimeType = {}", dObj.getMimetype());
                        return Response.ok(respStr).type(dObj.getMimetype()).header(
                            "X-CDMI-Specification-Version", "1.0.2").build();
                    }
                } // if/else
            } catch (Exception ex) {
                LOG.error("Failed to find data object", ex);
                return Response.status(Response.Status.BAD_REQUEST)
                        .tag("Object Fetch Error : " + ex.toString()).build();
            }
        }
    }

    /**
     * <p>
     * [9.2] Create a Container (CDMI Content Type) and
     * [9.6] Update a Container (CDMI Content Type)
     * </p>
     *
     * @param path
     *            Path to the new or existing container
     * @param noClobber
     *            Value of the no-clobber header (or "false" if not present)
     * @param mustExist
     *            Value of the must-exist header (or "false" if not present)
     */
    @PUT
    @Path("/{path:.+}/")
    @Consumes(MediaTypes.CONTAINER)
    @Produces(MediaTypes.CONTAINER)
    public Response putContainer(
            @PathParam("path") String path,
            @HeaderParam("X-CDMI-NoClobber") @DefaultValue("false") String noClobber,
            @HeaderParam("X-CDMI-MustExist") @DefaultValue("false") String mustExist,
            byte[] bytes) {
          
        LOG.trace("In PathResource.putContainer, path is: {}", path);

        if (LOG.isTraceEnabled()) {
            String inBuffer = new String(bytes);
            LOG.trace("Request = {}", inBuffer);
        }
              
        Container containerRequest = new Container();
        
        try {
            containerRequest.fromJson(bytes, false);
            Container container = containerDao.createByPath(path,
                    containerRequest);
            if (container == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                // make http response
                // build a JSON representation
                String respStr = container.toJson(false);
                ResponseBuilder builder = Response.created(new URI(path));
                builder.header("X-CDMI-Specification-Version", "1.0.2");
                //ResponseBuilder builder = Response.status(Response.Status.CREATED);
                return builder.entity(respStr).build();
                /* return Response.created(respStr).header(
                        "X-CDMI-Specification-Version", "1.0.2").build(); */
            } // if/else
        } catch (Exception ex) {
            LOG.error("Failed to find container", ex);
            return Response.status(Response.Status.BAD_REQUEST)
                    .tag("Object Creation Error : " + ex.toString()).build();
        }
    }

    /**
     * <p>
     * [8.2] Create a Data Object (CDMI Content Type)
     * [8.6] Update Data Object (CDMI Content Type)
     * </p>
     *
     * @param path
     *            Path to the parent container for the new data object
     * @param mediaType
     *            Declared media type of the data object
     * @param dataObject
     *            Raw content of the new data object
     */
    @PUT
    @Path("/{path:.+}")
    @Consumes(MediaTypes.DATA_OBJECT)
    @Produces(MediaTypes.DATA_OBJECT)
    public Response putDataObject(
            @Context HttpHeaders headers,
            @PathParam("path") String path,
            byte[] bytes) {

        System.out.println(path);      
        try {
            DataObject dObj = dataObjectDao.findByPath(path);
            if (dObj == null) {
                dObj = new DataObject();

                dObj.setObjectType("application/cdmi-object");
                // parse json
                dObj.fromJson(bytes, false);
                if (dObj.getValue() == null) {
                    dObj.setValue("== N/A ==");
                }
                dObj = dataObjectDao.createByPath(path, dObj);
                // return representation
                String respStr = dObj.toJson();
                
                return Response.created(URI.create(path)).
                        header("X-CDMI-Specification-Version", "1.0.2").
                        entity(respStr).
                        build();
            }
            return Response.status(Response.Status.BAD_REQUEST).tag(
                  "Object PUT Error : Object exists with this name").build();
        } catch (Exception ex) {
            LOG.error("Failed to find the data object", ex);
            return Response.status(Response.Status.BAD_REQUEST).tag(
                  "Object PUT Error : " + ex.toString()).build();
        }
    }

    /**
     * <p>
     * [8.3] Create a new Data Object (Non-CDMI Content Type) and
     * [8.7] Update a Data Object (Non-CDMI Content Type)
     * </p>
     *
     * @param path
     *            Path to the new or existing data object
     * @param mediaType
     *            Declared media type of the data object
     * @param dataObject
     *            Raw content of the new data object
     */
    @PUT
    @Path("/{path:.+}")
    public Response putDataObject(
            @PathParam("path") String path,
            @HeaderParam("Content-Type") String contentType,
            byte[] bytes) {
        LOG.trace("Non-CDMI putDataObject(): type={}, size={}, path={}",
                contentType, bytes.length, path); 
     
        try {
            DataObject dObj = dataObjectDao.findByPath(path);
            if (dObj == null) {
                dObj = new DataObject();

                dObj.setObjectType("application/cdmi-object");
                // parse json
                //dObj.fromJson(bytes, false);
                if (dObj.getValue() == null) {
                    dObj.setValue(bytes);
                }
                LOG.trace("Calling createNonCDMIByPath");
                dObj = dataObjectDao.createNonCDMIByPath(path, contentType, dObj);
                // return representation
                //String respStr = dObj.toJson();
                //return Response.ok(respStr).header(
                 //       "X-CDMI-Specification-Version", "1.0.2").build();
            }
            //dObj.fromJson(bytes,false);
            return Response.created(URI.create(path)).build();
        } catch (Exception ex) {
            LOG.error("Failed to find data object", ex);
            return Response.status(Response.Status.BAD_REQUEST).tag(
                  "Object PUT Error : " + ex.toString()).build();
        }
        //throw new UnsupportedOperationException(
         //       "PathResource.putDataObject(Non-CDMI Content Type");
    }

    /**
     * <p>
     * [9.10] Create a New Data Object (NON-CDMI Content Type)
     * </p>
     *
     * @param
     *      path Path to the new or existing data object
     * @param
     *      object value
     */
    @Path("/{path:.+}")
    @POST
    public Response postDataObject(
            @PathParam("path") String path,
            byte[] bytes) {

        String inBuffer = new String(bytes);
        LOG.trace("Path = {} {}", path, inBuffer);

        boolean containerRequest = false;
        if (containerDao.isContainer(path)) {
            containerRequest = true;
        }

        try {
            String objectId = ObjectID.getObjectID(11);
            String objectPath = path + "/" + objectId;

            DataObject dObj = new DataObject();
            dObj.setObjectID(objectId);
            dObj.setObjectType(objectPath);
            dObj.setValue(inBuffer);

            LOG.trace("objectId = {}, objecctPath = {}", objectId,
                    objectPath);

            dObj = dataObjectDao.createByPath(objectPath, dObj);

            if (containerRequest) {
                return Response.created(URI.create(path)).header("Location",
                        dObj.getObjectType()).build();
            }
            return Response.created(URI.create(path)).build();
        } catch (Exception ex) {
            LOG.error("Failed to create data object", ex);
            return Response.status(Response.Status.BAD_REQUEST).
              tag("Object Creation Error : " + ex.toString()).build();
        }
    }


  
    
    /**
     * encrypt a data object
     * returns no content
     */
    //encrypt an existing plain object or decrypt an existing encrypted object
    @POST
    @Path("/{path:.+}")
    @Consumes(MediaTypes.DATA_OBJECT) //here input mime type is application/jwe
    public Response postToEndeDataObject(
            @Context HttpHeaders headers,
            @PathParam("path") String path,
            byte[] bytes) {
        File objFile, metadataFile, baseDirectory;
        //if not a container, but a object

        if (!containerDao.isContainer(path)) {
            try {
                DataObject doj = dataObjectDao.findByPath(path);
                //doj.fromJson(bytes, false);
                if (doj.getValue() != null) {
                    DataObject tmp = new DataObject();
                    tmp.fromJson(bytes, false);
                    String mime = tmp.getMimetype();
                    if (mime.equals("text/plain")) {
                        decryptData(doj, path, true);
                        return Response.created(URI.create(path)).build();
                    } else if (mime.equals("application/jose+json")) {
                        String key = RandomStringUtils.getRandomString(43);
                        encryptData(doj, path, key);
//                        String plaintext = doj.getValue();    //do encryption with the data object specified by @path
//                        CryptoProvider cp = new CryptoProvider();
//                        //read key id from the params
//                        String key = RandomStringUtils.getRandomString(43);
//                        //cp.setSymkey("{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2D\"}");
//                        cp.setSymkey("{\"kty\":\"oct\",\"k\":\"" + key + "\"}");
//                        cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
//                        cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);
//
//                        String cip = cp.doJWEEncrypt(plaintext);
//                        //we change the content of the data object into an "encrypted" one here.
//                        doj.setMimetype("application/jose+json"); //set the mime type here to encrypted format
//                        //save the key into de metadata of encrypted object
//                        doj.setMetadata("key", key);
//                        doj.setValue(cip);
//                        dataObjectDao.modifyDataObject(path, doj);
//
//                        System.out.println("XXX: " + cip);
//                        String plain = cp.doJWEDecrypt(cip);
//                        System.out.println("XXX: " + plain);

                        return Response.created(URI.create(path)).build();
                    }
                }
               
                
           }
           catch(Exception ex)
           {
                LOG.error("Failed to find data object", ex);
                return Response.status(Response.Status.BAD_REQUEST)
                        .tag("Object Fetch Error : " + ex.toString()).build();
           }
           
        }  
        else
        {
            System.out.println("This is a container!");
        }
        //return null;
         return Response.status(Response.Status.BAD_REQUEST).build();
    }
     
    //update the plain object or encrypted object with my own plain object or encrypted object
    @POST
    @Path("/{path:.+}")
    @Consumes(MediaTypes.ENCRYPTED_OBJECT)
    public Response postMyDataObject(
            @Context HttpHeaders headers,
            @PathParam("path") String path,
            byte[] bytes) {
        if (!containerDao.isContainer(path)) {
            try {
                DataObject doj = dataObjectDao.findByPath(path);
                //mimeRequest means the mimetype of object we use to update the existing object.
                String mimeRequest = JsonUtils.getValue(bytes, "mimetype");
                String value = JsonUtils.getValue(bytes, "value");
                if (doj.getMimetype().equals("text/plain")) {
                    if (mimeRequest.equals("text/plain")) {
                        doj.fromJson(bytes, false);
                        dataObjectDao.modifyDataObject(path, doj);
                        return Response.ok().build();
                    }
                } else if (doj.getMimetype().equals("application/jose+json")) {
                    if (mimeRequest.equals("text/plain")) {
                        String keyUsed = doj.getMetadata().get("key");
                        doj = encryptData(doj, path, keyUsed, value);
                        return Response.ok().build();
                    } else if (mimeRequest.equals("application/jose+json")) {
                        String key = JsonUtils.getValue(bytes, "key");
                        doj.setMetadata("key", key);
                        doj.setValue(value);
                        dataObjectDao.modifyDataObject(path, doj);
                        return Response.ok().build();
                    }
                }

                
//                if (doj.getValue() != null) {
//                    DataObject tmp = new DataObject();
//                    tmp.fromJson(bytes, false);
//                    String mime = tmp.getMimetype();
//                    if (mime.equals("text/plain")) {
//                        decryptData(doj, path, true);
//                        return Response.created(URI.create(path)).build();
//                    } else if (mime.equals("application/jose+json")) {
//                        String plaintext = doj.getValue();    //do encryption with the data object specified by @path
//                        CryptoProvider cp = new CryptoProvider();
//                        //read key id from the params
//                        String key = RandomStringUtils.getRandomString(43);
//                        //cp.setSymkey("{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2D\"}");
//                        cp.setSymkey("{\"kty\":\"oct\",\"k\":\"" + key + "\"}");
//                        cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
//                        cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);
//
//                        String cip = cp.doJWEEncrypt(plaintext);
//                        //we change the content of the data object into an "encrypted" one here.
//                        doj.setMimetype("application/jose+json"); //set the mime type here to encrypted format
//                        //save the key into de metadata of encrypted object
//                        doj.setMetadata("key", key);
//                        doj.setValue(cip);
//                        dataObjectDao.modifyDataObject(path, doj);
//
//                        System.out.println("XXX: " + cip);
//                        String plain = cp.doJWEDecrypt(cip);
//                        System.out.println("XXX: " + plain);
//
//                        return Response.created(URI.create(path)).build();
//                    }
//                }
               
                
           }
           catch(Exception ex)
           {
                LOG.error("Failed to find data object", ex);
                return Response.status(Response.Status.BAD_REQUEST)
                        .tag("Object Fetch Error : " + ex.toString()).build();
           }
           
        }  
        else
        {
            System.out.println("This is a container!");
        }
        //return null;
         return Response.status(Response.Status.BAD_REQUEST).build();
    }

    private DataObject decryptData(DataObject doj, String path, Boolean modify) {
        String cipherText = doj.getValue();    //do decryption with the data object specified by @path
        CryptoProvider cp = new CryptoProvider();
        //read key id from the params
        String key = doj.getMetadata().get("key");
        cp.setSymkey("{\"kty\":\"oct\",\"k\":\"" + key + "\"}");
        cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);

        String plain = cp.doJWEDecrypt(cipherText);
        doj.setMimetype("text/plain"); //set the mime type here to decrypted format
        doj.getMetadata().remove("key");
        doj.setValue(plain);
        if (modify == false) {
            doj.setMetadata("cdmi_size", Integer.toString(doj.getValue().length()));
        } else {
            dataObjectDao.modifyDataObject(path, doj);
        }
        return doj;
    }
    
    private DataObject encryptData(DataObject doj, String path, String key, String plainText) {
        doj.setValue(plainText);
        return encryptData(doj, path, key);
    }

    private DataObject encryptData(DataObject doj, String path, String key) {
        String plainText = doj.getValue();    //do decryption with the data object specified by @path
        CryptoProvider cp = new CryptoProvider();
        //read key id from the params
        cp.setSymkey("{\"kty\":\"oct\",\"k\":\"" + key + "\"}");
        cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);

        String cip = cp.doJWEEncrypt(plainText);
        doj.setMimetype("application/jose+json"); //set the mime type here to encrypted format
        doj.setMetadata("key", key);
        doj.setValue(cip);
        dataObjectDao.modifyDataObject(path, doj);
        return doj;

    }

    //decrypt the data object
    //@GET
    //@Path("/{path:.+}")
    //@Consumes(MediaTypes.DATA_OBJECT) //here input mime type is application/jwe
//    public Response decryptDataObject(
//            @Context HttpHeaders headers,
//            @PathParam("path") String path,
//            byte[] bytes) {
//        if (!containerDao.isContainer(path)) {
//            try {
//                DataObject doj = dataObjectDao.findByPath(path);
//                if (doj.getValue() != null) {
//                    String cipherText = doj.getValue();    //do decryption with the data object specified by @path
//                    CryptoProvider cp = new CryptoProvider();
//                    //read key id from the params
//
//                    cp.setSymkey("{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2I\"}");
//                    cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
//                    cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);
//
//                    String plain = cp.doJWEDecrypt(cipherText);
//                    //String cip = cp.doJWEEncrypt(plaintext);
//                    //we change the content of the data object into an "encrypted" one here.
//                    doj.setMimetype("text/plain"); //set the mime type here to decrypted format
//
//                    doj.setValue(plain);
//                    dataObjectDao.modifyDataObject(path, doj);
//
//                    System.out.println("XXX: " + cipherText);
//                    System.out.println("XXX: " + plain);
//
//                    return Response.created(URI.create(path)).build();
//                }
//
//            } catch (Exception ex) {
//                LOG.error("Failed to find data object", ex);
//                return Response.status(Response.Status.BAD_REQUEST)
//                        .tag("Object Fetch Error : " + ex.toString()).build();
//            }
//
//        } else {
//            System.out.println("This is a container!");
//        }
//        //return null;
//        return Response.status(Response.Status.BAD_REQUEST).build();
//    }
    
}
