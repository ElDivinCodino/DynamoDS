package dynamo.nodeutilities;

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
        // TODO: throw an exception in case a key already exists. to do it here or in the main Node class.
        return false;
    }

    public boolean addPeers(TreeMap<Integer, Peer> peers){
        for (Map.Entry<Integer, Peer> entry : peers.entrySet()){
            if (!this.addPeer(entry.getValue()))
                return false;
        }
        return true;
    }

    public boolean removePeer(Peer peer){
        if (peers.containsKey(peer.getKey())) {
            peers.remove(peer.getKey());
            return true;
        }
        return false;
    }

    public boolean removePeer(Integer key){
        if (!peers.containsKey(key)){
            peers.remove(key);
            return true;
        }
        return false;
    }

    public Integer nextEqual(Integer key){
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

    public Integer next(Integer key){
        Integer next = peers.higherKey(key);
        if (next == null){
            next = peers.firstKey();
        }
        return next;
    }

    public Integer previous(Integer key){
        Integer previous = peers.lowerKey(key);
        if (previous == null){
            previous = peers.lastKey();
        }
        return previous;
    }

    public Peer getPeer(Integer key){
        return peers.get(key);
    }

    public Peer getNextPeer(Integer key){
        return this.getPeer(this.next(key));
    }

    public Integer getNumberOfPeers(){
        return this.peers.size();
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
    public ArrayList<Peer> getReplicasFromKey(Integer N, Integer itemKey){
        // The nodes responsible for this item will be the N ones
        // with key >= itemKey
        Integer next = this.nextEqual(itemKey);
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

    /**
     * Checks if this Node is among the next N nodes in clockwise
     * order starting from a certain key value
     * @param startingKey the starting key
     * @param N the number of replicas to check for
     * @param selfKey the key of this node
     * @return true if the node is among the next N clockwise nodes
     * from startingKey, false otherwise
     */
    public boolean selfIsNextNClockwise(Integer startingKey, Integer N, Integer selfKey){
        // TODO: Here there is a problem in case we have smaller number of nodes than N.
        boolean amNext = false;
        Integer replicaCounter = 1;
        Integer currentPeer = this.next(startingKey);
        while (!amNext && replicaCounter < N){
            if (currentPeer.equals(selfKey)){
                amNext = true;
            }
            currentPeer = this.next(currentPeer);
            replicaCounter++;
        }
        return amNext;
    }

    public TreeMap<Integer, Peer> getPeers() {
        return peers;
    }

    public void setPeers(TreeMap<Integer, Peer> peers) {
        this.peers = peers;
    }


    @Override
    public String toString() {
        String output = "Ring: \n";
        for (Map.Entry<Integer, Peer> entry : this.getPeers().entrySet()) {
            output +=  "\t- " + entry.getValue().toString() + "\n";
        }
        return output;
    }
}
