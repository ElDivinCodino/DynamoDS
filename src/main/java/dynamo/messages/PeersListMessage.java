package dynamo.messages;

import dynamo.nodeutilities.Peer;

import java.io.Serializable;
import java.util.TreeMap;

public class PeersListMessage implements Serializable {

    private boolean request = false;
    TreeMap<Integer, Peer> peers = null;

    public PeersListMessage(boolean request, TreeMap<Integer, Peer> peers) {
        this.request = request;
        this.peers = peers;
    }

    public boolean isRequest() {
        return request;
    }

    public TreeMap<Integer, Peer> getPeers() {
        return peers;
    }

    @Override
    public String toString() {
        return "PeersListMessage{" +
                "request=" + request +
                ", peers=" + peers +
                '}';
    }
}
