package dynamo.messages;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class OperationMessage {

    private boolean client;
    private boolean request;
    // true for a read, false for a write
    private boolean read;

    private Integer key;
    private String value;
    private Integer version;

    public OperationMessage(boolean client, boolean request, boolean read, Integer key, String value) {
        this.client = client;
        this.request = request;
        this.read = read;
        this.key = key;
        this.value = value;
    }

    public OperationMessage(boolean client, boolean request, boolean read, Integer key, String value, Integer version) {
        this.client = client;
        this.request = request;
        this.read = read;
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

    public boolean isRead() {
        return read;
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
