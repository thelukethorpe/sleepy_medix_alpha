package alpha.medix.sleepy.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashingUtils {

  public static String sha256(String text) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    byte[] hashedBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
    return new String(hashedBytes);
  }

}
