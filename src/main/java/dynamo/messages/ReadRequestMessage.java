package dynamo.messages;

import java.io.Serializable;

/**
 * Created by StefanoFiora on 14/03/2017.
 */
public class ReadRequestMessage implements Serializable {

    private Integer key;

    public ReadRequestMessage(Integer key){
        this.key = key;
    }

    public Integer getKey() {
        return key;
    }
}
