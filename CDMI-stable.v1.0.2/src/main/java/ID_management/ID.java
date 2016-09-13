/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ID_management;

/**
 *
 * @author 310241647
 */
public class ID {
    
    private String id;
    private String name;
    
     public ID() {
        
    }
    
    public ID(String name, String id) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ID{" + "id=" + id + ", name=" + name + '}';
    }
    
}
