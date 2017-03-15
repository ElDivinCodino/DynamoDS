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
    private ArrayList<ReadResponseMessage> readResponseMessages = new ArrayList<ReadResponseMessage>();

    private void handleReadRequest(Integer itemKey) {
        ArrayList<Peer> replicas = ring.getReplicasFromKey(N, itemKey);

        // send a retrieve message to each one of the replicas (check if one of these if SELF)


        // ->> wait for the response???
        // no we do not want to do this blocking because we may receive other dynamo.messages in the mean while.
    }

    private void handleReadResponseToClient() {
        int v = 0;
        String value = null;
        for (ReadResponseMessage msg : readResponseMessages){
            if (msg.getVersion() > v){
                value = msg.getValue();
            }
        }

        // TODO: Here send to the client the response.
    }

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
                // TODO: delete from storage unnecessary items, first check with ring class from which key we have to delete
                // TODO: Useful API in Storage to delete all items smaller that a certain key
                break;
            case "PeerListMessage":
                PeersListMessage reply = new PeersListMessage(false, ring.getPeers());
                getSender().tell(reply, getSelf());
                break;
            case "RequestInitItemsMessage":
                // TODO: here we have to decide which Items to send back to the sender.
//                RequestInitItemsMessage reply = new RequestInitItemsMessage()
//                getSender().tell(reply, getSelf());
                break;
            case "ReadRequestMessage": // client > node (request a read)
                handleReadRequest(((ReadRequestMessage) message).getKey());
                break;
            case "RequestItemMessage": // node > node (request an item)
                break;
            case "ResponseItemMessage": // node > node (node returns an item)
                /*
                 waitingReadQuorum is true in case this Node sent a
                 RequestItemMessage to other nodes. So it is waiting
                 to have at least R replies before sending the response back to
                 the client
                */
                if (waitingReadQuorum){
                    readQuorum++;
                    readResponseMessages.add((ReadResponseMessage) message);
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
            case "WriteRequestMessage":
                break;
            default:
                unhandled(message);
                break;
        }
    }
}
