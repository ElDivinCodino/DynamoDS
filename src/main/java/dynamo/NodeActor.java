package dynamo;

import dynamo.messages.*;
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

public class NodeActor extends UntypedActor{

    // For know we hard code these values
    // Think about maybe reading them form the config at
    // actor initialization
    private Integer N = 0;
    private Integer R = 0;
    private Integer W = 0;

    // The identifier of the dynamo.NodeActor.
    private Integer idKey = 0;
    private String remotePath = null;

    // Where all the peers are stored.
    private Ring ring = null;

    // Where all the data items are stored.
    private Storage storage = null;

    private boolean waitingReadQuorum = false;
    private Integer readQuorum = 0;
    private ArrayList<ReadOperationMessage> readResponseMessages = new ArrayList<ReadOperationMessage>();

    private void handleReadRequest(Integer itemKey) {
        ArrayList<Peer> replicas = ring.getReplicasFromKey(N, itemKey);

        // send a retrieve message to each one of the replicas (check if one of these if SELF)


        // ->> wait for the response???
        // no we do not want to do this blocking because we may receive other dynamo.messages in the mean while.
    }

    private void handleReadResponseToClient() {
        int v = 0;
        String value = null;
        for (ReadOperationMessage msg : readResponseMessages){
            if (msg.getVersion() > v){
                value = msg.getValue();
            }
        }

        // TODO: Here send to the client the response.
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
            case "ReadOperationMessage":
                ReadOperationMessage readMessage = (ReadOperationMessage) message;
                if (readMessage.isClient()){
                    // if the message is coming from the client it must be a request
                    assert readMessage.isRequest();
                    /*
                     So we have received a read operation from the client.
                     So we have to contact the nodes responsible for the specified item
                     to retrieve the data.
                    */
                    handleReadRequest(readMessage.getKey());
                } else{ // isNode
                    if (readMessage.isRequest()){
                        // A node is requiring a data item
                        // TODO
                    } else{
                        /*
                         waitingReadQuorum is true in case this Node sent a
                         RequestItemMessage to other nodes. So it is waiting
                         to have at least R replies before sending the response back to
                         the client
                        */
                        if (waitingReadQuorum){
                            readQuorum++;
                            readResponseMessages.add((ReadOperationMessage) message);
                            /*
                             if we have reached the read quorum, send response
                             to client and reset variables. Here clearly we assume that
                             a Node can handle just one read request from a client at a time.
                            */
                            if (readQuorum.equals(R)){
                                this.handleReadResponseToClient();
                                readQuorum = 0;
                                readResponseMessages.clear();
                                waitingReadQuorum = false;
                            }
                        }else {
                            // do nothing for now.
                        }
                    }
                }
                break;
            case "WriteRequestMessage":
                break;
            default:
                unhandled(message);
                break;
        }
    }
}
