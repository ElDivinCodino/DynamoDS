package dynamo.nodeutilities;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * This class represents the storage where all Items for which a particular node is responsible are stored
 */
public class Storage {

    private ArrayList<Item> db;
    // TODO: Pass to the constructor the path to be defined in the akka config (e.g. $HOME)
    private String pathname = "/storage/storage.txt";

    public Storage(ArrayList<Item> db) {
        this.db = db;
    }

    /**
     * updates an NodeUtilities.Item in the NodeUtilities.Storage, if already present, or adds it to the Storage if not
     *
     * @param key the key of the NodeUtilities.Item
     * @param value the updated value of the NodeUtilities.Item
     * @param version the version number of the NodeUtilities.Item
     */
    public void update(int key, String value, int version) {
        Item item = new Item(key, value, version);

        int i = 0;

        while(item.compareTo(db.get(i)) < 0 && i < db.size()) {
            i++;
        }

        //if already existing, update
        if(item.getKey() == key) {
            item.setKey(key);
            item.setValue(value);
            item.setVersion(version);
            save();
            return;
        } else {
            //else, add, shifting all the others on the right, if any is present after
            db.add(i, item);
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

    /*
     * @param i the threshold: retrieve Items until i
     * @param b if true means that the two nodes are one the tail and one the head of the ring
     * @return the list of the Items that are stored in this node but not in the next one
     */
    public ArrayList<Item> retrieveAll(ArrayList<Item> storage) {
        ArrayList<Item> list = new ArrayList<>();
        list.addAll(db);
        list.removeAll(storage);
        return list;
    }

    /*
     * This method deletes, after a new Node joining the system, the Items the Storage is not anymore responsible for
     * @param list the list of Items the Storage is not anymore responsible for
     */
    public void looseResponsabilityOf(ArrayList<Item> receivedList) {
        db.removeAll(receivedList);
    }

    /*
     * This method deletes, after a new Node joining the system, the Items the Storage is not anymore responsible for
     * @param list the list of Items the Storage is not anymore responsible for
     */
    public void acquireResponsabilityOf(ArrayList<Item> receivedList) {
        db.addAll(receivedList);
    }

    /*
    * @return the ArrayList representing the Storage
    */
    public ArrayList<Item> getStorage() {
        return db;
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
