package dynamo.messages;

import dynamo.nodeutilities.Item;

import java.util.ArrayList;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class RequestInitItemsMessage {
    private boolean request = false;
    ArrayList<Item> items = null;
    private Integer senderKey = null;
    private String senderRemotePath = null;

    public RequestInitItemsMessage(boolean request, Integer senderKey, String senderRemotePath) {
        this.request = request;
        this.senderKey = senderKey;
        this.senderRemotePath = senderRemotePath;
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

    public String getSenderRemotePath() {
        return senderRemotePath;
    }
}
