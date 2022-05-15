package client;

public class TestClient {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java TestClient <node_ap> <operation> [<opnd>]");
            System.exit(1);
        }

        // Either <IP address>:<port number> (TCP/UDP) or object's name (RMI)
        final String nodeAP = args[0];
        final String operation = args[1];
        final String operand = args.length == 3 ? args[2] : null;

        if (operand == null && operation.equals("put")) {
            System.out.println("The put operation requires the file pathname to be provided");
            System.exit(1);
        }

        if (operand == null && (operation.equals("get") || operation.equals("delete"))) {
            System.out.println("The " + operation + " requires a key to be provided");
            System.exit(1);
        }
    }
}
