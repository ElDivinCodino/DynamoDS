package dynamo.nodeutilities;

import java.util.*;

/**
 * The representation of the current network, as a ring;
 * it contains the Nodes and the logic operations that can be performed on the ring
 */
public class Ring {

    private TreeMap<Integer, Peer> peers = new TreeMap<Integer, Peer>();

    public Ring(){ }

    /**
     * Adds a Peer to the Collection
     *
     * @param peer the Peer to be added to the Collection
     * @return true if the Peer was not already in the Collection, false otherwise
     */
    public boolean addPeer(Peer peer){
        return this.addPeer(peer, false);
    }

    /**
     * Add a Peer to the Collection
     * @param peer the Peer to be added to the Collection
     * @param force true if we want to replace a possibily already existing Peer
     *              (with the same key) with the new one
     * @return true if the operation went good
     */
    public boolean addPeer(Peer peer, boolean force){
        if (!peers.containsKey(peer.getKey()) || force) {
            peers.put(peer.getKey(), peer);
            return true;
        }
        return false;
    }

    /**
     * Adds a set of Peers to the Collection
     *
     * @param peers the set of Peers to be added to the Collection
     * @return true if the entire set was not already in the Collection, false otherwise
     */
    public boolean addPeers(TreeMap<Integer, Peer> peers){
        for (Map.Entry<Integer, Peer> entry : peers.entrySet()){
            if (!this.addPeer(entry.getValue()))
                return false;
        }
        return true;
    }

    /**
     * Removes a Peer from the Collection
     *
     * @param key the key of the Peer that has to be removed
     * @return true if the Peer is contained in the Collection and correctly removed, false otherwise
     */
    public boolean removePeer(Integer key){
        if (peers.containsKey(key)){
            peers.remove(key);
            return true;
        }
        return false;
    }

    /**
     * Gets the least equal or greater key
     *
     * @param key the predecessor of the desired key
     * @return the least equal or greater key, or null if there is no such key
     */
    private Integer nextEqual(Integer key){

        Integer lowest = peers.ceilingKey(key);
        // in case there is not a key, returns null
        // this means we are at the 'end' of the ring,
        // have to start from the beginning
        if (lowest == null) {
            lowest = peers.firstKey();
        }
        return lowest;
    }

    /**
     * Gets the least greater key
     *
     * @param key the predecessor of the desired key
     * @return the least greater key, or null if there is no such key
     */
    private Integer next(Integer key){
        Integer next = peers.higherKey(key);
        if (next == null){
            next = peers.firstKey();
        }
        return next;
    }

    /**
     * Gets the Peer corresponding to the given key
     *
     * @param key the key of the Peer to be obtained
     * @return the Peer corresponding to the key
     */
    public Peer getPeer(Integer key){
        return peers.get(key);
    }

    /**
     * Gets the Peer next to the given key
     *
     * @param key the key of the predecessor of the desired Peer
     * @return the Peer having the next key in the Ring
     */
    public Peer getNextPeer(Integer key){
        return this.getPeer(this.next(key));
    }

    /**
     * Gets the number of Peers currently joined to the Ring
     *
     * @return the number of Peers currently joined to the Ring
     */
    public Integer getNumberOfPeers(){
        return this.peers.size();
    }

    /**
     * Returns true if a Peer with a certain key belongs to the Ring, false otherwise
     *
     * @param key the key of the Peer that has to be checked
     * @return true if a Peer with a certain key belongs to the Ring, false otherwise
     */
    public boolean keyExists(Integer key) {
        return this.peers.keySet().contains(key);
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
        ArrayList<Peer> replicas = new ArrayList<>();

        // In case there are less nodes than N, an item is stored in all
        // peers in the system.
        if (this.getNumberOfPeers() <= N) {
            replicas = new ArrayList<>(this.getPeers().values());
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
        while (!amNext && replicaCounter <= N){
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
    }

    /**
     * Gets the entire set of Peers currently joined to the Ring
     *
     * @return the entire set of Peers currently joined to the Ring
     */
    public TreeMap<Integer, Peer> getPeers() {
        return peers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(DynamoLogger.ANSI_WHITE + "Ring: \n" + DynamoLogger.ANSI_RESET);
        for (Map.Entry<Integer, Peer> entry : this.getPeers().entrySet()) {
            sb.append("\t- ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
