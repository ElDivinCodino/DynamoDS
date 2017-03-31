package dynamo.nodeutilities;

import java.util.*;

/**
 * Created by StefanoFiora on 14/03/2017.
 */
public class Ring {

    //    List<NodeUtilities.Peer> peers = new ArrayList<>();
    TreeMap<Integer, Peer> peers = new TreeMap<Integer, Peer>();

    public Ring(){ }

    //TODO put the javadoc
    public boolean addPeer(Peer peer){
        if (!peers.containsKey(peer.getKey())) {
            peers.put(peer.getKey(), peer);
            return true;
        }
        return false;
    }

    //TODO put the javadoc
    // (FRA) ha senso che restituisca un booleano?
    public boolean addPeers(TreeMap<Integer, Peer> peers){
        for (Map.Entry<Integer, Peer> entry : peers.entrySet()){
            if (!this.addPeer(entry.getValue()))
                return false;
        }
        return true;
    }

    public boolean removePeer(Peer peer){
        return this.removePeer(peer.getKey());
    }

    //TODO put the javadoc
    public boolean removePeer(Integer key){
        if (peers.containsKey(key)){
            peers.remove(key);
            return true;
        }
        return false;
    }

    //TODO put the javadoc
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

    //TODO put the javadoc
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

    //TODO put the javadoc
    public Peer getPeer(Integer key){
        return peers.get(key);
    }

    //TODO put the javadoc
    public Peer getNextPeer(Integer key){
        return this.getPeer(this.next(key));
    }

    //TODO put the javadoc
    public Integer getNumberOfPeers(){
        return this.peers.size();
    }

    /**
     * Returns an array of Peers, where the peers are
     * the Nodes responsible for the replica of a specific data item.
     * The method takes the first N peers in the ring with key greater
     * or equal to itemKey. In case the number of peers in the system
     * is less or equal than N, then all peers are returned.
     * @param N The number of replicas responsible for a data item
     * @param itemKey the item's key to retrieve
     * @return an array of Peers
     */
    public ArrayList<Peer> getReplicasFromKey(Integer N, Integer itemKey){
        // The nodes responsible for this item will be the N ones
        // with key >= itemKey
        Integer next = this.nextEqual(itemKey);
        ArrayList<Peer> replicas = new ArrayList<Peer>();

        // In case there are less nodes than N, an item is stored in all
        // peers in the system.
        if (this.getNumberOfPeers() <= N) {
            replicas = (ArrayList<Peer>)this.getPeers().values();
        } else {
            for (int i = 0; i < N; i++) {
                replicas.add(peers.get(next));
                next = this.next(next);
            }
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

    /**
     * Checks if a certain node is withing a certain range from an item in the ring
     * @param itemKey the item to be checked
     * @param nodeKey the node we are testing for
     * @param N the allowed range (number of nodes) from the item to the node (with the node included)
     * @return true if the given node is withing N number of nodes from the item (i.e. it should contain a replica of the given item)
     */
    public boolean isNodeWithinRangeFromItem(Integer itemKey, Integer nodeKey, Integer N) {
        Integer counter = 1;  // take into account the first one
        Integer currentPeer = this.nextEqual(itemKey);
        boolean responsible = false;

        /*
        starting form the itemKey, iterate over the ring until
        we find the node OR we pass more than N nodes. In the latter case
        it meas that the node we are search for is places farther tha N nodes
        from the item, so it is not responsible for it.
         */
        while (counter <= N && !responsible){
            if (currentPeer.equals(nodeKey)){
                responsible = true;
            }
            currentPeer = this.next(currentPeer);
            counter++;
        }
        return responsible;

//        for(int i = 0; i < N; i++) {
//            if(/*currentPeer contains item*/) {
//                counter++;
//            } else {
//                break; // performance reasons
//            }
//            if(/*i am within the N-counter nodes*/) {
//                return true;
//            } else {
//                return false;
//            }
//        }
    }

    //TODO put the javadoc
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
