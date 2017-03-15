package dynamo.messages;

import dynamo.nodeutilities.Peer;

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class PeersListMessage implements Serializable {

    private boolean request = false;
    TreeMap<Integer, Peer> peers = null;

    public PeersListMessage(boolean request) {
        this.request = request;
    }

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
}
