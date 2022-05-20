package server;

import server.cluster.MembershipService;
import server.network.TCPListener;
import server.network.UDPListener;
import server.storage.StorageService;
import server.storage.TransferService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Store {
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port> [isRootNode]");
            System.exit(1);
        }

        final String multicastIPAddr = args[0];
        final int multicastIPPort = Integer.parseInt(args[1]);
        final String nodeId = args[2];
        final int storePort = Integer.parseInt(args[3]);

        boolean isRootNode = false;
        if (args.length == 5) {
            isRootNode = Boolean.parseBoolean(args[4]);
        }

        final MembershipService membershipService = new MembershipService(multicastIPAddr, multicastIPPort, nodeId, isRootNode);
        final StorageService storageService = new StorageService(membershipService.getNodeMap(), nodeId);
        final TransferService transferService = new TransferService(membershipService.getNodeMap());
        final ExecutorService executorService = Executors.newCachedThreadPool();

        executorService.submit(new TCPListener(storageService, membershipService, transferService, executorService, nodeId, storePort));

        if (membershipService.join()) {
            executorService.submit(new UDPListener(storageService, membershipService, transferService, executorService));
        }

        // TODO Adapt this to the client
        /*
        Path path = Paths.get("./Utils.java");
        byte[] data = Files.readAllBytes(path);
        String key = storageService.put(data);
        System.out.println("Key = " + key);

        byte[] data = storageService.get("df1847064eaf9321457a8090bbac85c084925f30ba9ac3f2f631960569d7f37f");
        try (FileOutputStream fos = new FileOutputStream("file")) {
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        storageService.delete("df1847064eaf9321457a8090bbac85c084925f30ba9ac3f2f631960569d7f37f");
        */
    }
}
