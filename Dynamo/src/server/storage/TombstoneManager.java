package server.storage;

import server.Constants;

import java.io.*;

public class TombstoneManager implements Runnable {
    private final File tombstoneFolder;
    private final String dbFolder;

    public TombstoneManager(String dbFolder) {
        String tombstonePath = dbFolder + "/tombstones/";
        this.dbFolder = dbFolder;
        this.tombstoneFolder = new File(tombstonePath);
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(Constants.tombstoneCheckIntervalMS);
            } catch (InterruptedException e) {
                System.out.println("Error sleeping tombstone manager");
                e.printStackTrace();
            }

            File[] tombstones = tombstoneFolder.listFiles();
            if (tombstones == null || tombstones.length == 0) {
                continue;
            }

            for (File file : tombstones) {
                try (DataInputStream fis = new DataInputStream(new FileInputStream(file))) {
                    long timestamp = fis.readLong();

                    if (System.currentTimeMillis() - timestamp > Constants.tombstoneExpirationMS) {
                        String realFilePath = dbFolder + "/" + file.getName();
                        File realFile = new File(realFilePath);
                        if (!realFile.exists()) {
                            System.out.println("File corresponding to the tombstone does not exist: " + realFile.getName());
                            break; // It's possible to receive a delete request before the respective put request
                        }
                        if (!file.delete()) System.out.println("Error deleting tombstone file: "+ file.getName());
                        if (!realFile.delete()) System.out.println("Error deleting real file: "+ realFile.getName());
                    }
                } catch (IOException e) {
                    System.out.println("Error opening tombstone file in manager: " + file.getName());
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
