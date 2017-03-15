package dynamo.nodeutilities;

import akka.actor.ActorRef;


/**
 * Created by StefanoFioravanzo on 14/03/2017.
 */
public class Peer {

    // the akka remote path that identifies an actor.
    private String remotePath = null;
    // actor reference
    private ActorRef me = null;
    // unique (global) actor identifier
    private Integer key = null;

    public Peer(String remotePath, Integer key){
        this.remotePath = remotePath;
        this.key = key;
    }

    public Peer(String remotePath, ActorRef me, Integer key){
        this.remotePath = remotePath;
        this.me = me;
        this.key = key;
    }

    // Getters and setters for private properties of the class

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public ActorRef getMe() {
        return me;
    }

    public void setMe(ActorRef me) {
        this.me = me;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return "NodeUtilities.Peer{" +
                "remotePath='" + remotePath + '\'' +
                ", me=" + me +
                ", key=" + key +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Peer peer = (Peer) o;

        return key != null ? key.equals(peer.key) : peer.key == null;
    }

    @Override
    public int hashCode() {
        return key != null ? key.hashCode() : 0;
    }
}
