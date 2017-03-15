package dynamo.messages;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class ReadOperationMessage {

    private boolean client;
    private boolean request;

    private Integer key;
    private String value;
    private Integer version;

    public ReadOperationMessage(boolean client, boolean request, Integer key) {
        this.client = client;
        this.request = request;
        this.key = key;
    }

    public ReadOperationMessage(boolean client, boolean request, Integer key, String value, Integer version) {
        this.client = client;
        this.request = request;
        this.key = key;
        this.value = value;
        this.version = version;
    }

    public boolean isClient() {
        return client;
    }

    public boolean isRequest() {
        return request;
    }

    public Integer getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Integer getVersion() {
        return version;
    }
}
