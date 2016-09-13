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

import java.io.File;
import java.net.URI;
import java.util.List;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import key_management.KMS;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;

import org.snia.cdmiserver.dao.ContainerDao;
import org.snia.cdmiserver.dao.DataObjectDao;
import org.snia.cdmiserver.model.Container;
import org.snia.cdmiserver.model.DataObject;
import org.snia.cdmiserver.util.JsonUtils;
import org.snia.cdmiserver.util.MediaTypes;
import org.snia.cdmiserver.util.ObjectID;


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

    private KMS kmsObj;

    public void setKmsObj(KMS kmsObj) {
        this.kmsObj = kmsObj;
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
            @PathParam("path") String path,
            @Context HttpHeaders headers) {
        try {
            containerDao.deleteByPath(path);
            if (headers.getRequestHeader("X-CDMI-Specification-Version").isEmpty()) {
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                return Response.status(Response.Status.NO_CONTENT).header("X-CDMI-Specification-Version", "1.0.2").build();
            }
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

    
    @GET
    @Path("/{path:.+}")
    public Response getObjOrContainerByHTTP(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {
        if (containerDao.isContainer(path)) {
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
                return Response.status(Response.Status.NOT_FOUND).tag(
                        "Container Read Error : " + ex.toString()).build();
            }
        }
        //get dataObject
        try {
            DataObject dObj = dataObjectDao.findByPath(path);
            if (dObj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<MediaType> typeList = headers.getAcceptableMediaTypes();

            String respStr = new String();
            if (typeList.size() == 2) {
                //get ciphertext
                String authName = headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0);
                if (kmsObj.getKey(dObj, authName) != null) {
                    dObj = decryptData(dObj, path, kmsObj.getKey(dObj, authName), false);
                    respStr = dObj.getValue();
                } else {
                    respStr = dObj.getValue();
                }
            } //get plaintext
            else if (typeList.size() == 1) {
                respStr = dObj.getValue();
            }
            return Response.ok(respStr).type(MediaType.TEXT_PLAIN).build();
        } catch (Exception ex) {
            LOG.error("Failed to find data object", ex);
            return Response.status(Response.Status.BAD_REQUEST).tag(
                    "Object Fetch Error : " + ex.toString()).build();        
        } 
}

    
//    @GET
//    @Path("/{path:.+}")
//    @Produces({MediaTypes.ENCRYPTED_OBJECT})
//    public Response getPlainObjFromEncObjByHTTP(
//            @PathParam("path") String path,
//            @Context HttpHeaders headers) {
//        if (containerDao.isContainer(path)) {
//            try {
//                Container container = containerDao.findByPath(path);
//                if (container == null) {
//                    return Response.status(Response.Status.NOT_FOUND).build();
//                } else {
//                    String respStr = container.toJson(false);
//                    return Response.ok(respStr).header(
//                            "X-CDMI-Specification-Version", "1.0.2").build();
//                }
//            } catch (Exception ex) {
//                return Response.status(Response.Status.NOT_FOUND).tag(
//                        "Container Read Error : " + ex.toString()).build();
//            }
//        }
//        try {
//            DataObject dObj = dataObjectDao.findByPath(path);
//            if (dObj == null) {
//                return Response.status(Response.Status.NOT_FOUND).build();
//            } else if ("text/plain".equals(dObj.getMimetype())) {
//                return Response.status(Response.Status.BAD_REQUEST).tag("The resource you request is plain text.").build();
//            } else {
//                // make http response
//                // build a JSON representation
//                String respStr = dObj.getValue();
//                //ResponseBuilder builder = Response.status(Response.Status.CREATED)
//                return Response.ok(respStr).type(MediaType.TEXT_PLAIN).build();
//            } // if/else
//        } catch (Exception ex) {
//            LOG.error("Failed to find data object", ex);
//            return Response.status(Response.Status.BAD_REQUEST).tag(
//                    "Object Fetch Error : " + ex.toString()).build();
//        }
//    }

    @Deprecated
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
//      @Deprecated
//    //Use the method "getPlainDataObjectOrContainer"
//    //abandoned
////    @GET
////    @Path("/{path:.+}")
////    @Consumes(MediaTypes.ENCRYPTED_OBJECT)
//    public Response getEncContainerOrDataObject(
//            @PathParam("path") String path,
//            @Context HttpHeaders headers) {
//
//        LOG.trace("In PathResource.getContainerOrObject, path={}", path);
//        //print headers for debug
//        if (LOG.isDebugEnabled()) {
//            for (String hdr : headers.getRequestHeaders().keySet()) {
//                LOG.debug("Hdr: {} - {}", hdr, headers.getRequestHeader(hdr));
//            }
//        }
//
//        if (headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).isEmpty()) {  //to check 
//          return getDataObjectOrContainer(path,headers);
//        }
//
//        // Check for container vs object
//        if (containerDao.isContainer(path)) {
//          // if container build container browser page
//          try {
//            Container container = containerDao.findByPath(path);
//            if (container == null) {
//              return Response.status(Response.Status.NOT_FOUND).build();
//            } else {
//              String respStr = container.toJson(false);
//              return Response.ok(respStr).header(
//                      "X-CDMI-Specification-Version", "1.0.2").build();
//            }
//          } catch (Exception ex) {
//            LOG.error("Failed to find container", ex);
//            return Response.status(Response.Status.NOT_FOUND).tag(
//                    "Container Read Error : " + ex.toString()).build();
//          }
//        }
//        try {
//          DataObject dObj = dataObjectDao.findByPath(path);
//          if (dObj == null) {
//            return Response.status(Response.Status.NOT_FOUND).build();
//          } else {
//            // make http response
//            // build a JSON representation
//            String respStr = decryptData(dObj, path, false).toJson();
//            //ResponseBuilder builder = Response.status(Response.Status.CREATED)
//            return Response.ok(respStr).header(
//                    "X-CDMI-Specification-Version", "1.0.2").build();
//          } // if/else
//        } catch (Exception ex) {
//            LOG.error("Failed to find data object", ex);
//          return Response.status(Response.Status.BAD_REQUEST).tag(
//                  "Object Fetch Error : " + ex.toString()).build();
//        }
//    }
    
        
    @GET
    @Path("/{path:.+}")
    @Produces(MediaTypes.DATA_OBJECT)
    public Response getObjOrContainerByCDMI(
            @PathParam("path") String path,
            @Context HttpHeaders headers) {
        if (headers.getRequestHeader(HttpHeaders.ACCEPT).get(0).toString().equals("*/*")) {  //to check 
            return getObjOrContainerByHTTP(path, headers);
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
        //get dataobject
        try {
            DataObject dObj = dataObjectDao.findByPath(path);
            String respStr = new String();
            if (dObj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else if(!headers.getRequestHeader(HttpHeaders.AUTHORIZATION).isEmpty()){
                //
                //get the mimetype to judge whether it is needed to decrypt the object before reponse             
                if (dObj.getMimetype().equals("application/jose+json")) {
                    String authName=headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0);
                    if(kmsObj.getKey(dObj,authName)!=null){
                        respStr = decryptData(dObj, path,kmsObj.getKey(dObj,authName), false).toJson();                                            
                    }
                    else
                        respStr=dObj.toJson();                    
                }                
          }
            //get plaintext
            else{
                respStr = dObj.toJson();         
            }
          return Response.ok(respStr).
                  header("X-CDMI-Specification-Version", "1.0.2").type("application/cdmi-object").
                  build();            
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
//    @GET
//    @Path("/")
//    @Consumes(MediaTypes.CONTAINER)
//    public Response getRootContainer(
//            @PathParam("path") String path,
//            @Context HttpHeaders headers) {
//
//        LOG.trace("In PathResource.getRootContainer");
//        System.out.println("Xavier:debug get root");
//         System.out.println("Im getRootContainer -------");
//        //print headers for debug
//        if (LOG.isDebugEnabled()) {
//            for (String hdr : headers.getRequestHeaders().keySet()) {
//                LOG.debug("Hdr: {} - {}", hdr, headers.getRequestHeader(hdr));
//            }
//        }
//        System.out.println("getRootContainer path:" + path);
//        return getContainerOrDataObject(path, headers);
//
//    }

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
    @Deprecated
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
                    return Response.ok(respStr).type(MediaType.APPLICATION_JSON).header(
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
    public Response putDataObjectByCDMI(
            @Context HttpHeaders headers,
            @PathParam("path") String path,
            byte[] bytes) {
        if (headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).equals("text/plain") || HttpHeaders.CONTENT_TYPE.equals("application/jose+json")) {
            return putDataObjectByHTTP(path, headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).get(0),headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0),bytes);
        }

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
                //if create a ciphertext object
                //
                if(dObj.getMimetype().equals("application/jose+json")){
                            //create key        
                }
                dObj = dataObjectDao.createByPath(path, dObj);
                // return representation
                String respStr = dObj.toJson();
                return Response.created(URI.create(path)).
                        header("X-CDMI-Specification-Version", "1.0.2").
                        type("application/cdmi-object").
                        entity(respStr).
                        build();
            }
                        //update
            DataObject dob=new DataObject();
            dob.fromJson(bytes, true);            
            //if the input is plaintext
            if("text/plain".equals(dob.getMimetype())){                
           
                //existing data object is pliantext
                //plaintext to plaintext
                if ("text/plain".equals(dObj.getMimetype())) {
                    if(JsonUtils.getValue(bytes, "Authorization", false) != null){
                        return Response.status(Response.Status.FORBIDDEN).build();
                    }
                    dataObjectDao.updateDataObject(dob, dObj);
                    dataObjectDao.modifyDataObject(path, dObj);
                    return Response.status(Response.Status.NO_CONTENT).build();
                } else if ("application/jose+json".equals(dObj.getMimetype())) {

                    //existing data object is ciphertext
                    //ciphertext to plaintext
                    String key=kmsObj.getKey(dObj,headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
                    if(key==null){
                        return Response.status(Response.Status.FORBIDDEN).build();                 
                    }
                   else if (JsonUtils.getEntityNum(bytes) > 1) {
                        dataObjectDao.updateDataObject(dob, dObj);
                        encryptData(dObj, path, key);
                        return Response.status(Response.Status.NO_CONTENT).build();
                    } else {
                        //如果request中不包含value值，则执行解密操作                      
                        dObj = decryptData(dObj, path, key, true);
                        this.kmsObj.removeKey(dObj, headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
                        return Response.status(Response.Status.NO_CONTENT).build();
                    }
                }
            }else if("application/jose+json".equals(dob.getMimetype())){

                //ciphertext to ciphertext
                if ("application/jose+json".equals(dObj.getMimetype())) {
//                    String key = kmsObj.getKey(dObj, dObj.getMetadata().get("keyID"));
//                    if (key == null) {
//                        return Response.status(Response.Status.FORBIDDEN).build();
//                    }
//                    dataObjectDao.updateDataObject(dob, dObj);                
//                    dataObjectDao.modifyDataObject(path,dObj);
//                    return Response.status(Response.Status.NO_CONTENT).build();                   
                } //encrypt an existing object
                else {
                    this.kmsObj.createKey(dObj, headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0));
                    dObj = encryptData(dObj, path, this.kmsObj.getKey(dObj, headers.getRequestHeader(HttpHeaders.AUTHORIZATION).get(0)));
                    return Response.status(Response.Status.NO_CONTENT).build();
                }
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
    @Consumes({MediaType.TEXT_PLAIN, MediaTypes.ENCRYPTED_OBJECT})
    public Response putDataObjectByHTTP(
            @PathParam("path") String path,
            @HeaderParam("Content-Type") String contentType,
            @HeaderParam("Authorization") String auth,
            byte[] bytes) {
        LOG.trace("Non-CDMI putDataObject(): type={}, size={}, path={}",
                contentType, bytes.length, path); 
        try {
            DataObject dObj = dataObjectDao.findByPath(path);
            if (dObj == null) {
                dObj = new DataObject();

                dObj.setObjectType(MediaTypes.DATA_OBJECT);
                if (MediaType.TEXT_PLAIN.equals(contentType)) {
                    if (dObj.getValue() == null) {
                        dObj.setValue(bytes);
                    }
                } else if (MediaTypes.ENCRYPTED_OBJECT.equals(contentType)) {
                    dObj.fromJson(bytes, false);
                }
                // parse json
                //dObj.fromJson(bytes, false);

                LOG.trace("Calling createNonCDMIByPath");
                dObj = dataObjectDao.createNonCDMIByPath(path, contentType, dObj);
                return Response.created(URI.create(path)).type(dObj.getMimetype()).build();
            }
            //update                     
            //if the input is plaintext
            if ("text/plain".equals(contentType)) {
                //existing data object is pliantext
                //plaintext to plaintext
                if ("text/plain".equals(dObj.getMimetype())) {
                    if (JsonUtils.getValue(bytes, "Authorization", false) != null) {
                        return Response.status(Response.Status.FORBIDDEN).build();
                    }
                    dObj.setValue(bytes);
                    dataObjectDao.modifyDataObject(path, dObj);
                    return Response.status(Response.Status.NO_CONTENT).build();
                } else if ("application/jose+json".equals(dObj.getMimetype())) {
                    //existing data object is ciphertext
                    String key=kmsObj.getKey(dObj,auth);
                    if(key==null)
                        return Response.status(Response.Status.FORBIDDEN).build();
                    //ciphertext to plaintext
                    if (bytes.length != 0) {
                        dObj.setValue(bytes);
                        encryptData(dObj, path, key);
                        dataObjectDao.modifyDataObject(path, dObj);
                        return Response.status(Response.Status.NO_CONTENT).build();
                    } //如果request中不包含value值，则执行解密操作
                    else {
                        dObj = decryptData(dObj, path, key, true);
                        this.kmsObj.removeKey(dObj, auth);
                        return Response.status(Response.Status.NO_CONTENT).build();
                    }
                }
            }
            //if the input is ciphertext            
            if ("application/jose+json".equals(contentType)) {
                //ciphertext to ciphertext
                if ("application/jose+json".equals(dObj.getMimetype())) {
//                    String key = kmsObj.getKey(dObj, auth);
//                    if (key == null) {
//                        return Response.status(Response.Status.FORBIDDEN).build();
//                    }
//                    dObj.setValue(bytes);
//                    dataObjectDao.modifyDataObject(path, dObj);
//                    return Response.status(Response.Status.NO_CONTENT).build();
                } //encrypt an existing object
                else {
                    this.kmsObj.createKey(dObj, auth);
                    dObj = encryptData(dObj, path, this.kmsObj.getKey(dObj, auth));
                    return Response.status(Response.Status.NO_CONTENT).build();
                }
            }
            return Response.status(Response.Status.BAD_REQUEST).tag(
                    "Object PUT Error").build();
            //return Response.status(Response.Status.BAD_REQUEST).tag(
            //       "Object PUT Error : Object exists with this name").build();
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
//    @Deprecated
//    @POST
//    @Path("/{path:.+}")
//    @Consumes(MediaTypes.DATA_OBJECT) //here input mime type is application/jwe
//    public Response postToEndeDataObject(
//            @Context HttpHeaders headers,
//            @PathParam("path") String path,
//            byte[] bytes) {
//        File objFile, metadataFile, baseDirectory;
//        //if not a container, but a object
//
//        if (!containerDao.isContainer(path)) {
//            try {
//                DataObject doj = dataObjectDao.findByPath(path);
//                //doj.fromJson(bytes, false);
//                if (doj.getValue() != null) {
//                    DataObject tmp = new DataObject();
//                    tmp.fromJson(bytes, false);
//                    String mime = tmp.getMimetype();
//                    if (mime.equals("text/plain")) {
//                        decryptData(doj, path, true);
//                        return Response.created(URI.create(path)).build();
//                    } else if (mime.equals("application/jose+json")) {
//                        String key = RandomStringUtils.getRandomString(43);
//                        encryptData(doj, path, key);
////                        String plaintext = doj.getValue();    //do encryption with the data object specified by @path
////                        CryptoProvider cp = new CryptoProvider();
////                        //read key id from the params
////                        String key = RandomStringUtils.getRandomString(43);
////                        //cp.setSymkey("{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2D\"}");
////                        cp.setSymkey("{\"kty\":\"oct\",\"k\":\"" + key + "\"}");
////                        cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
////                        cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);
////
////                        String cip = cp.doJWEEncrypt(plaintext);
////                        //we change the content of the data object into an "encrypted" one here.
////                        doj.setMimetype("application/jose+json"); //set the mime type here to encrypted format
////                        //save the key into de metadata of encrypted object
////                        doj.setMetadata("key", key);
////                        doj.setValue(cip);
////                        dataObjectDao.modifyDataObject(path, doj);
////
////                        System.out.println("XXX: " + cip);
////                        String plain = cp.doJWEDecrypt(cip);
////                        System.out.println("XXX: " + plain);
//
//                        return Response.created(URI.create(path)).build();
//                    }
//                }
//               
//                
//           }
//           catch(Exception ex)
//           {
//                LOG.error("Failed to find data object", ex);
//                return Response.status(Response.Status.BAD_REQUEST)
//                        .tag("Object Fetch Error : " + ex.toString()).build();
//           }
//           
//        }  
//        else
//        {
//            System.out.println("This is a container!");
//        }
//        //return null;
//         return Response.status(Response.Status.BAD_REQUEST).build();
//    }

    //update the plain object or encrypted object with my own plain object or encrypted object
    @Deprecated
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
                String mimeRequest = JsonUtils.getValue(bytes, "mimetype", false);
                String value = JsonUtils.getValue(bytes, "value", false);
                //int i = JsonUtils.getEntityNum(bytes);      
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
                        String key = JsonUtils.getValue(bytes, "key", true);
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

    private DataObject decryptData(DataObject doj, String path,String key, Boolean modify) {
        String cipherText = doj.getValue();    //do decryption with the data object specified by @path
        CryptoProvider cp = new CryptoProvider();
        //read key id from the params        
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
