package server.cluster;

import common.Utils;

import java.io.*;
import java.util.*;

public class LogHandler {
    /**
     * Compare this node logs with the logs from a new node relative to their recency.
     * @param newLogs
     * @param newNodeId
     * @param folderPath
     * @param nodeId
     * @return
     */
    public static boolean isMoreRecent(HashMap<String, Integer> newLogs, String newNodeId, String folderPath, String nodeId) {
        int score = 0;

        HashMap<String, Integer> currLogs = buildLogsMap(folderPath);

        Set<String> logs = new HashSet<>(currLogs.keySet());
        logs.addAll(newLogs.keySet().stream().toList());

        for (final String currNodeId : logs) {
            int currCounter = -1;
            int newCounter = -1;
            if (currLogs.containsKey(currNodeId))
                currCounter = currLogs.get(currNodeId);

            if (newLogs.containsKey(currNodeId))
                newCounter = newLogs.get(currNodeId);

            if (newCounter > currCounter)
                score += 1;
            else if (currCounter > newCounter)
                score -= 1;
        }

        if (score == 0) {
            // Consider the node with lower id to be the most recent
            return newNodeId.compareTo(nodeId) < 0;
        }

        return score > 0;
    }

    public static HashMap<String, Integer> buildLogsMap(String folderPath) {
        File file = new File(folderPath + Utils.membershipLogFileName);
        HashMap<String, Integer> nodesMap = new HashMap<>();

        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] lineData = line.split(" ");
                nodesMap.put(lineData[0], Integer.parseInt(lineData[1]));
            }
        } catch (IOException e) {
            return nodesMap;
        }

        return nodesMap;
    }
}
