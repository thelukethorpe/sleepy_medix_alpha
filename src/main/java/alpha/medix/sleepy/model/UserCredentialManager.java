package alpha.medix.sleepy.model;

import alpha.medix.sleepy.utils.HashingUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserCredentialManager {

  private final Map<String, String> usernameToHashedPassword;

  public UserCredentialManager() {
    usernameToHashedPassword = new HashMap<>();
  }

  public List<UserCredentials> toList() {
    return usernameToHashedPassword
        .entrySet()
        .stream()
        .map(entry -> {
          String username = entry.getKey();
          String hashedPassword = entry.getValue();
          return new UserCredentials(username, hashedPassword);
        })
        .collect(Collectors.toList());
  }

  public void updateWith(List<UserCredentials> userCredentialsList) {
    usernameToHashedPassword.clear();
    userCredentialsList.forEach(userCredentials -> usernameToHashedPassword
        .put(userCredentials.getUsername(), userCredentials.getHashedPassword()));
  }

  private String hash(String password) {
    return HashingUtils.sha256(password);
  }

  public boolean registerNewUser(String username, String password) {
    if (usernameToHashedPassword.containsKey(username)) {
      return false;
    }
    String hashedPassword = hash(password);
    usernameToHashedPassword.put(username, hashedPassword);
    return true;
  }

  public boolean verifyExistingUser(String username, String password) {
    String hashedPassword = hash(password);
    return Objects.equals(hashedPassword, usernameToHashedPassword.get(username));
  }
}
