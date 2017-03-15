import nodeutilities.Peer;
import nodeutilities.Ring;
import nodeutilities.Storage;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;
import messages.*;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;

public class NodeActor extends UntypedActor{

    // For know we hard code these values
    // Think about maybe reading them form the config at
    // actor initialization
    Integer N = 0;
    Integer R = 0;
    Integer W = 0;

    // The identifier of the NodeActor.
    Integer id_key = 0;

    // Where all the peers are stored.
    Ring ring = null;

    // Where all the data items are stored.
    Storage storage = null;

    boolean waitingReadQuorum = false;
    Integer readQuorum = 0;
    ArrayList<ReadResponseMessage> readResponseMessages = new ArrayList<ReadResponseMessage>();

    private void handleReadRequest(Integer itemKey){
        ArrayList<Peer> replicas = ring.getNextNodesFromKey(N, itemKey);

        // send a retrieve message to each one of the replicas (check if one of these if SELF)


        // ->> wait for the response???
        // no we do not want to do this blocking because we may receive other messages in the mean while.
    }

    private void handleReadRespondeToClient(){
        int v = 0;
        String value = null;
        for (ReadResponseMessage msg : readResponseMessages){
            if (msg.getVersion() > v){
                value = msg.getValue();
            }
        }

        // TODO: Here send to the client the respose.
    }

    private void requestPeersToRemote(String remotePath) throws Exception {
        // Set timeout for the reply
//        final Timeout timeout = new Timeout(Duration.create(5, "seconds"));
//        final Future<Object> future = Patterns.ask(
//                getContext().actorSelection(remotePath),
//                new PeersListRequestMessage(), timeout);

        // Blocking wait for the node to respond with the list of peers
//        final PeersListResponseMessage message = Await.result(future, timeout.duration());


        final Timeout timeout = new Timeout(Duration.create(5, "seconds"));
        ActorSelection remoteActor = getContext().actorSelection(remotePath);
        final Future<Object> future = Patterns.ask(remoteActor,
                new PeersListRequestMessage(), timeout);

        // wait for an acknowledgement
        final Object message = Await.result(future, timeout.duration());
        assert message instanceof PeersListResponseMessage;

        PeersListResponseMessage msg = (PeersListResponseMessage) message;

        // the message returns a list of remotePaths and ids
        // from this we get the Remote reference to the actor

        // Now have to initialize current NodeUtilities.Ring class to manage Peers.
        ring = new Ring();
        for (int i = 0; i < msg.getRemotePaths().size(); i++) {
            ring.addPeer(new Peer(msg.getRemotePaths()[i]))
        }


        // add self to the ring

        // send Hello message to everyone on the network.
    }



    public void onReceive(Object message) throws Exception {
        switch (message.getClass().getName()){
            case "StartJoinMessage": // from actor system, request to join network
                StartJoinMessage msg = (StartJoinMessage) message;
                String remotePath = "akka.tcp://mysystem@"+
                        msg.getRemoteIp() + ":" +
                        msg.getRemotePort() + "/user/node";
                requestPeersToRemote(remotePath);
                break;
            case "JoinRequestMessage":
                JoinReqeustMessage msg = (JoinReqeustMessage) message;
                break;
            case "ReadRequestMessage": // client > node (request a read)
                handleReadRequest(((ReadRequestMessage) message).getKey());
                break;
            case "RequestItemMessage": // node > node (request an item)
                break;
            case "ResponseItemMessage": // node > node (node returns an item)
                if (waitingReadQuorum){
                    readQuorum++;
                    readResponseMessages.add((ReadResponseMessage) message);

                    if (readQuorum == R){
                        this.handleReadRespondeToClient();
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
