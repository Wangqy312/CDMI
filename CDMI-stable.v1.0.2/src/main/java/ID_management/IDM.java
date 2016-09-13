/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ID_management;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONStyle;
import net.minidev.json.parser.ParseException;
import java.io.*;
/**
 *
 * @author 310241647
 */
public class IDM {
    
     private String filePath;
    
    public IDM()
    {
            
    }
    //in-class test
    public static void main(String[] args) throws IOException{
        IDM idm = new IDM();
        idm.setFilePath("C:/data/idtest.txt");
        
        // create some default users*
        idm.createToken("Kevin", "001");
        idm.createToken("Sam", "002");
        idm.createToken("Lilei", "003");
        idm.createToken("Zhaokang", "004");
        idm.createToken("Guohuan", "005");
        idm.createToken("Zhangsan", "006");
        idm.createToken("Wanger", "007");
        idm.createToken("Hanmeimei", "008");
        
        
      //  System.out.println(idm.getToken("Kevin"));
        
    }
    
    
    //Find Token by username. Return the json string if found, else return "Did not find the user".
    public String getToken(String username) throws IOException{
        this.setFilePath("C:/data/idtest.txt");
        File f = new File(filePath);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(f));
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        while(line != null) {
            Object obj=JSONValue.parse(line);           
            JSONObject jobj = (JSONObject)obj;
            if(jobj.containsKey(username)) {
                System.out.println(jobj.toJSONString());
               JSONObject temp = (JSONObject)(jobj.get(username));
            
                return temp.get("id").toString();
            }
            line = br.readLine();
        }       
        return "Did not find the user.";
    }
    
    //Create Token with username and id, save in C:/data/id.txt
    public boolean createToken(String username, String id) throws IOException{
        if(username == null) {
            System.out.println("Null username!");
            return false;
        }
        //Struct to JsonObject
        ID newID = new ID(username, id);
        JSONObject obj = new JSONObject();
        JSONDomain data = new JSONDomain();   // for convert
        data.setResult(newID);
        obj.put(username, data.getResult());
        System.out.println(obj.toJSONString(JSONStyle.NO_COMPRESS));
        //File io
        try {
            File f = new File(filePath);
            if(!f.exists()) 
                f.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
            bw.append(obj.toJSONString(JSONStyle.NO_COMPRESS) + "\r\n");
            bw.flush();
            bw.close();
            return true;
        } catch (Exception e) {
        }
        return false;
    }
    
    public class JSONDomain {    // for convert struct <==> json
        public Object result = new JSONObject();

        public Object getResult() {
            return result;
        }
        public void setResult(Object result) { 
            this.result = result;
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
}
