package key_management;

/**
 *
 * @author 310241647
 */
public class Authorization {

    private String objId;
    private String keyId;
    private String keyValue;

    public Authorization() {
    }

    public Authorization(String objId, String keyId, String keyValue) {
        System.out.println("KeySet constructor.");
        this.objId = objId;
        this.keyId = keyId;
        this.keyValue = keyValue;
    }

    public String getObjId() {
        return objId;
    }

    public void setObjId(String objId) {
        this.objId = objId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getKeyValue() {
        return keyValue;
    }

    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    @Override
    public String toString() {
        return "Authorization{" + "objId" + objId + ", keyId=" + keyId + ", keyValue=" + keyValue + "}";
    }

}

