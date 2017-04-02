package dynamo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import dynamo.messages.StartJoinMessage;
import dynamo.nodeutilities.Utilities;

import java.net.InetAddress;

/**
 * Created by StefanoFioravanzo on 15/03/2017.
 */
public class Node {
    public static void main(String[] args){

        String remoteIp = null;
        String remotePort = null;
        Integer localId = null;

        String localIP = null;

        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
//            System.out.println(localIP);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // First we parse the arguments given from CLI
        // If JOIN we have to create a new actor and contact
        // the given remotePath to join the distributed storage system.

        // If RECOVER we will do some recovery action

        // we use a simple if-else parsing since we have a small
        // number of possible input arguments

        String error_msg = "Exactly three parameters are needed!\n" +
                "There are two options available:\n" +
                "\t-java dynamo.Node join remote_ip remote_port local_id\n" +
                "\t-java dynamo.Node recover remote_ip remote_port local_id";
        // Really there is a third option with just one argument (id) for the first actor of dynamo.

        if (args.length > 4 || args.length < 1){
            throw new IllegalArgumentException(error_msg);
        }

        // here goes the logic for joining a new node
        if (args[0].equals("join") || args[0].equals("start")){
            boolean join = false;
            if (args[0].equals("join")){
                join = true;
            }

            // TODO: Have to generate here a unique id key for the node? For now we are taking it from CLI

            if (join){
                remoteIp = args[1];
                remotePort = args[2];
                if (args.length == 4){
                    localId = Integer.parseInt(args[3]);
                }
            } else{
                if (args.length == 2){
                    localId = Integer.parseInt(args[1]);
                }
            }

            Config myConfig = ConfigFactory.load("application");
            // bind the node to a random port
            Integer randomPort = Utilities.getAvailablePort(10000, 10100);
            Config custom = ConfigFactory.parseString("akka.remote.netty.tcp.hostname =" + localIP + ", akka.remote.netty.tcp.port = " + randomPort);

            ActorSystem system = ActorSystem.create("dynamo", custom.withFallback(myConfig));
            System.out.println("ActorSystem started successfully, listening on ip: " + localIP + ", port " + randomPort);
            ActorRef localNode = null;

            // Get replication parameters from config file.
            Integer n = myConfig.getInt("dynamo.replication.N");
            Integer r = myConfig.getInt("dynamo.replication.R");
            Integer w = myConfig.getInt("dynamo.replication.W");
            String path = myConfig.getString("dynamo.storage.location");

            if (r + w < n){
                // Illegal
                throw new IllegalArgumentException("R + W is less than N.");
            }

            // Can extend here the create call with arguments to the
            // constructor of the dynamo.Node class
            localNode = system.actorOf(Props.create(NodeActor.class, localId, n, r, w, path), "node");
            System.out.println("Node started and waiting for messages (id : " + localId + ")");

            /*
            if is the starting Node, remoteIp and remotePort == null. When the node receives
            the StartJoinMessage, if these parameters are null it knows that it is the first
            node of the system. So it does not try to contact anyone and instantiates an empty storage.
             */
            localNode.tell(new StartJoinMessage(remoteIp, remotePort), null);

        } else if (args[0].equals("recover")) {
            throw new IllegalArgumentException("Not yet implemented.");
            // TODO
        } else {
             throw new IllegalArgumentException("Argument not recognized.");
        }
    }
}