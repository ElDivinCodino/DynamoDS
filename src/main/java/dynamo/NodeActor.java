package dynamo;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import dynamo.messages.*;
import dynamo.nodeutilities.*;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class NodeActor extends UntypedActor{

    private DynamoLogger nodeActorLogger = new DynamoLogger();

    // For know we hard code these values
    // Think about maybe reading them form the config at
    // actor initialization
    private Integer N = 0;
    private Integer R = 0;
    private Integer W = 0;
    private Integer Q = 0;

    // The identifier of the dynamo.NodeActor.
    private Integer idKey = null;
    private String remotePath = null;

    // Where all the peers are stored.
    private Ring ring = null;

    // Where all the data items are stored.
    private Storage storage = null;
    private String storagePath;
    /*
    Handy variable when we are dealing with a read/write request
    from the client.
     */
    // read or write operation
    private boolean readOperation = false;
    // true if we are waiting for some nodes to reply (to reach the quorum)
    private boolean waitingQuorum = false;
    // partial quorum counter
    private Integer quorum = 0;
    // the quorum that has to be reached (it changes based on read or write)
    private Integer quorumThreshold = 0;
    // the new value to be updated
    private String newValue = null;
    // the reference of the client to respond to after the quorum operation
    private ActorRef clientReferenceRequest = null;
    // contains the read responses from the issued nodes
    private ArrayList<OperationMessage> readResponseMessages = new ArrayList<>();

    // A cancellable returned from the scheduler which lets us cancel the scheduled message
    private Cancellable scheduledTimeoutMessageCancellable;

    public NodeActor(Integer id, Integer n, Integer r, Integer w, String storagePath) {
        this.idKey = id;
        this.N = n;
        this.R = r;
        this.W = w;
        this.Q = Math.max(this.R, this.W);
        this.storagePath = storagePath;

        assert W + R > N;

        // Now have to initialize current NodeUtilities.Ring class to manage Peers.
        ring = new Ring();

        this.nodeActorLogger.setLevel(DynamoLogger.LOG_LEVEL.INFO);
    }

    /**
     *
     * Broadcast a message to every Peer in the system, save the local node
     * @param message The message to be sent
     * @param logMessage The message to be printed to CLI
     */
    private void broadcastToPeers(Object message, String logMessage){
        for (Map.Entry<Integer, Peer> entry : ring.getPeers().entrySet()) {
            Peer peer = entry.getValue();
            Integer key = entry.getKey();
            // we do not send a message to ourselves
            if (!this.idKey.equals(key)) {
                peer.getRemoteSelection().tell(message, getSelf());
                if (logMessage != null){
                    nodeActorLogger.debug(logMessage);
                }
            }
        }
    }

    /**
     * Send a message to the replicas responsible for
     * data item with a certain key
     * @param message the message to be sent (must implement Serializable interface)
     * @param itemKey the key of the data item
     */
    private void sendMessageToReplicas(Object message, Integer itemKey) {
        for (Peer p : ring.getReplicasFromKey(this.N, itemKey)){
            p.getRemoteSelection().tell(message, getSelf());
            // getContext().actorSelection(p.getRemotePath()).tell(message, getSelf());
            nodeActorLogger.debug("Sent message {} to Node {} ({})",
                    message.toString(), p.getKey(), p.getRemotePath());
        }
    }

    /**
     * Sends a read request for a certain item to all of the N next nodes
     * @param itemKey the item's key to retrieve
     */
    private void handleClientReadRequest(Integer itemKey) {
        nodeActorLogger.debug("handleClientReadRequest: itemKey {}", itemKey);
        OperationMessage readRequest = new OperationMessage(false, true, true, itemKey, null);
        // send a retrieve message to each one of the replicas (check if one of these is SELF)
        waitingQuorum = true;
        sendMessageToReplicas(readRequest, itemKey);
    }

    /**
     * Returns the item with newer version number from the responses from the replicas
     *
     * @return an Item object with value, key and version number
     */
    private Item getLatestVersionItemFromResponses() {
        nodeActorLogger.debug("getLatestVersionItemFromResponses");
        OperationMessage max = readResponseMessages.get(0);
        for (OperationMessage msg : readResponseMessages){
            if (msg.getVersion() > max.getVersion()){
                max = msg;
            }
        }
        return new Item(max.getKey(), max.getValue(), max.getVersion());
    }

    /**
     * Decides what Item to send back to the client between the ones received by the replicas in the system, and then sends it.
     */
    private void handleReadResponseToClient() {
        Item latest = getLatestVersionItemFromResponses();
        // Send response to client
        OperationMessage response = new OperationMessage(
                false,
                false,
                true,
                latest.getKey(),
                latest.getValue(),
                latest.getVersion());
        nodeActorLogger.debug("handleReadResponseToClient: message {} sent to client",
                response.toString());
        clientReferenceRequest.tell(response, getSelf());
    }

    /**
     * Sends success message to client and then tell the replicas to update their data item with the new value and latest version number.
     *
     * @param item the Item to be updated
     */
    private void issueUpdateToReplicas(Item item){
        if (item == null) {
            item = getLatestVersionItemFromResponses();
        }

        // send success response to client
        OperationMessage clientResponse = new OperationMessage(
                false,
                false,
                true,
                null,
                "success",
                null);
        clientReferenceRequest.tell(clientResponse, getSelf());
        nodeActorLogger.debug("issueUpdateToReplicas: message {} sent to client",
                clientResponse.toString());
        // issue update to replicas
        Integer newVersion = item.getVersion() + 1;
        OperationMessage issueUpdate = new OperationMessage(
                false,
                true,
                false,
                item.getKey(),
                this.newValue,
                newVersion);
        // send update message to replicas
        nodeActorLogger.debug("issueUpdateToReplicas: call sendMessageToReplicas with message {}",
                issueUpdate.toString());
        sendMessageToReplicas(issueUpdate, item.getKey());
    }


    /**
     * Request to a remote actor it list of peers to have knowledge of the network
     * This method is blocking, i.e. it waits for the response from the remote actor
     * and upon receiving it, it instantiated a new Ring object and copied the received
     * list of Peers (adding also itself to the list)
     * It is ok to make this method blocking because the network still does not have
     * knowledge about this actor, so it is not possible to receive messages while we
     * are waiting.
     * @param remotePath The path of the remote actor
     */
    private void requestPeersToRemote(String remotePath) throws Exception {
        final Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        ActorSelection remoteActor = getContext().actorSelection(remotePath);
        final Future<Object> future = Patterns.ask(remoteActor,
                new PeersListMessage(true, this.ring.getPeers()), timeout);

        nodeActorLogger.debug("requestPeersToRemote: waiting for response from {}", remotePath);

        // wait for an acknowledgement
        final Object message = Await.result(future, timeout.duration());
        assert message instanceof PeersListMessage;

        PeersListMessage msg = (PeersListMessage) message;
        assert !msg.isRequest();

        ring.addPeers(msg.getPeers());

        nodeActorLogger.info("requestPeersToRemote: initialized Ring with {} peers",
                this.ring.getNumberOfPeers());
    }

    /**
     * Send a message to everyone in the network (except to self) to announce the new node.
     */
    private void announceSelfToSystem() {
        // send a hello message to everyone.
        HelloMatesMessage message = new HelloMatesMessage(getContext().actorSelection(self().path()),
                this.idKey, this.remotePath);
        String logMessage = "announceSelfToSystem: sent HelloMatesMessage to remote Node with key " + this.idKey;
        this.broadcastToPeers(message, logMessage);
    }

    /**
     * Send a message to every one in the network (except to self) to account we are leaving the system.
     */
    private void leaveSystem(){
        // send a leave message to everyone
        ByeMatesMessage message = new ByeMatesMessage(this.idKey, this.storage.getStorage());
        String logMessage = "leaveSystem: send ByematesMessage to remote Node with key " + this.idKey;
        this.broadcastToPeers(message, logMessage);
    }

    private void resetVariables() {
        // stop the quorum operation
        this.quorum = 0;
        this.quorumThreshold = 0;
        this.waitingQuorum = false;
        this.readResponseMessages.clear();
        this.clientReferenceRequest = null;
        this.newValue = null;
    }

    /**
     * Schedule a TimeoutMessage to self after
     * @param time How many time units to wait
     * @param unit Specific time unit to use
     */
    private void scheduleTimeout(Integer time, TimeUnit unit) {
        this.scheduledTimeoutMessageCancellable = getContext().system().scheduler().scheduleOnce(
                Duration.create(time, unit),
                getSelf(), new TimeoutMessage(), getContext().system().dispatcher(), getSelf());
        nodeActorLogger.debug("scheduleTimeout: scheduled timeout in {} {}",
                time, unit.toString());
    }

    public void onReceive(Object message) throws Exception {
        nodeActorLogger.debug("Received Message {}", message.toString());

        // class name is represented as dynamo.messages.className, so split and take last element.
        switch (message.getClass().getName().split("[.]")[2]) {
            case "StartJoinMessage": // from actor system, request to join network
                /*
                if Node is the first one, initialize the storage and generate a new key
                Also add self to the Ring. In the case we have to wait for the Peers
                to decide for the new key (else branch) we have to wait adding this new node
                to the ring (because it needs the new key).
                 */
                if(((StartJoinMessage) message).getRemoteIp() == null) {
                    // first node in the system, generate the id
                    if (this.idKey == null) {
                        // if the ID was not set manually by the client
                        this.idKey = ThreadLocalRandom.current().nextInt(1, 100);
                    }
                    ring.addPeer(new Peer(this.remotePath, context().actorSelection(self().path()),  this.idKey));
                    System.out.println("Node started and waiting for messages (" + DynamoLogger.ANSI_GREEN + "id : " + this.idKey + DynamoLogger.ANSI_RESET + ")");
                    storagePath = storagePath + "/dynamo_storage_node" + this.idKey + ".dynamo";
                    // initialize local storage
                    this.storage = new Storage(this.storagePath);
                } else {
                    // otherwise, ask for peers
                    String remotePath = "akka.tcp://dynamo@"+
                            ((StartJoinMessage) message).getRemoteIp() + ":" +
                            ((StartJoinMessage) message).getRemotePort() + "/user/node";

                    requestPeersToRemote(remotePath);
                    // once we have the list of peers if the client did not specify an ID
                    // we can generate this node's key checking it does not collide with an existing one
                    if (this.idKey == null) {
                        // if the ID was not set automatically by the client
                        do {
                            this.idKey = ThreadLocalRandom.current().nextInt(1, 100);
                        } while(this.ring.keyExists(this.idKey));
                    }else {
                        if (this.ring.keyExists(this.idKey)){
                            this.nodeActorLogger.error("Key already exists in the system");
//                            throw new Exception("Key already exists in the system");
                            context().system().terminate();
                            break;
                        }
                    }
                    // add self to the ring
                    ring.addPeer(new Peer(this.remotePath, context().actorSelection(self().path()),  this.idKey));
                    // Print current state of ring
                    nodeActorLogger.info(ring.toString());
                    storagePath = storagePath + "/dynamo_storage_node" + this.idKey + ".dynamo";
                    // initialize local storage
                    this.storage = new Storage(this.storagePath);
                    // Here we request the items we are responsible for to the
                    // next node in the ring
                    this.ring.getNextPeer(this.idKey).getRemoteSelection().tell(
                            new RequestInitItemsMessage(true, this.idKey),
                            getSelf()
                    );
                }
                // nodeActorLogger.info("Initialized node unique key (key: {})", this.idKey);
                break;
            case "HelloMatesMessage":
                // Here the nodes registers the info about the new peer and
                // deletes items from its storage if necessary
                Peer peer = new Peer(
                        ((HelloMatesMessage) message).getRemotePath(),
                        ((HelloMatesMessage) message).getRemoteSelection(),
                        ((HelloMatesMessage) message).getKey());
                ring.addPeer(peer);
                nodeActorLogger.info("Added {} to local ring", peer.toString());
                // Print current state of ring
                nodeActorLogger.info(ring.toString());

                if (this.ring.getNumberOfPeers() > this.N){
                    this.storage.removeItemsOutOfResponsibility(this.idKey, this.ring, this.N);
                }
                nodeActorLogger.info(this.storage.toString());
                break;
            case "LeaveMessage":
                // send message to everyone that we are leaving. Send also local storage alongside
                // interested peers will pick it up.
                this.leaveSystem();
                // send response to client and shutdown system
                getSender().tell(new LeaveMessage(), getSelf());
                context().system().terminate();
                break;
            case "ByeMatesMessage":
                /*
                    So one node in the network told us it is leaving.
                    The leaving node sent this message to all other Nodes so we have
                    to check if we are among the next N clockwise ones, in such
                    case we have to take care of the data it has passed, otherwise we just
                    remove it from our topology
                 */
                Integer senderKey = ((ByeMatesMessage) message).getKey();
                ArrayList<Item> senderStorage = ((ByeMatesMessage) message).getItems();
                boolean removed = ring.removePeer(senderKey);

                if (!removed){
                    this.nodeActorLogger.error("Ring did not contain a Peer with key " + senderKey);
//                    throw new Exception("Ring did not contain a Peer with key " + senderKey);
                    break;
                }

                nodeActorLogger.info(this.ring.toString());

                /*
                Just the next N clockwise peers should check for the incoming storage
                Indeed all the others do not care about the storage of this leaving node.
                Moreover, if we have less/equal than N (before removing the leaving peer -
                that's why we use > N - 1) nodes in the system we just skip all this
                 */
                if (this.ring.getNumberOfPeers() > (N - 1)  &&
                        this.ring.selfIsNextNClockwise(senderKey, this.N, this.idKey)){
                    for(Item item: senderStorage) {
                        /*
                        If a node is the Nth clockwise node from the leaving one, then it will
                        insert a new item in its storage, fine. In the case this node is 'nearer'
                        to the leaving one, then it will update its current item in case the leaving node
                        has one with a newer version number. This thing was not required in the assignment
                        but it comes easy with our code and indeed it seem a reasonable thing to do.
                         */
                        if(this.ring.isNodeWithinRangeFromItem(item.getKey(), this.idKey, this.N)){
                            storage.update(item.getKey(), item.getValue(), item.getVersion());
                        }
                    }
                }
                nodeActorLogger.info(this.storage.toString());
                break;
            case "PeersListMessage":
                PeersListMessage reply = new PeersListMessage(false, this.ring.getPeers());
                getSender().tell(reply, getSelf());
                break;
            case "RequestInitItemsMessage":
                RequestInitItemsMessage msg = ((RequestInitItemsMessage)message);

                if (msg.isRequest()){
                    /*
                    Here a new node is requesting the items it is responsible for.
                    In case the number of nodes is less (not equal, because with the new one
                     we have N + 1 node) than N, then the new node will need ALL the local items.
                    In case there are already >= N nodes, then the new one will need all the nodes
                    this one stores, EXCEPT the items with key GREATER than the new node's key and
                    LESS OR EQUAL than the current node. This is because this node has to loose management
                    of the 'local' items with key less than the new node and it has also to pass ALL the replicas
                    it contains from the other Peers. Indeed, if this node has a replica, then it is obvious
                    that the previous node will have that replica too.
                     */
                    ArrayList<Item> responseItems;
                    if (this.ring.getNumberOfPeers() < N) {
                         responseItems = this.storage.getStorage();
                    } else {
                        /*
                        Here we need to get all the items of this node EXCEPT the ones with key in range
                        between the new node's key and the current node key.
                         */
                        responseItems = this.storage.getItemsForNewNode(msg.getSenderKey(), this.idKey);
                    }
                    RequestInitItemsMessage response = new RequestInitItemsMessage(false, responseItems);
                    getSender().tell(response, getSelf());

                    // When the new node will receive its data, it will announce itself to the system
                    // At that time every other node will check what item should be deleted from the storage.
                    // So for now this node does not do anything on its storage yet, it will once the new node
                    // officially announces itself to the system.

                } else { // isResponse (from the next clockwise node)
                    // Here we receive the data sent from the next node, this is all the data present in the system
                    // that we are responsible for.
                    this.storage.initializeStorage(((RequestInitItemsMessage) message).getItems());
                    nodeActorLogger.info(this.storage.toString());

                    // Now that we have initialized the storage, we can announce this new node to the system
                    announceSelfToSystem();
                }
                break;
            case "OperationMessage":
                if (this.N > this.ring.getNumberOfPeers()){
                    this.nodeActorLogger.error("N is greater than the number of active nodes.");
                    break;
//                    throw new Exception("N is greater than the number of active nodes.");
                }
                OperationMessage opMessage = (OperationMessage) message;
                if (opMessage.isClient()){
                    // if the message is coming from the client it must be a request
                    assert opMessage.isRequest();
                    /*
                     So we have received a read/write operation from the client.
                     So we have to contact the nodes responsible for the specified item
                     to retrieve the data.
                    */
                    // save a reference to the client to be used to respond later
                    this.clientReferenceRequest = getSender();
                    if (opMessage.isRead()){
                        this.readOperation = true;
                        this.quorumThreshold = this.R;
                    } else{
                        this.quorumThreshold = this.Q;
                        this.readOperation = false;
                        this.newValue = opMessage.getValue();
                    }
                    this.scheduleTimeout(2, TimeUnit.SECONDS);
                    this.handleClientReadRequest(opMessage.getKey());
                } else{ // isNode
                    if (opMessage.isRequest()){
                        if (opMessage.isRead()){
                            // A node is requiring a data item
                            Item item = storage.getItem(opMessage.getKey());
                             // In case there is no item with this key, return the message with null
                            // version number. In this way the coordinator can issue an update
                            // to all replicas with version number 1 and the item will be created.
                            if (item == null) {
                                nodeActorLogger.debug("Respond with item=null");
                                getSender().tell(new OperationMessage(false, false,
                                        true, opMessage.getKey(), null, null),
                                        getSelf());
                            } else {
                                nodeActorLogger.debug("Respond with {}", item.toString());
                                getSender().tell(new OperationMessage(false, false,
                                        true, item.getKey(), item.getValue(), item.getVersion()),
                                        getSelf());
                            }
                        } else{ // isUpdate
                            this.storage.update(opMessage.getKey(), opMessage.getValue(), opMessage.getVersion());
                            nodeActorLogger.info(this.storage.toString());
                        }
                    } else{
                        // we can have responses just from read requests, not from update requests
                        assert opMessage.isRead();
                        /*
                         waitingQuorum is true in case this Node sent a
                         ReadMessage to other nodes. So it is waiting
                         to have at least R replies before sending the response back to
                         the client
                        */
                        if (waitingQuorum){
                            OperationMessage response = (OperationMessage) message;
                            if (response.getVersion() == null && readOperation){
                                nodeActorLogger.debug("{} node does not have this item", getSender());
                            } else {
                                this.quorum++;
                                this.readResponseMessages.add((OperationMessage) message);
                                /*
                                 if we have reached the read quorum, send response
                                 to client and reset variables. Here clearly we assume that
                                 a Node can handle just one read request from a client at a time.
                                */
                                if (quorum.equals(this.quorumThreshold)){
                                    if (this.readOperation){
                                        // respond to the client with the proper item
                                        this.handleReadResponseToClient();
                                    } else{
                                        // Q nodes have sent a response. Now we have to check if ALL of these responses
                                        // are null (which means that no replica has this element yet > do an insert), otherwise
                                        // we just send the update with the latest received item

                                        // check for null responses
                                        boolean isNull = true;
                                        for (OperationMessage m : readResponseMessages){
                                            if (m.getVersion() != null){
                                                isNull = false;
                                            }
                                        }
                                        // in case all the messages were null, we just sent the new item (insert operation)
                                        // to all replicas
                                        if (isNull){
                                            Item newItem = new Item(response.getKey(),
                                                    null, 0); // the version will become 1 before sending to replicas
                                            // send to replicas the new element.
                                            this.issueUpdateToReplicas(newItem);
                                        } else { // otherwise we send the update with the latest version
                                            this.issueUpdateToReplicas(null);
                                        }
                                    }
                                    resetVariables();
                                    this.scheduledTimeoutMessageCancellable.cancel();
                                }
                            }
                        }else {
                            // do nothing for now. Wait for other responses.
                            nodeActorLogger.debug("Received an {} with waitingQuorum=false. Message Ignored.", opMessage.toString());
                        }
                    }
                }
                break;
            case "TimeoutMessage":
                // if we are still waiting for some nodes to respond but too much time has passed
                if (waitingQuorum) {
                    // delete the upcoming scheduled Timeout
                    this.scheduledTimeoutMessageCancellable = null;
                    OperationMessage clientResponse = new OperationMessage(
                            false,
                            false,
                            true,
                            null,
                            "failure",
                            null);
                    clientReferenceRequest.tell(clientResponse, getSelf());
                    resetVariables();
                }else {
                    clientReferenceRequest.tell(new TimeoutMessage(), getSelf());
                    resetVariables();
                }
                break;
            case "RecoveryMessage":
                RecoveryMessage recMessage = ((RecoveryMessage)message);

                // if I received a recovery request from User interface
                if (recMessage.getRemoteIp() != null) {

                    String remotePath = "akka.tcp://dynamo@"+
                            recMessage.getRemoteIp() + ":" +
                            recMessage.getRemotePort() + "/user/node";

                    this.requestPeersToRemote(remotePath);
                    // add self to ring
                    this.ring.addPeer(new Peer(this.remotePath, context().actorSelection(self().path()),  this.idKey), true);

                    String logMessage = "announce changing parameters: sent RecoveryMessage to remote Node with key " + this.idKey;
                    recMessage = new RecoveryMessage(this.remotePath, context().actorSelection(self().path()), recMessage.getRequesterId());
                    this.broadcastToPeers(recMessage, logMessage);

                    // Print current state of ring
                    this.nodeActorLogger.info(ring.toString());
                    this.storagePath = storagePath + "/dynamo_storage_node" + this.idKey + ".dynamo";

                    // initialize local storage
                    this.storage = new Storage(this.storagePath);
                    boolean load = storage.loadItems();

                    if(!load) {
                        this.nodeActorLogger.error("Cannot load the Items from the local Storage");
//                        throw new Exception("Cannot load the Items from the local Storage");
                        break;
                    } else {
                        System.out.println("Items correctly loaded from local Storage");
                    }

                    this.storage.removeItemsOutOfResponsibility(this.idKey, this.ring, this.N);
                    this.nodeActorLogger.info(this.storage.toString());
                } else { // if I received a recovery request from the Node
                    this.ring.getPeer(recMessage.getRequesterId()).setRemotePath(recMessage.getRemotePath());
                    this.ring.getPeer(recMessage.getRequesterId()).setRemoteSelection(recMessage.getActorSelection());
                    // log the state of the ring
                    this.nodeActorLogger.info(ring.toString());
                }
                break;
            default:
                unhandled(message);
                break;
        }
    }
}
