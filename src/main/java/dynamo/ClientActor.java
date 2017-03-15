package dynamo;

import akka.actor.UntypedActor;
import dynamo.messages.Get;
import dynamo.messages.Leave;
import dynamo.messages.Timeout;
import dynamo.messages.Update;
import scala.concurrent.duration.Duration;

/**
 * The actor the client exploits to be able to interact with the network
 */
public class ClientActor extends UntypedActor{

    private String remotePath;
    private int time = 2000; // TODO: SET IT CORRECTLY!
    private boolean hasDecided = false;

    /**
     * initialize the dynamo.ClientActor with the address and the port of the coordinator
     * @param address the address of the coordinator
     * @param port the port of the coordinator
     */
    public ClientActor(String address, String port) {
        remotePath = "akka.tcp://mysystem@"+address+":"+port+"/user/node";
    }

    /**
     * send an update request to the coordinator
     * @param key key of the NodeUtilities.Item to be updated
     * @param value the new value of the NodeUtilities.Item
     */
    public void update(int key, String value) {
        sendRequest(new Update(key, value));
    }

    /**
     * send a get request to the coordinator
     * @param key key of the needed NodeUtilities.Item
     */
    public void get(int key) {
        sendRequest(new Get(key));
    }

    /**
     * send a leave request to the coordinator
     */
    public void leave() {
        sendRequest(new Leave());
    }

    public void onReceive(Object message) {
        if(message instanceof Timeout) {
            if (!hasDecided) {
                //TODO what we do if a decision is not taken yet?
            }
        } else if (true){
            //TODO implement all the possible answers coming from the coordinator
        } else {
            unhandled(message);
        }
    }

    private void sendRequest(Object message) {
        if (remotePath != null) {
            getContext().actorSelection(remotePath).tell(message, getSelf());
            setTimeout(time);
        }
    }


    /**
     * schedule a Timeout message in specified time
     * @param time the time (in milliseconds) before the timeout happens
     */
    private void setTimeout(int time) {
        getContext().system().scheduler().scheduleOnce(
                Duration.create(time, java.util.concurrent.TimeUnit.MILLISECONDS), getSelf(), new Timeout(), getContext().system().dispatcher(), getSelf()
        );
    }
}