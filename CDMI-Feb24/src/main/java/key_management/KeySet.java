
package key_management;

/**
 *
 * @author 310241647
 */
public class KeySet {
    
    private String username;
    private String key;
    
    public KeySet() {}
    
    public KeySet(String username, String key) {
        this.username = username;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "KeySet{" + "username=" + username + ", key=" + key + '}';
    }
    
}
