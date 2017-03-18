package dynamo;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dynamo.messages.GetMessage;
import dynamo.messages.LeaveMessage;
import dynamo.messages.OperationMessage;
import scala.concurrent.Future;
import scala.concurrent.Await;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;

/**
 * This class represents the interface for the client
 */
public class Client {

    private String remotePath;
    private ActorSystem system;
    private int timeoutInSeconds = 5;

    public Client(String address, String port) {
        // load configuration from application.conf file
        final Config config = ConfigFactory.load();

        this.remotePath = "akka.tcp://mysystem@"+address+":"+port+"/user/node";
        this.system = ActorSystem.create("system", config);
    }
    public static void main(String[] args) {

        Client newClient;

        if(args.length > 2) {
            String address = args[0];
            String port = args[1];

            newClient = new Client(address, port);
        } else{
            System.out.println("Sorry, command not found.");
            return;
        }

        if(args[2] == "read" && args.length == 4) {
            newClient.get(Integer.parseInt(args[3]));
        } else if (args[2] == "write" && args.length == 5) {
            newClient.update(Integer.parseInt(args[3]), args[4]);
        } else if (args[2] == "leave" && args.length == 3) {
            newClient.leave();
        } else {
            System.out.println("Sorry, command not found.");
            return;
        }
    }

    /**
     * send an update request to the coordinator
     * @param key key of the NodeUtilities.Item to be updated
     * @param value the new value of the NodeUtilities.Item
     */
    public void update(int key, String value) {
        sendRequest(new OperationMessage(true, true, false, key, value));
    }

    /**
     * send a get request to the coordinator
     * @param key key of the needed NodeUtilities.Item
     */
    public void get(int key) {
        sendRequest(new GetMessage(key));
    }

    /**
     * send a leave request to the coordinator
     */
    public void leave() {
        sendRequest(new LeaveMessage());
    }

    private void sendRequest(Object message) {
        if (remotePath != null) {

            // connect to the remote actor
            final ActorSelection remoteActor = system.actorSelection(remotePath);

            Timeout timeout = new Timeout(Duration.create(timeoutInSeconds, "seconds"));
            Future<Object> future = Patterns.ask(remoteActor, message, timeout);

            try {
                // Await.result is blocking
                final Object result = Await.result(future, timeout.duration());

                if (result instanceof OperationMessage) {
                    OperationMessage msg = (OperationMessage) result;
                    if(msg.isRead()) {
                        System.out.println("Node returned item with "
                                + "\n\tkey: " + msg.getValue()
                                + "\n\tvalue: " + msg.getValue()
                                + "\n\tversion: " + msg.getVersion());
                    } else {
                        System.out.println("Node updated successfully with "
                                + "\n\tkey: " + msg.getValue()
                                + "\n\tvalue: " + msg.getValue()
                                + "\n\tversion: " + msg.getVersion());
                    }
                } else if(result instanceof LeaveMessage) {
                    System.out.println("Node left successfully Dynamo");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
