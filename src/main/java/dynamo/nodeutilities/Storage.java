package dynamo.nodeutilities;

import dynamo.NodeActor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * This class represents the storage where all Items for which a particular node is responsible are stored
 */
public class Storage {

    private List<Item> db;
    private NodeActor owner; // Do we really need this?
    // TODO: Pass to the constructor the path to be defined in the akka config (e.g. $HOME)
    private String pathname = "/storage/storage.txt";

    public Storage(NodeActor owner) {
        this.owner = owner;
        db = new ArrayList<Item>();
    }

    public Storage(List<Item> db) {
        this.db = db;
    }

    /**
     * updates an NodeUtilities.Item in the NodeUtilities.Storage
     *
     * @param key the key of the NodeUtilities.Item
     * @param value the updated value of the NodeUtilities.Item
     * @param version the version number of the NodeUtilities.Item
     */
    public void update(int key, String value, int version) {
        Item item;

        for(int i = 0; i < db.size(); i++) {
            item = db.get(i);

            if(item.getKey() == key) {
                item.setKey(key);
                item.setValue(value);
                item.setVersion(version);
                save();
                return;
            }
        }
    }

    /**
     * deletes an NodeUtilities.Item from the NodeUtilities.Storage
     *
     * @param key the key of the NodeUtilities.Item
     */
    public void delete(int key) {
        Item item;

        for(int i = 0; i < db.size(); i++) {
            item = db.get(i);

            if(item.getKey() == key) {
                db.remove(item);
                save();
                return;
            }
        }
    }

    /**
     * saves the storage on a local text file
     */
    private void save() {
        try {
            FileWriter out = new FileWriter(pathname);
            out.write(this.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * gets a specific NodeUtilities.Item
     *
     * @param key the key of the NodeUtilities.Item
     * @return the NodeUtilities.Item
     */
    public Item getItem(int key) {
        Item item;

        for(int i = 0; i < db.size(); i++) {
            item = db.get(i);

            if(item.getKey() == key) {
                return item;
            }
        }
        return null;
    }

    /**
     * recovers after a crash
     *
     * to be implemented yet
     */
    public void recover() {

    }

    /**
     *  overrides the java.lang.Object.toString() method, useful to manage the representation of the entire NodeUtilities.Storage
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < db.size(); i++) {
            sb.append(db.get(i) + "\n");
        }

        return sb.toString();
    }
}
