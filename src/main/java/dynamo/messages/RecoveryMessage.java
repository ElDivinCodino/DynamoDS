package dynamo.messages;

import akka.actor.ActorSelection;

import java.io.Serializable;

/**
 * Message responsible manage the recovery of a crashed Node
 */
public class RecoveryMessage implements Serializable{

    private String remoteIp, remotePort, remotePath;
    private int requesterId;
    private ActorSelection remoteSelection;

    public RecoveryMessage(String remoteIp, String remotePort, int requesterId) {
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.requesterId = requesterId;
    }

    public RecoveryMessage(String remotePath, ActorSelection remoteSelection, int requesterId) {
        this.remotePath = remotePath;
        this.remoteSelection = remoteSelection;
        this.requesterId = requesterId;
    }

    public String getRemotePath(){
        return remotePath;
    }

    public ActorSelection getActorSelection() {
        return remoteSelection;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public String getRemotePort() {
        return remotePort;
    }

    @Override
    public String toString() {
        return "RecoveryMessage{" +
                "remoteIp='" + remoteIp + '\'' +
                ", remotePort='" + remotePort + '\'' +
                ", remotePath='" + remotePath + '\'' +
                ", requesterId=" + requesterId +
                ", remoteSelection=" + remoteSelection +
                '}';
    }
}
