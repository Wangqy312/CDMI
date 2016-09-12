
package key_management;

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
public class KMS {
    
    private String filePath = "C:/data/key.txt";
    
    public static void main(String[] args) throws Exception{
        KMS kms = new KMS();
        
       // Create some default usernames and keys 
        kms.createKey("Kevin", "asdfghjk");
        kms.createKey("Sam", "aesfghjk");
        kms.createKey("Lilei", "qwefghjk");
        kms.createKey("Zhaokang", "xcvfghjk");
        kms.createKey("Guohuan", "atyuiojk");
        kms.createKey("Zhangsan", "aghjkhjk");
        kms.createKey("Wanger", "mnbfghjk");
        kms.createKey("Hanmeimei", "oiufghjk");
        
        kms.createKey("", "");
        kms.getKey("Kevin");
        kms.getKey("Guohuan");
        
        System.out.println(kms.getKey("Guohuan"));
    }
    
    public String getKey(String username) throws Exception{
        try {
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
            
                return temp.get("key").toString();
            }
            line = br.readLine();
        }       
        } catch (Exception e) {
        }
        
        return "Did not find the user.";
    }
    
    public boolean createKey(String username, String key) {
        if(username == null) {
            System.out.println("Null username!");
            return false;
        }
        if(key.getBytes().length != 8) {
            System.out.println("Key length wrong! 32 bytes string required!");
            return false;
        }
        //Struct to JsonObject
        KeySet keySet = new KeySet(username, key);
        JSONObject obj = new JSONObject();
        JSONDomain data = new JSONDomain();   // for convert
        data.setResult(keySet);
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
}
