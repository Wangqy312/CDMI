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

    public static String getValue(byte[] jsonBytes, String valueName) throws Exception{
        JsonFactory f = new JsonFactory();
        JsonParser jp = f.createJsonParser(jsonBytes);

        JsonToken tolkein;
        tolkein = jp.nextToken();// START_OBJECT
        String value = new String();
        while ((tolkein = jp.nextToken()) != JsonToken.END_OBJECT) {
            String key = jp.getCurrentName();
            if (valueName.equals(key)) { // process mimetype
                jp.nextToken();
                value = jp.getText();
                break;
            }
        }
        return value;
    }
}
