package dynamo;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dynamo.messages.LeaveMessage;
import dynamo.messages.OperationMessage;
import dynamo.messages.TimeoutMessage;
import dynamo.nodeutilities.DynamoLogger;
import dynamo.nodeutilities.Utilities;
import scala.concurrent.Future;
import scala.concurrent.Await;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

/**
 * Represents the interface for acting as client
 */
public class Client {

    // used by default as seconds
    final private Integer MESSAGE_TIMEOUT = 5;

    private String remotePath;
    private ActorSystem system;


    public Client(String address, String port) {
        // load configuration from application.conf file
        Config myConfig = ConfigFactory.load("application");

        // bind the client to a random port
        Integer randomPort = Utilities.getAvailablePort(10000, 10100);
        Config custom = ConfigFactory.parseString("akka.remote.netty.tcp.hostname = localhost, akka.remote.netty.tcp.port = " + randomPort);

        this.remotePath = "akka.tcp://dynamo@"+address+":"+port+"/user/node";
        this.system = ActorSystem.create("dynamo", custom.withFallback(myConfig));
    }

    public static void main(String[] args) {

        Client newClient;
        String address = null;
        String port= null;

        if(args.length > 2) {
            address = args[0];
            port = args[1];
        } else{
            System.out.println("Sorry, command not found.");
            System.exit(-1);
        }

        newClient = new Client(address, port);

        if(args[2].equals("read") && args.length == 4) {
            newClient.get(Integer.parseInt(args[3]));
        } else if (args[2].equals("update") && args.length == 5) {
            newClient.update(Integer.parseInt(args[3]), args[4]);
        } else if (args[2].equals("leave") && args.length == 3) {
            newClient.leave();
        } else {
            System.out.println("Sorry, command not found.");
            System.exit(-1);
        }
    }

    /**
     * send an update request to the coordinator
     * @param key key of the NodeUtilities.Item to be updated
     * @param value the new value of the NodeUtilities.Item
     */
    private void update(int key, String value) {
        sendRequest(new OperationMessage(true, true, false, key, value));
    }

    /**
     * send a get request to the coordinator
     * @param key key of the needed NodeUtilities.Item
     */
    private void get(int key) {
        sendRequest(new OperationMessage(true, true, true, key, null));
    }

    /**
     * send a leave request to the coordinator
     */
    private void leave() {
        sendRequest(new LeaveMessage());
    }

    private void sendRequest(Object message) {
        if (remotePath != null) {

            // connect to the remote actor
            final ActorSelection remoteActor = system.actorSelection(remotePath);

            Timeout timeout = new Timeout(Duration.create(MESSAGE_TIMEOUT, TimeUnit.SECONDS));
            Future<Object> future = Patterns.ask(remoteActor, message, timeout);

            try {
                // Await.result is blocking
                final Object result = Await.result(future, timeout.duration());

                if (result instanceof OperationMessage) {
                    OperationMessage msg = (OperationMessage) result;
                    if(msg.isRead()) {
                        System.out.println("Node returned item with "
                                + "\n\tkey: " + msg.getKey()
                                + "\n\tvalue: " + msg.getValue()
                                + "\n\tversion: " + msg.getVersion());
                    } else {
                        System.out.println("Node updated successfully with "
                                + "\n\tkey: " + msg.getKey()
                                + "\n\tvalue: " + msg.getValue()
                                + "\n\tversion: " + msg.getVersion());
                    }
                } else if(result instanceof LeaveMessage) {
                    System.out.println("Node left successfully Dynamo");
                } else if (result instanceof TimeoutMessage) {
                    System.out.println("Timeout has expired");
                }
            } catch (Exception e) {
                System.out.println(DynamoLogger.ANSI_RED + "[TIMEOUT] " + DynamoLogger.ANSI_RESET + "The request did not receive any response.");
                // e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
