/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.snia.cdmiserver.util;

import java.util.Random;

/**
 *
 * @author 310253694
 */
public class RandomStringUtils {

    static public String getRandomString(int length) {
        String model = new String("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        StringBuilder result = new StringBuilder();
        Random ran = new Random();
        int len = model.length();
        for (int i = 0; i < length; i++) {
            result.append(model.charAt(ran.nextInt(len)));
        }
        return result.toString();
    }
}
