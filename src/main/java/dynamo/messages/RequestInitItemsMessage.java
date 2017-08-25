package dynamo.messages;

import dynamo.nodeutilities.Item;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Message responsible to ask for and share all the already existing Items that the sender will be responsible for
 */
public class RequestInitItemsMessage implements Serializable {
    private boolean request = false;
    ArrayList<Item> items = null;
    private Integer senderKey = null;

    public RequestInitItemsMessage(boolean request, Integer senderKey) {
        this.request = request;
        this.senderKey = senderKey;
    }

    public RequestInitItemsMessage(boolean request, ArrayList<Item> items) {
        this.request = request;
        this.items = items;
    }

    public boolean isRequest() {
        return request;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    public Integer getSenderKey() {
        return senderKey;
    }

    @Override
    public String toString() {
        return "RequestInitItemsMessage{" +
                "request=" + request +
                ", items=" + items +
                ", senderKey=" + senderKey +
                '}';
    }
}
