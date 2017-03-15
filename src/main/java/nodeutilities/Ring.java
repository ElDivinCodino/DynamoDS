package nodeutilities;

import java.util.*;

/**
 * Created by StefanoFiora on 14/03/2017.
 */
public class Ring {

    //    List<NodeUtilities.Peer> peers = new ArrayList<>();
    TreeMap<Integer, Peer> peers = new TreeMap<Integer, Peer>();

    public Ring(){ }

    public boolean addPeer(Peer peer){
        if (!peers.containsKey(peer.getKey())) {
            peers.put(peer.getKey(), peer);
            return true;
        }
        return false;
    }

    public boolean removePeer(Peer peer){
        if (!peers.containsKey(peer.getKey())) {
            peers.remove(peer.getKey());
            return true;
        }
        return false;
    }

    private Integer getNextEqualKey(Integer key){
        // gets the least equal or greater key
        Integer lowest = peers.ceilingKey(key);
        // in case there is not a key, returns null
        // this means we are at the 'end' of the ring,
        // have to start from the beginning
        if (lowest == null) {
            lowest = peers.firstKey();
        }
        return lowest;
    }

    private Integer next(Integer key){
        Integer next = peers.higherKey(key);
        if (next == null){
            next = peers.firstKey();
        }
        return next;
    }

    private Integer previous(Integer key){
        Integer previous = peers.lowerKey(key)
        if (previous == null){
            previous = peers.lastKey();
        }
        return previous;
    }

    /**
     * Returns an array of Peers, where the peers are
     * the Nodes responsible for the replica of a specific data item.
     * The method takes the first N peers in the ring with key greater
     * or equal to itemKey.
     * @param N The number of replicas responsible for a data item
     * @param itemKey the item's key to retrieve
     * @return an array of Peers
     */
    public ArrayList<Peer> getNextNodesFromKey(Integer N, Integer itemKey){
        // The nodes responsible for this item will be the N ones
        // with key >= itemKey
        Integer next = this.getNextEqualKey(itemKey);
        ArrayList<Peer> replicas = new ArrayList<Peer>();
        // TODO: What if we have LESS nodes in the ring than N??
        // let's assume that in the ring there are ALL the Nodes,
        // so there is also Self.
        for (int i = 0; i < N; i++) {
            replicas.add(peers.get(next));
            next = this.next(next);
        }
        return replicas;
    }

    public TreeMap<Integer, Peer> getPeers() {
        return peers;
    }

    public void setPeers(TreeMap<Integer, Peer> peers) {
        this.peers = peers;
    }

//    /**
//     * Comparator used to keep a collections of Peers sorted
//     * by key.
//     */
//    public class NameComparator implements Comparator<NodeUtilities.Peer>
//    {
//        public int compare(NodeUtilities.Peer o1, NodeUtilities.Peer o2)
//        {
//            return o1.getKey().compareTo(o2.getKey());
//        }
//    }
}
