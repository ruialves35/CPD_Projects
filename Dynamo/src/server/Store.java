package server;

import common.Message;
import common.Utils;
import example.Hello;
import server.cluster.MembershipService;
import server.cluster.Node;
import server.network.TCPListener;
import server.network.UDPListener;
import server.storage.StorageService;
import server.storage.TombstoneManager;
import server.storage.TransferService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Member;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Store implements Server{
    private final String multicastIPAddr;
    private final int multicastIPPort;
    private final String nodeId;
    private final int storePort;
    private final MembershipService membershipService;
    private final StorageService storageService;
    private final TransferService transferService;

    private ExecutorService executorService;

    private ServerSocket serverSocket = null;
    private MulticastSocket multicastSocket = null;

    public Store(String multicastIPAddr, int multicastIPPort, String nodeId, int storePort) {
        this.multicastIPAddr = multicastIPAddr;
        this.multicastIPPort = multicastIPPort;
        this.nodeId = nodeId;
        this.storePort = storePort;

        this.membershipService = new MembershipService(multicastIPAddr, multicastIPPort, nodeId, storePort);
        this.storageService = new StorageService(membershipService.getNodeMap(), nodeId, executorService);
        this.transferService = new TransferService(membershipService.getNodeMap(), storageService, new Node(nodeId, storePort));
    }
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Wrong number of arguments. Please invoke the program as:");
            System.out.println("java Store <IP_mcast_addr> <IP_mcast_port> <node_id> <Store_port>");
            System.exit(1);
        }

        final String multicastIPAddr = args[0];
        final int multicastIPPort = Integer.parseInt(args[1]);
        final String nodeId = args[2];
        final int storePort = Integer.parseInt(args[3]);

        try {
            LocateRegistry.createRegistry(1099);

            Store store = new Store(multicastIPAddr, multicastIPPort, nodeId, storePort);
            Server stub = (Server) UnicastRemoteObject.exportObject(store, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Server", stub);

            System.out.println("Server ready");
        } catch (RemoteException | AlreadyBoundException e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public boolean join() throws RemoteException {
        try {
            // THERE ARE 2 SOLUTIONS: THIS ONE or LEAVING THE THREADS RUNNING BUT NOT PROCESSING THE EVENTS
            try {
                InetAddress addr = InetAddress.getByName(nodeId);
                serverSocket = new ServerSocket(storePort, 50, addr);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            executorService = Executors.newFixedThreadPool(10);    // TODO: Check number of threads
            executorService.submit(new TCPListener(storageService, membershipService, transferService, executorService, serverSocket));


            membershipService.join();
            transferService.join();

            try {
                multicastSocket = new MulticastSocket(multicastIPPort);
                executorService.submit(new UDPListener(storageService, membershipService, transferService, executorService, multicastSocket));
                executorService.submit(new TombstoneManager(storageService.getDbFolder()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch(RuntimeException re) {
            System.err.println(re.getMessage());
            // Clear ExecutorService threads?
        }


        return true;
    }

    @Override
    public boolean leave() throws RemoteException {
        try {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                    serverSocket = null;
                }
                if (multicastSocket != null) {
                    InetSocketAddress group = new InetSocketAddress(multicastIPAddr, multicastIPPort);
                    NetworkInterface netInf = NetworkInterface.getByIndex(0);
                    multicastSocket.leaveGroup(group, netInf);
                    multicastSocket.close();
                    multicastSocket = null;
                }
                executorService.shutdownNow();
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.out.println("Executor terminated.");
                } else {
                    System.out.println("Executor still running");
                }
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            transferService.leave();
            membershipService.leave();
        } catch (RuntimeException re) {
            System.err.println(re.getMessage());
            // Clear ExecutorService threads?
        }

        return true;
    }

    @Override
    public String put(String filePath) throws RemoteException {
        File file = new File(filePath);
        String fileKey = Utils.generateKey(file.getName());

        try {
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            storageService.put(fileKey, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileKey;
    }

    @Override
    public byte[] get(String fileKey) throws RemoteException {
        Message fileMessage  = storageService.get(fileKey);
        return fileMessage.getBody();
    }

    @Override
    public boolean delete(String fileKey) throws RemoteException {
        storageService.delete(fileKey);

        return true;
    }
}
