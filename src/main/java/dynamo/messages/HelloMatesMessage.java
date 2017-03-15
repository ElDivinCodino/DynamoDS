package dynamo.messages;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class HelloMatesMessage {

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
}
