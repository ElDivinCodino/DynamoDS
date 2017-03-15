package dynamo.messages;

import java.io.Serializable;

public class Get implements Serializable {
    public int key;

    public Get(int key) {
        this.key = key;
    }
}
