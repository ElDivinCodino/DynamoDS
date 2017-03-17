package dynamo.messages;

import java.io.Serializable;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class HelloMatesMessage implements Serializable {

    private Integer key;
    private String remotePath;

    public HelloMatesMessage(Integer key, String remotePath) {
        this.key = key;
        this.remotePath = remotePath;
    }

    public Integer getKey() {
        return key;
    }

    public String getRemotePath() {
        return remotePath;
    }

    @Override
    public String toString() {
        return "HelloMatesMessage{" +
                "key=" + key +
                ", remotePath='" + remotePath + '\'' +
                '}';
    }
}
