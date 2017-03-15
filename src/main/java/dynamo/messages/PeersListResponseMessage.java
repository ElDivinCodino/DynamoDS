package dynamo.messages;

import dynamo.nodeutilities.Peer;

import java.util.TreeMap;

/**
 * Created by StefanoFiora on 15/03/2017.
 */
public class PeersListResponseMessage {

    TreeMap<Integer, Peer> peers;

    public PeersListResponseMessage(TreeMap<Integer, Peer> peers) {
        this.peers = peers;
    }

    public TreeMap<Integer, Peer> getPeers() {
        return peers;
    }

}
