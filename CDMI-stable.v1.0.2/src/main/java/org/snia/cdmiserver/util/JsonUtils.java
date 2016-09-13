/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.snia.cdmiserver.util;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.snia.cdmiserver.exception.BadRequestException;

/**
 *
 * @author 310253694
 */
public class JsonUtils {

    public static String getValue(byte[] jsonBytes, String valueName, Boolean isMetadata) throws Exception{
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(jsonBytes);
        JsonToken tolkein;
        tolkein = jp.nextToken();// START_OBJECT
        String value = new String();
        while ((tolkein = jp.nextToken()) != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            if (valueName.equals(key)) { // process mimetype
                if ("metadata".equals(key) && isMetadata) {// process metadata
                    tolkein = jp.nextToken();
                    while ((tolkein = jp.nextToken()) != JsonToken.END_OBJECT) {
                        key = jp.getCurrentName();
                        tolkein = jp.nextToken();
                        if(key.equals(valueName)){
                            value = jp.getText();
                        }
                    }// while
                }
                jp.nextToken();
                value = jp.getText();
                break;
            }
        }
        return value;
    }
    
    public static int getEntityNum(byte[] jsonBytes) throws Exception{
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(jsonBytes);
        JsonToken tolkein;
        tolkein = jp.nextToken();// START_OBJECT
        int num = 0;
        while ((tolkein = jp.nextToken()) != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            num++;
        }
        return num/2;
    }
}
