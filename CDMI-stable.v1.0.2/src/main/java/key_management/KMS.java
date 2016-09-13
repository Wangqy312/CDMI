//KMS
package key_management;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.JSONArray;
import java.io.*;
import java.util.UUID;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.snia.cdmiserver.util.RandomStringUtils;
import org.snia.cdmiserver.model.DataObject;

/**
 *
 * @author 310241647
 */
public class KMS {

    private String filePath = "C:/data/key.txt";

    private Authorization auth;

    public Authorization getAuth() {
        return auth;
    }

    public void setAuth(Authorization auth) {
        this.auth = auth;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
//    public static void main(String[] args) throws Exception {
//        KMS kms = new KMS();
//        Authorization auth = new Authorization();
//        // Create some default usernames and keys 
//        DataObject dObj = new DataObject();
//        dObj.setObjectID("stupid");
//        //kms.createKey(auth, dObj, "flt");
//        System.out.println("the keyValue = " + kms.removeKey(dObj, "wqy"));
//    }

    public String getKey(DataObject dObj, String authName) throws Exception {
        try {
            File f = new File(filePath);
            InputStreamReader isr = new InputStreamReader(new FileInputStream(f));
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();

            while (line != null) {
                Object obj = JSONValue.parse(line);
                JSONObject jobj = (JSONObject) obj;
                if (jobj.containsKey(authName)) {
                    JSONArray jarray = (JSONArray) jobj.get(authName);
                    for (int i = 0; i < jarray.size(); i++) {
                        JSONParser jp = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
                        Authorization tmp = jp.parse(jarray.get(i).toString(), Authorization.class);
                        if (tmp.getObjId().equals(dObj.getObjectID())) {
                            return tmp.getKeyValue();
                        }
                    }
                    return null;
                }
                line = br.readLine();
            }
        } catch (IOException | ParseException e) {
            System.out.println(e);
        }

        return null;
    }

    public boolean removeKey(DataObject dObj, String authName) throws Exception {
        JSONObject jcontent = (JSONObject) JSONValue.parse(this.getContent().toString());
        if (jcontent.containsKey(authName)) {
            JSONArray jarray = (JSONArray) jcontent.get(authName);
            for (int i = 0; i < jarray.size(); i++) {
                JSONParser jp = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
                Authorization tmp = jp.parse(jarray.get(i).toString(), Authorization.class);
                if (tmp.getObjId().equals(dObj.getObjectID())) {
                    jcontent.remove(authName);
                    jarray.remove(i);
                    jcontent.put(authName, jarray);
                    //File io
                    try {
                        File f = new File(filePath);
                        if (!f.exists()) {
                            f.createNewFile();
                        }
                        BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));

                        bw.write(jcontent.toJSONString());
                        bw.flush();
                        bw.close();
                        return true;
                    } catch (Exception e) {
                    }
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean createKey(DataObject dObj, String authName) throws Exception {
        if (auth == null) {
            System.out.println("Null auth!");
            return false;
        }
        //JSONObject tmp = new JSONObject();
        StringBuilder sb = this.getContent();
        Object obj = JSONValue.parse(sb.toString());

        JSONObject jcontent = (JSONObject) obj;
        if (jcontent.get(authName) != null) {
            if (this.getKey(dObj, authName) != null) {
                return false;
            }
            //update the object of this auth
            String keyId = UUID.randomUUID().toString();
            String keyValue = RandomStringUtils.getRandomString(43);
            auth.setKeyId(keyId);
            auth.setKeyValue(keyValue);
            auth.setObjId(dObj.getObjectID());
            JSONArray objItem = (JSONArray) jcontent.get(authName);
            objItem.add(auth);
            jcontent.remove(authName);
            jcontent.put(authName, objItem);
        } else {
            //create a new auth
            String keyId = UUID.randomUUID().toString();
            String keyValue = RandomStringUtils.getRandomString(43);
            auth.setKeyId(keyId);
            auth.setKeyValue(keyValue);
            auth.setObjId(dObj.getObjectID());
            JSONArray objItem = new JSONArray();
            objItem.add(auth);
            jcontent.put(authName, objItem);
        }
        //File io
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                f.createNewFile();
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter(f, false));

            bw.write(jcontent.toJSONString());
            bw.flush();
            bw.close();
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    private StringBuilder getContent() throws Exception {
        File f = new File(filePath);
        InputStreamReader isr = new InputStreamReader(new FileInputStream(f));
        BufferedReader br = new BufferedReader(isr);
        String line = br.readLine();
        StringBuilder sb = new StringBuilder();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }
        return sb;
    }
}
