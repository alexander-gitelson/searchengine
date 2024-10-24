package searchengine;

import lombok.SneakyThrows;

import java.security.MessageDigest;

public class MD5 {

    @SneakyThrows
    public static String hash(String input) {

        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(input.getBytes());
        byte[] bytes = digest.digest();

        StringBuilder MD5Hash = new StringBuilder();

        for (byte b : bytes) {
            MD5Hash.append(String.format("%02x", b));
        }
        return MD5Hash.toString();
    }
}
