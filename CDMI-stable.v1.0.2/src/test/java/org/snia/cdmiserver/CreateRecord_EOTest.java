/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.snia.cdmiserver;


import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snia.cdmiclient.CDMIClient;

import static org.snia.cdmiclient.Request.Method.*;
import static org.snia.cdmiserver.Matchers.*;

import java.net.URISyntaxException;

import static org.junit.Assert.assertThat;
import static org.snia.cdmiserver.ServerContext.given;
import ID_management.*;

import java.io.*;
/**
 *
 * @author 310219516
 */
public class CreateRecord_EOTest {
    
    private CDMIClient client;
    private ServerContext server;
 
    @Before
    public void setup() throws URISyntaxException
    {
        client = new CDMIClient("http://localhost:8080/CDMI-Feb24");
        client.setRequestVersion("1.0.2");
        client.addRequestObserver(new Eraser());
        server = new ServerContext(client);
    }

    @After
    public void tearDown()
    {
        client.close();
    }

    //≤‚ ‘
    //@Test
    //jump to postToEndeDataObject()
    public void testEncryption() throws Exception {
        //step 4 instructs the server do in-place encryption
        HttpResponse response = client.request(POST, "/TestContainer/TestObject.txt")
                .withContentType("application/cdmi-object")
                .withEntity("{\n"
                        + "\"mimetype\": \"application/jose+json\"\n"
                        + "}\n")
                .send();
        assertThat(response.getStatusLine(), hasStatusCode(201));
        
    }
    
    //@Test
    //jump to postToEndeDataObject()
    public void testDecryption() throws Exception
    {
        //step 4 instructs the server do in-place encryption
         HttpResponse response = client.request(POST, "/TestContainer/TestObject.txt")
                .withContentType("application/cdmi-object")
                .withEntity("{\n"
                        + "\"mimetype\": \"text/plain\"\n"
                        + "}\n")
                .send();
         assertThat(response.getStatusLine(), hasStatusCode(201));
        
    }
    
    //@Test
    //jump to postMyDataObject()
    public void testUpdateDataObjectWithPlainObject() throws Exception {
        HttpResponse response = client.request(POST, "/TestContainer/TestObject.txt")
                .withContentType("application/jose+json")
                .withEntity("{\n"
                        + "\"mimetype\": \"text/plain\",\n"
                        + "\"value\": \"This is the most ridiculous things I have ever heard.\"\n"
                        + "}\n")
                .send();
    }
    
    //@Test
    //jump to postMyDataObject()
    public void testUpdateDataObjectWithEncObject() throws Exception {
        HttpResponse response = client.request(POST, "/TestContainer/TestObject.txt")
                .withContentType("application/jose+json")
                .withEntity("{\n"
                        + "\"mimetype\": \"application/jose+json\",\n"
                        + "\"value\": \"eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0..TDpOLgTBXvapPNs8-yAUwQ.mVPC7z3G1ULqxpsvZ8YDYg.YzqpRpE2I06rGUO1NMtv4A\",\n"
                        + "\"metadata\": {\n"
                        +               "\"key\": \"TrlkAwiRGadZeInmNs6hFme4tjxd9HSmY2Nx5WsRumm\"\n"   //the plaintext of it is "This is a test"
                        +               "}\n"
                        + "}\n")
                .send();
    }


    
      //≤‚ ‘
    //@Test
    
    public void createRecord() throws Exception
    {
        //step 1: obtain a IHE-IUA token from IDM
        String username = "Kevin";
         String token;
      
        //faked to get a token from the IDM
        //IDM idm_instance = new IDM();
        //token = idm_instance.getToken(username); 
       
       
        
        //step 2  pass the token to the hospital controller
        /*
        given(server.hasContainer("/temp/"));
        
        HttpResponse response1 = client.request(PUT, "/temp/token.txt")
                .withContentType("application/cdmi-object")
                .withEntity("{\n" + 
                                "\"mimetype\": \"text/plain\",\n" +
                                "\"value\": \""+token+"\"\n" +  //input the client token value here
                            "}\n")    
                .send();
         assertThat(response1.getStatusLine(), hasStatusCode(201));
         */
         //step 3 send the patient record to the hospital controller 
         
        //µ˜ ‘ 
        //given(server.hasContainer("/temp/"));
        
        HttpResponse response2 = client.request(PUT, "/temp/record1.txt")
                .withContentType("application/cdmi-object")
                .withEntity("{\n" + 
                                "\"mimetype\": \"text/plain\",\n" +
                                "\"value\": \""+" :"+username+"\"\n" +  //input the client token value here
                            "}\n")    
                .send();
         assertThat(response2.getStatusLine(), hasStatusCode(201));
         
       //step 4 instructs the server do in-place encryption
         HttpResponse response3 = client.request(PUT, "/temp/record1.txt")
                .withContentType("application/jose+json")
                .send();
         assertThat(response3.getStatusLine(), hasStatusCode(201));
    }
    //POC
    /*
 @Test   
    public void getDecryptedRecord() throws Exception
    {
          HttpResponse response3 = client.request(GET, "/temp/record1.txt")
                .send();
            assertThat(response3.getStatusLine(), hasStatusCode(200));
    }*/
}
