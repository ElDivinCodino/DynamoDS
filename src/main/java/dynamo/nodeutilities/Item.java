package dynamo.nodeutilities;

/**
 * Class which represents  basic item we want to store and manage
 */
public class Item {

    private int key;
    private String value;
    private int version;

    public Item(int key, String value, int version) {
        this.key = key;
        this.value = value;
        this.version = version;
    }

    /**
     * @return the key of the item
     */
    public int getKey() {
        return key;
    }

    /**
     * @return the value of the item
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the version of the item
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param key must be an integer
     */
    public void setKey(int key) {
        this.key = key;
    }

    /**
     * @param value must be a String
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @param version must be an integer
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     *  overrides the java.lang.Object.toString() method, useful to manage the representation of the NodeUtilities.Item
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(key + "\t\t");
        sb.append(value + "\t\t");
        sb.append(version);

        return sb.toString();
    }
}
