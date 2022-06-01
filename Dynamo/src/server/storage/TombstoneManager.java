package server.storage;

import server.Constants;

import java.io.File;

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
                if (System.currentTimeMillis() - file.lastModified() > Constants.tombstoneExpirationMS) {
                    if (!file.delete()) System.out.println("Error deleting tombstone file: "+ file.getName());
                    String realFilePath = dbFolder + "/" + file.getName();
                    File realFile = new File(realFilePath);
                    if (!realFile.delete()) System.out.println("Error deleting real file: " + realFile.getName());
                }
            }
        }
    }
}
