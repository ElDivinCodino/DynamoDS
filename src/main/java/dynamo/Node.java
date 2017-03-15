package dynamo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import dynamo.messages.StartJoinMessage;

/**
 * Created by StefanoFioravanzo on 15/03/2017.
 */
public class Node {
    public static void main(String[] args){

        String remoteIp = null;
        String remotePort = null;

        ActorSystem system = ActorSystem.create("Dynamo");
        ActorRef localNode = null;

        // First we parse the arguments given from CLI
        // If JOIN we have to create a new actor and contact
        // the given remotePath to join the distributed storage system.

        // If RECOVER we will do some recovery action

        // we use a simple if-else parsing since we have a small
        // number of possible input arguments

        String error_msg = "Exactly two parameters are needed!\n" +
                "There are two options available:" +
                "\t-java dynamo.Node join remote_ip remote_port" +
                "\t-java dynamo.Node recover remote_ip remote_port";

        if (args.length != 2){
            throw new IllegalArgumentException(error_msg);
        }

        // here joes the logic for joining a new node
        if (args[0].equals("join")){
            remoteIp = args[1];
            remotePort = args[2];

            // TODO: add parameters N, W, R to the config file and pass them to the create method
            // TODO: Have to generate here a unique id key for the node?

            // Can extend here the crete call with arguments to the
            // constructor of the dynamo.Node class
            localNode = system.actorOf(Props.create(Node.class));
            localNode.tell(new StartJoinMessage(), null);
        }

        if (args[0].equals("recover")) {
            throw new IllegalArgumentException("Not yet implemented.");
            // TODO
        }else{
            throw new IllegalArgumentException("Argument not recognized.");
        }
    }
}