import akka.actor.UntypedActor;
import messages.*;

/**
 * The actor the client exploits to be able to interact with the network
 */
public class ClientActor extends UntypedActor{

    private String remotePath;

    /**
     * initialize the ClientActor with the address and the port of the coordinator
     * @param address the address of the coordinator
     * @param port the port of the coordinator
     */
    public ClientActor(String address, String port) {
        remotePath = "akka.tcp://mysystem@"+address+":"+port+"/user/node";
    }

    /**
     * send an update request to the coordinator
     * @param key key of the Item to be updated
     * @param value the new value of the Item
     */
    public void update(int key, String value) {
        if (remotePath != null) {
            getContext().actorSelection(remotePath).tell(new Update(key, value), getSelf());
        }
    }

    /**
     * send a get request to the coordinator
     * @param key key of the needed Item
     */
    public String get(int key) {
        if (remotePath != null) {
            getContext().actorSelection(remotePath).tell(new Get(key), getSelf());
        }
    }

    /**
     * send a leave request to the coordinator
     */
    public void leave() {
        if (remotePath != null) {
            getContext().actorSelection(remotePath).tell(new Leave(), getSelf());
        }
    }

    public void onReceive(Object message) {
        if(message instanceof Object) {

        } else {
            unhandled(message);
        }
    }
}
