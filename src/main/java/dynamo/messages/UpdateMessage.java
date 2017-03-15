package dynamo.messages;

import java.io.Serializable;

public class UpdateMessage implements Serializable{
    public int key;
    public String value;

    public UpdateMessage(int key, String value){
        this.key = key;
        this.value = value;
    }
}
