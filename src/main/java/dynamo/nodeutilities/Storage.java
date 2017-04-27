package dynamo.nodeutilities;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class represents the storage where all Items for which a particular node is responsible are stored
 */
public class Storage {

    private ArrayList<Item> db;

    private String pathname;

    public Storage(String pathname) {
        this.db = new ArrayList<>();
        this.pathname = pathname;
    }

    public void initializeStorage(ArrayList<Item> initItems){
        for (Item item : initItems){
            this.update(item.getKey(), item.getValue(), item.getVersion());
        }
    }

    /**
     * updates an NodeUtilities.Item in the NodeUtilities.Storage, if already present,
     * or adds it to the Storage if not. Items are stored in crescent order
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
        if(db.size() > i && db.get(i).getKey() == key) {
            db.get(i).setValue(value);
            db.get(i).setVersion(version);
            save();
            return;
        } else {
            //else, add, shifting possibly all the others on the right, if any is present after
            db.add(i, item);
        }
        save();
    }

    /**
     * This method iterates over every item in the storage and
     * checks if the local node is not among the N replicas of
     * a given item anymore
     *
     * @param localNodeKey the key of the current node
     * @param localNodeRing the Ring of the current node
     * @param N the number of Peers that must have a copy of an Item
     *
     */
    public void removeItemsOutOfResponsibility(Integer localNodeKey, Ring localNodeRing, Integer N){
        ArrayList<Item> valuesToBeRemoved = new ArrayList<>(); // to avoid java.util.ConcurrentModificationException

        for (Item item : this.db){
            // if the node has not responsibility of this item
            if (!localNodeRing.isNodeWithinRangeFromItem(item.getKey(), localNodeKey, N)){
                valuesToBeRemoved.add(item);
            }
        }
        db.removeAll(valuesToBeRemoved);
        save();
    }

    /**
     * Gets a specific Item
     *
     * @param key the key of the NodeUtilities.Item
     * @return the Item
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
     * Gets all the items excepts the ones with key that is inside
     * the given range, with 'to' excluded.
     * @param from
     * @param to
     * @return the list of the Items the requesting node is responsible for
     */
    public ArrayList<Item> getItemsForNewNode(Integer from, Integer to){
        ArrayList<Item> list = new ArrayList<>();
        for(int i = 0; i < db.size(); i++) {
            Item item = db.get(i);

            if(item.getKey() <= from || item.getKey() > to) {
                list.add(item);
            }
        }
        return list;
    }

    /**
    * @return the ArrayList representing the Storage
    */
    public ArrayList<Item> getStorage() {
        return db;
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
     * load Items after a crash, when recovery is requested
     * @return true if the operation has been correctly executed, false otherwise
     */
    public boolean loadItems() throws Exception {

        int key, version;
        String value;

        try {
            FileReader in = new FileReader(pathname);
            BufferedReader b = new BufferedReader(in);

            // this is the "Storage: \n" line, so must be discarded
            b.readLine();
            String current;

            Pattern pattern = Pattern.compile("\t- Item\\{key=(\\d+), value='(\\w+)', version=(\\d+)\\}");
            Matcher matcher;

            while((current = b.readLine()) != null) {

                matcher = pattern.matcher(current);

                if(matcher.matches()) {
                    key = Integer.parseInt(matcher.group(1));
                    value = matcher.group(2);
                    version = Integer.parseInt(matcher.group(3));

                    update(key, value, version);
                } else {
                    throw new Exception("Corrupted data. Failed to load local Storage.");
                }
            }
            b.close();
            in.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     *  overrides the java.lang.Object.toString() method, useful to manage the representation of the entire NodeUtilities.Storage
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Storage: \n");

        if (this.db.size() == 0) {
            sb.append("\tEmpty.");
        }else {
            for (Item aDb : db) {
                sb.append("\t- ").append(aDb).append("\n");
            }
        }
        return sb.toString();
    }

}
