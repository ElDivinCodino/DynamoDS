package dynamo.messages;

import java.io.Serializable;

public class Update implements Serializable{
    public int key;
    public String value;

    public Update(int key, String value){
        this.key = key;
        this.value = value;
    }
}
