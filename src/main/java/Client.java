/**
 * This class represents the interface for the client
 */
public class Client {

    private static ClientActor coordinator;

    public static void main(String[] args) {

        if(args.length > 2) {
            String address = args[0];
            String port = args[1];

            coordinator = new ClientActor(address, port);
        } else{
            System.out.println("Sorry, command not found.");
            return;
        }

        if(args[2] == "read" && args.length == 4) {
            coordinator.get(args[3]);
        } else if (args[2] == "write" && args.length == 5) {
            coordinator.update(args[3], args[4]);
        } else if (args[2] == "leave" && args.length == 3) {
            coordinator.leave();
        } else {
            System.out.println("Sorry, command not found.");
            return;
        }
    }
}
