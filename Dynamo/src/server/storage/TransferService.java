package server.storage;

import server.Utils;
import server.cluster.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class TransferService {
    private final TreeMap<String, Node> nodeMap;

    public TransferService(TreeMap<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public boolean join(Node node) {
        // TODO Must transfer the files that are of the new node
        //   from the next node to it, since they were stored over there atm

        String key = Utils.generateKey(node.getId());
        System.out.println("Node: " + key);

        Node nextNode = this.getNextNode(key);
        String nextKey = "a6864eb339b0e1f6e00d75293a8840abf069a2c0fe82e6e53af6ac099793c1d5"; // Utils.generateKey(nextNode.getId());
        File[] nodeFiles = this.getNodeFiles(key, nextKey, true);

        if (nodeFiles != null) {
            for (final File fileEntry : nodeFiles) {
                String fileName = fileEntry.getName();
                String fileHash = Utils.generateKey(fileName);
                if (fileHash.compareTo(Utils.generateKey(node.getId())) < 0)
                    // TODO Send the file with put to the new node
                    System.out.println("Name: " + fileName + " hash: " + fileHash);
            }
        }

        return true;
    }

    public void leave(Node node) {
        // TODO When a node leaves, it must put the files on next node
        // TODO Change this to get the key from the node received by argument (it's just for tests)

        String key = Utils.generateKey(node.getId());
        System.out.println("Node: " + key);

        Node nextNode = this.getNextNode(key);
        String nextKey = "a6864eb339b0e1f6e00d75293a8840abf069a2c0fe82e6e53af6ac099793c1d5"; // Utils.generateKey(nextNode.getId());
        File[] nodeFiles = this.getNodeFiles(key, nextKey, true);

        if (nodeFiles != null) {
            for (final File fileEntry : nodeFiles) {
                String fileName = fileEntry.getName();
                String fileHash = Utils.generateKey(fileName);
                if (fileHash.compareTo(Utils.generateKey(node.getId())) > 0)
                    // TODO should i read the file with get and then send with put?
                    System.out.println("Name: " + fileName + " hash: " + fileHash);
            }
        }

    }

    /**
     *  Gets next node to node with key. If there is no node with higher key value,
     *  then gets the first node, so it behaves like a circular map
     * @param key key of the node that we want to get the next node
     * @return Next Node
     */
    public Node getNextNode(String key) {
        // Get the next node to store the files
        Map.Entry<String, Node> nextEntry = nodeMap.higherEntry(key);
        if (nextEntry == null) nextEntry = nodeMap.firstEntry();

        Node nextNode = nextEntry.getValue();

        return nextNode;
    }

    public File[] getNodeFiles(String nodeKey, String nextNodeKey, boolean isJoin) {

        // if it's join then we want to get the files of the next node
        // if it's leave we want to get the files of the current node
        String folderPath = "database/" + (isJoin ? nextNodeKey : nodeKey) + "/";
        File folder = new File(folderPath);
        File[] nodeFiles = folder.listFiles();
        return nodeFiles;
    }

}
