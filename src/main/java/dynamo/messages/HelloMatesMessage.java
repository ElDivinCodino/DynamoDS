package dynamo.messages;

import akka.actor.ActorSelection;

import java.io.Serializable;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class HelloMatesMessage implements Serializable {

    private ActorSelection remoteSelection;
    private Integer key;
    private String remotePath;

    public HelloMatesMessage(ActorSelection remoteSelection, Integer key, String remotePath) {
        this.remoteSelection = remoteSelection;
        this.key = key;
        this.remotePath = remotePath;
    }

    public ActorSelection getRemoteSelection() {
        return remoteSelection;
    }

    public Integer getKey() {
        return key;
    }

    public String getRemotePath() {
        return remotePath;
    }

    @Override
    public String toString() {
        return "HelloMatesMessage{" +
                "remoteSelection=" + remoteSelection +
                ", key=" + key +
                ", remotePath='" + remotePath + '\'' +
                '}';
    }
}
