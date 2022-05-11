package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Utils {
    public static String generateKey(final String hashable) {
        return generateKey(hashable.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateKey(final byte[] hashable) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Arrays.toString(digest.digest(hashable));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Invalid MessageDigest algorithm");
            System.exit(1);
        }
        return null;
    }
}
