package dynamo.nodeutilities;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * This class represents the storage where all Items for which a particular node is responsible are stored
 */
public class Storage {

    private ArrayList<Item> db;
    // TODO: Pass to the constructor the path to be defined in the akka config (e.g. $HOME)
    private String pathname = "./storage/storage.txt";

    public Storage() {
        this.db = new ArrayList<>();
    }

    public Storage(ArrayList<Item> db) {
        this.db = db;
    }

    public void initializeStorage(ArrayList<Item> initItems){
        for (Item item : initItems){
            this.update(item.getKey(), item.getValue(), item.getVersion());
        }
    }

    /**
     * updates an NodeUtilities.Item in the NodeUtilities.Storage, if already present, or adds it to the Storage if not. Items are stored in crescent order
     *
     * @param key the key of the NodeUtilities.Item
     * @param value the updated value of the NodeUtilities.Item
     * @param version the version number of the NodeUtilities.Item
     */
    public void update(int key, String value, int version) {
        Item item = new Item(key, value, version);

        int i = 0;

        while(i < db.size() && item.compareTo(db.get(i)) < 0) {
            i++;
        }

        //if already existing, update
        if(db.size() > 0 && db.get(i).getKey() == key) {
            db.get(i).setValue(value);
            db.get(i).setVersion(version);
            save();
            return;
        } else {
            //else, add, shifting all the others on the right, if any is present after
            db.add(i, item);
        }
    }

    /**
     * deletes a NodeUtilities.Item from the NodeUtilities.Storage
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
     * deletes NodeUtilities.Item up to a certain value from the NodeUtilities.Storage
     *
     * @param key the key of the NodeUtilities.Item
     */
    public void deleteUpTo(int key) {
        Item item;

        while(db.size() > 0) {
            item = db.get(0); // we exploit the fact that elements are sorted in crescent order to improve performance

            if(item.getKey() < key) { //TODO choose if less / less or equal
                db.remove(item);
            } else {
                return;
            }
        }
        return;
    }

    /**
     * This method iterates over every item in the storage and
     * checks if the local node is not among the N replicas of
     * a given item anymore
     */
    public void removeItemsOutOfResponsibility(Integer localNodeKey, Ring localNodeRing, Integer N){
        for (Item item : this.db){
            // if the node has not responsibility of this item
            if (!localNodeRing.isNodeWithinRangeFromItem(item.getKey(), localNodeKey, N)){
                this.delete(item.getKey());
            }
        }
    }

    /**
     * saves the storage on a local text file
     */
    private void save() {
        //TODO
        /*try {
            FileWriter out = new FileWriter(pathname);
            out.write(this.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
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
     * @param storage the node next to the current
     * @return the list of the Items that are stored in this node but not in the next one
     */
    public ArrayList<Item> retrieveAll(ArrayList<Item> storage) {
        ArrayList<Item> list = new ArrayList<>();
        list.addAll(db);
        list.removeAll(storage);
        return list;
    }

    /**
     * Gets all the items excepts the ones with key that is inside
     * the given range, with 'to' excluded.
     * @param from
     * @param to
     * @return
     */
    public ArrayList<Item> getItemsForNewNode(Integer from, Integer to){
        ArrayList<Item> list = new ArrayList<>();
        for(int i = 0; i < db.size(); i++) {
            Item item = db.get(i);

            if(item.getKey() <= from && item.getKey() > to) {
                list.add(item);
            }
        }
        return list;
    }

    /**
     * This method deletes, after a Node joining or leaving the system, the Items the Storage is not anymore responsible for
     * @param receivedList the list of Items the Storage is not anymore responsible for
     */
    public void looseResponsibilityOf(ArrayList<Item> receivedList) {
        db.removeAll(receivedList);
        save();
    }

    /**
     * This method adds, after a Node joining or leaving the system, the Items the Storage is now responsible for
     * @param receivedList the list of Items the Storage is not anymore responsible for
     */
    public void acquireResponsibilityOf(ArrayList<Item> receivedList) {
        db.addAll(receivedList);
        save();
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

        sb.append("Storage: \n");
        for (Item aDb : db) {
            sb.append("\t- ").append(aDb).append("\n");
        }
        return sb.toString();
    }

}
