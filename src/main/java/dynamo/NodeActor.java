package dynamo;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import com.sun.corba.se.spi.orb.Operation;
import dynamo.messages.*;
import dynamo.nodeutilities.Item;
import dynamo.nodeutilities.Peer;
import dynamo.nodeutilities.Ring;
import dynamo.nodeutilities.Storage;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NodeActor extends UntypedActor{

    // For know we hard code these values
    // Think about maybe reading them form the config at
    // actor initialization
    private Integer N = 0;
    private Integer R = 0;
    private Integer W = 0;
    private Integer Q = 0;

    // The identifier of the dynamo.NodeActor.
    private Integer idKey = 0;
    private String remotePath = null;

    // Where all the peers are stored.
    private Ring ring = null;

    // Where all the data items are stored.
    private Storage storage = null;

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
    private Integer quorumTreshold = 0;
    // the new value to be updated
    private String newValue = null;
    // the reference of the client to respond to after the quorum operation
    private ActorRef clientReferenceRequest = null;
    // contains the read responses from the issued nodes
    private ArrayList<OperationMessage> readResponseMessages = new ArrayList<>();

    // A cancellable returned from the scheduler which lets us cancel the scheduled message
    private Cancellable scheduledTimeoutMessageCancellable;

    public NodeActor(Integer n, Integer r, Integer w) {
        this.N = n;
        this.R = r;
        this.W = w;
        this.Q = Math.max(this.R, this.W);

        assert W + R > N;
    }

    /**
     * Send a message to the replicas responsible for
     * data item with a certain key
     * @param message the message to be sent (must implement Serializable interface)
     * @param itemKey the key of the data item
     */
    private void sendMessageToReplicas(Object message, Integer itemKey) {
        for (Peer p : ring.getReplicasFromKey(this.N, itemKey)){
            if (p.getRemotePath().equals("self")){
                // TODO: handle message to self
                // message to self should work like this
                getSelf().tell(message, getSelf());
            } else {
                // TODO: check if we can send message to self with remote path, if true we can remove this if statement
                getContext().actorSelection(p.getRemotePath()).tell(message, getSelf());
            }
        }
    }

    /**
     * Sends a read request for a certain item to all of the N next nodes
     * @param itemKey the item's key to retrieve
     */
    private void handleClientReadRequest(Integer itemKey) {
        OperationMessage readRequest = new OperationMessage(false, true, true, itemKey);
        // send a retrieve message to each one of the replicas (check if one of these is SELF)
        sendMessageToReplicas(readRequest, itemKey);
    }

    /**
     * Returns the item with newer version number from
     * the responses from the replicas
     * @return an Item object with value, key and version number
     */
    private Item getLatestVersionItemFromResponses() {
        int v = 0;
        OperationMessage max = readResponseMessages.get(0);
        for (OperationMessage msg : readResponseMessages){
            if (msg.getVersion() > max.getVersion()){
                max = msg;
            }
        }
        return new Item(max.getKey(), max.getValue(), max.getVersion());
    }

    /**
     * Decides what item to send back to the client between the ones
     * received by the replicas in the system.
     * Then sends the item.
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
        clientReferenceRequest.tell(response, getSelf());
    }

    /**
     * Send success message to client and then tell the replicas
     * to update their data item with the new value and latest version number.
     */
    private void issueUpdateToReplicas(){
        Item latest = getLatestVersionItemFromResponses();
        // send success resposnse to client
        OperationMessage clientResponse = new OperationMessage(
                false,
                false,
                true,
                null,
                "success",
                null);
        clientReferenceRequest.tell(clientResponse, getSelf());
        // issue update to replicas
        Integer newVersion = latest.getVersion() + 1;
        OperationMessage issueUpdate = new OperationMessage(
                false,
                true,
                false,
                latest.getKey(),
                this.newValue,
                newVersion);
        // send update message to replicas
        sendMessageToReplicas(issueUpdate, latest.getKey());
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
     * @throws Exception
     */
    private void requestPeersToRemote(String remotePath) throws Exception {
        final Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        ActorSelection remoteActor = getContext().actorSelection(remotePath);
        final Future<Object> future = Patterns.ask(remoteActor,
                new PeersListMessage(true), timeout);

        // wait for an acknowledgement
        final Object message = Await.result(future, timeout.duration());
        assert message instanceof PeersListMessage;

        PeersListMessage msg = (PeersListMessage) message;
        assert !msg.isRequest();

        // the message returns a list of remotePaths and ids
        // from this we get the Remote reference to the actor

        // Now have to initialize current NodeUtilities.Ring class to manage Peers.
        ring = new Ring();

        ring.setPeers(msg.getPeers());
        // add self to the ring
        ring.addPeer(new Peer(this.remotePath, this.idKey));
    }

    /**
     * Request to the next node in the network the data items
     * that need to pass to our competence. This method is blocking, which means
     * that the actor waits for the reply from the remote actor. This is possible
     * because no other node in the network knows of the existence of this node yet.
     * So it is not possible to receive other messages while we are waiting for
     * this reply.
     * @throws Exception
     */
    private void requestItemsToNextNode() throws Exception {
        //first get the next node
        String remotePathNext = ring.getNextPeer(this.idKey).getRemotePath();
        final Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        ActorSelection remoteActor = getContext().actorSelection(remotePathNext);
        final Future<Object> future = Patterns.ask(remoteActor,
                new RequestInitItemsMessage(true, this.idKey, this.remotePath), timeout);

        // wait for an acknowledgement
        final Object message = Await.result(future, timeout.duration());
        assert message instanceof RequestInitItemsMessage;

        RequestInitItemsMessage msg = (RequestInitItemsMessage) message;
        assert !msg.isRequest();

        // now instantiate local storage with received data
        this.storage = new Storage(msg.getItems());
    }

    /**
     * Send a message to everyone in the network (except to self)
     * to announce the new node.
     */
    private void announceSelfToSystem() {
        // here send a hello message to everyone.
        HelloMatesMessage message = new HelloMatesMessage(this.idKey, this.remotePath);
        for (Map.Entry<Integer, Peer> entry : ring.getPeers().entrySet()) {
            Peer peer = entry.getValue();
            Integer key = entry.getKey();
            // we do not send a message to ourselves
            if (!this.idKey.equals(key)) {
                getContext().actorSelection(peer.getRemotePath()).tell(message, getSelf());
            }
        }
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
    }

    public void onReceive(Object message) throws Exception {
        switch (message.getClass().getName()) {
            case "StartJoinMessage": // from actor system, request to join network
                String remotePath = "akka.tcp://mysystem@"+
                        ((StartJoinMessage) message).getRemoteIp() + ":" +
                        ((StartJoinMessage) message).getRemotePort() + "/user/node";
                requestPeersToRemote(remotePath);
                // Here we request the items we are responsible for to the
                // next node in the ring
                requestItemsToNextNode();
                // Here we announce our presence to the whole system
                announceSelfToSystem();
                break;
            case "HelloMatesMessage":
                // Here the nodes registers the info about the new peer and
                // deletes items from its storage if necessary
                Peer peer = new Peer(
                        ((HelloMatesMessage) message).getRemotePath(),
                        ((HelloMatesMessage) message).getKey());
                ring.addPeer(peer);
                // TODO: delete from storage unnecessary items, we have to delete all item with key SMALLER than the remoteKey (smaller or equal?)
                // TODO: Useful API in Storage to delete all items smaller that a certain key
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
                boolean removed = ring.removePeer(senderKey);
                assert removed;

                if (ring.selfIsNextNClockwise(senderKey, this.N, this.idKey)){
                    // TODO: assume control of the relevant data (to be implemented in Storage class)
                }
                break;
            case "PeerListMessage":
                PeersListMessage reply = new PeersListMessage(false, ring.getPeers());
                getSender().tell(reply, getSelf());
                break;
            case "RequestInitItemsMessage":
                // TODO: here we have to decide which Items to send back to the sender. So all the items with key that is smaller (or equal?) to the new node's key. Also be careful about the case in which an item has key greater than the greater node key in the system
//                RequestInitItemsMessage reply = new RequestInitItemsMessage()
//                getSender().tell(reply, getSelf());
                break;
            case "OperationMessage":
                // TODO: Check that we do not receive a read/write request while we are handling already another operation
                OperationMessage opMessage = (OperationMessage) message;
                if (opMessage.isClient()){
                    // if the message is coming from the client it must be a request
                    assert opMessage.isRequest();
                    /*
                     So we have received a read operation from the client.
                     So we have to contact the nodes responsible for the specified item
                     to retrieve the data.
                    */
                    // save a reference to the client to be used to respond later
                    this.clientReferenceRequest = getSender();
                    this.readOperation = true;
                    if (opMessage.isRead()){
                        this.quorumTreshold = this.R;
                    } else{
                        this.quorumTreshold = this.Q;
                        this.newValue = opMessage.getValue();
                    }
                    this.scheduleTimeout(2, TimeUnit.SECONDS);
                    this.handleClientReadRequest(opMessage.getKey());
                } else{ // isNode
                    if (opMessage.isRequest()){
                        if (opMessage.isRead()){
                            // A node is requiring a data item
                            // TODO: ask Storage class for the data item with specific key
                            // TODO: handle missing key case
                            // TODO: send data back to sender
                        } else{ // isUpdate
                            // TODO: a node is telling us to update an element in our storage (update the version)
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
                            this.quorum++;
                            this.readResponseMessages.add((OperationMessage) message);
                            /*
                             if we have reached the read quorum, send response
                             to client and reset variables. Here clearly we assume that
                             a Node can handle just one read request from a client at a time.
                            */
                            if (quorum.equals(this.quorumTreshold)){
                                if (this.readOperation){
                                    // respond to the client with the proper item
                                    this.handleReadResponseToClient();
                                } else{
                                    // report success to client and send the correct update to the replicas
                                    this.issueUpdateToReplicas();
                                }
                                this.quorum = 0;
                                this.quorumTreshold = 0;
                                this.waitingQuorum = false;
                                this.readResponseMessages.clear();
                                this.clientReferenceRequest = null;
                            }
                        }else {
                            // do nothing for now. Wait for other responses.
                            // TODO: Schedule a timeout to self at beginning in case we do not receive enough responses? Yes for sure in the case of write.
                        }
                    }
                }
                break;
            case "TimeoutMessage":
                // if we are still waiting for some nodes to respond but too much time has passed
                if (waitingQuorum) {
                    // delete the upcoming scheduled Timeout
                    this.scheduledTimeoutMessageCancellable.cancel();
                    OperationMessage clientResponse = new OperationMessage(
                            false,
                            false,
                            true,
                            null,
                            "failure",
                            null);
                    clientReferenceRequest.tell(clientResponse, getSelf());
                    this.quorum = 0;
                    this.quorumTreshold = 0;
                    this.waitingQuorum = false;
                    this.readResponseMessages.clear();
                    this.clientReferenceRequest = null;
                }else {
                    // we have completed all operation in time, do nothing.
                    System.out.println("LOg that we have received timeout but we don't need it anymore");
                }
                break;
            default:
                unhandled(message);
                break;
        }
    }
}
