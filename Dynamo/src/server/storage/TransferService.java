package server.storage;

import common.Message;
import common.Sender;
import common.Utils;
import server.Constants;
import server.cluster.Node;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransferService {
    private final StorageService storageService;
    private final Node node;
    public TransferService(StorageService storageService, Node node) {
        this.storageService = storageService;
        this.node = node;
    }

    public void join() {
        String key = Utils.generateKey(this.node.getId());
        Node nextNode = storageService.getNextNode(this.node);
        String nextKey = Utils.generateKey(nextNode.getId());
        ArrayList<String> nextNodeFiles;

        if (nextKey.equals(key)) return;

        try {
            nextNodeFiles = this.getNodeFileNames(nextNode);
        } catch (IOException e) {
            System.out.println("Could not get the files from the next node on Join.");
            throw new RuntimeException(e);
        }

        if (processJoin(nextNodeFiles, this.node, nextNode)) {
            System.out.println("Sent files from " + nextNode.getId() + " to " + this.node.getId());
        } else {
            System.out.println("Error sending files to joined node");
        }

    }

    public void leave() {
        File folder = new File(storageService.getDbFolder());
        ArrayList<String> folderFiles = new ArrayList<>(List.of(Objects.requireNonNull(folder.list())));

        Node curNode = this.node;
        String curKey = Utils.generateKey(curNode.getId());
        Node receivingNode = curNode;
        String receivingKey;

        for (int i = 0; i < Constants.replicationFactor; ++i) {
            receivingNode = this.storageService.getNextNode(receivingNode);
            receivingKey = Utils.generateKey(receivingNode.getId());
            // Replication is redundant if there are less than replicationFactor nodes left
            if (curKey.equals(receivingKey)) return;
        }

        for (int i = 0; i < Constants.replicationFactor; ++i) {
            ArrayList<String> filesToTransfer = filterResponsibleFiles(folderFiles, curNode);
            sendNodeFiles(filesToTransfer, receivingNode);
            curNode = storageService.getPreviousNode(curNode);
            receivingNode = storageService.getPreviousNode(receivingNode);
        }
    }


    /**
     * Creates a Message request to save a file
     * @param file file to be saved
     * @return if everything went well Message with saveFile action,
     *         otherwise Message with error action
     */
    private Message createMsgFromFile(File file) {
        try (FileInputStream fis = new FileInputStream(file.getPath())) {

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(file.getName().getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(fis.readAllBytes());

            return new Message("REQ", "saveFile", out.toByteArray());
        } catch (IOException e) {
            System.out.println("Error opening file in get operation: " + file.getPath());
            throw new RuntimeException(e);
            //return new Message("REP", "error", null);
        }
    }


    /**
     * Processes a join by checking if next node has files from this node
     * if it has, then request to get and delete those files, and store
     * them in this node
     * @param nextNodeFiles array with the name of the files stored in nextNode
     * @param node node that's joining or leaving
     * @param nextNode nextNode to this node in the membership
     * @return true if no errors occur, false otherwise
     */
    private boolean processJoin(ArrayList<String> nextNodeFiles, Node node, Node nextNode) {
        if (nextNodeFiles.size() != 0) {
            for (final String fileName : nextNodeFiles) {
                try {
                    String key = Utils.generateKey(node.getId());
                    String nextNodeKey = Utils.generateKey(nextNode.getId());

                    // file possibly belongs to the node
                    if (fileName.compareTo(key) <= 0) {

                        if (key.compareTo(nextNodeKey) < 0 || fileName.compareTo(nextNodeKey) > 0) {
                            Message requestFile = new Message("REQ", "getAndDelete", fileName.getBytes(StandardCharsets.UTF_8));
                            byte[] response = Sender.sendTCPMessage(requestFile.toBytes(), nextNode.getId(), nextNode.getPort());
                            Message responseMsg = new Message(response);
                            storageService.saveFile(key, responseMsg.getBody());
                        }

                    }

                } catch (IOException e) {
                    System.out.println("Could not get the files from the next node on Join.");
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }


    /**
     * Sends the files in nodeFiles to a node
     * @param fileNames array with the file names to send
     * @param node node to which we want to send the files
     */
    private void sendNodeFiles(ArrayList<String> fileNames, Node node) {
        for (String fileName : fileNames) {
            try {
                File file = new File(storageService.getDbFolder() + "/" + fileName);
                Message msg = createMsgFromFile(file);
                Sender.sendTCPMessage(msg.toBytes(), node.getId(), node.getPort());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * gets the name of the files in a node by requesting it through TCP
     * @param node node from which we want to get the files' names
     * @return array with the name of the files stored in node's database
     */
    private ArrayList<String> getNodeFileNames(Node node) throws IOException {
        Message message = new Message("REQ", "getFiles", Utils.generateKey(node.getId()).getBytes(StandardCharsets.UTF_8));

        Message reply = new Message(Sender.sendTCPMessage(message.toBytes(), node.getId(), node.getPort()));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(reply.getBody())));

        ArrayList<String> fileNames = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            fileNames.add(line);
        }

        return filterResponsibleFiles(fileNames, node);
    }

    private ArrayList<String> filterResponsibleFiles(ArrayList<String> fileNames, Node node) {
        Node prevNode = storageService.getPreviousNode(node);
        String prevNodeKey = Utils.generateKey(prevNode.getId());
        String nodeKey = Utils.generateKey(node.getId());
        for (int i = 0; i < fileNames.size(); ++i) {
            if (fileNames.get(i).compareTo(nodeKey) > 0 || fileNames.get(i).compareTo(prevNodeKey) < 0) {
                fileNames.remove(i);
                --i;
            }
        }

        return fileNames;
    }

}
