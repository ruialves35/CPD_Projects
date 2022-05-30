package common;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static final String membershipLogFileName = "membership.log";
    public static final String membershipCounterFileName = "membershipCounter.txt";
    public static final int numLogEvents = 32;
    public static final String newLine = "\r\n";

    public static final int maxResponseTime = 1000;    // TODO: CHECK THIS VALUES
    public static final int timeoutTime = 3000;
    public static final int timeoutStepTime = 500;
    public static final int numMembershipMessages = 3;

    public static String generateKey(final String hashable) {
        return generateKey(hashable.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateKey(final byte[] hashable) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(hashable);
            return String.format("%x", new BigInteger(1, hashBytes));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Invalid MessageDigest algorithm");
            System.exit(1);
        }
        return null;
    }

    public static String generateFolderPath(String nodeId) {
        return "database/" + Utils.generateKey(nodeId) + "/";
    }
}