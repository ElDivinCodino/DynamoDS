package dynamo.nodeutilities;

import akka.actor.ActorSelection;

import java.io.Serializable;


/**
 * Created by StefanoFioravanzo on 14/03/2017.
 */
public class Peer implements Serializable {

    // the akka remote path that identifies an actor.
    private String remotePath = null;
    // actor reference
    private ActorSelection remoteSelection = null;
    // unique (global) actor identifier
    private Integer key = null;

    public Peer(String remotePath, Integer key){
        this.remotePath = remotePath;
        this.key = key;
    }

    public Peer(String remotePath, ActorSelection remoteSelection, Integer key){
        this.remotePath = remotePath;
        this.remoteSelection = remoteSelection;
        this.key = key;
    }

    // Getters and setters for private properties of the class

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public ActorSelection getRemoteSelection() {
        return remoteSelection;
    }

    public void setRemoteSelection(ActorSelection remoteSelection) {
        this.remoteSelection = remoteSelection;
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
                ", remoteSelection=" + remoteSelection +
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
