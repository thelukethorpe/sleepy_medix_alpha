package alpha.medix.sleepy.model;

public class UserCredentials {

  private String username;
  private String hashedPassword;

  public UserCredentials(String username, String hashedPassword) {
    this.username = username;
    this.hashedPassword = hashedPassword;
  }

  public UserCredentials() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getHashedPassword() {
    return hashedPassword;
  }

  public void setHashedPassword(String hashedPassword) {
    this.hashedPassword = hashedPassword;
  }
}
