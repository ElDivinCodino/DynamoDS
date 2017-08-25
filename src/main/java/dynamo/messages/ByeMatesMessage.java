package dynamo.messages;

import dynamo.nodeutilities.Item;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Message to announce everyone that a leaving operation is started, and to share own Storage
 */
public class ByeMatesMessage implements Serializable {

    private Integer key;
    private ArrayList<Item> items;

    public ByeMatesMessage(Integer key, ArrayList<Item> items) {
        this.key = key;
        this.items = items;
    }

    public Integer getKey() {
        return key;
    }

    public ArrayList<Item> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "ByeMatesMessage{" +
                "key=" + key +
                ", items=" + items +
                '}';
    }
}
