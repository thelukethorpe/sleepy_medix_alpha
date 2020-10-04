package alpha.medix.sleepy.services;

import alpha.medix.sleepy.model.UserCredentials;
import alpha.medix.sleepy.utils.JsonUtils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class UserCredentialsService {

  private final String userCredentialsPath;

  public UserCredentialsService(String configPath) {
    this.userCredentialsPath = configPath + "/user_credentials.json";
  }

  public List<UserCredentials> loadUserCredentialsFromDisk() throws IOException {
    InputStream inputStream = new FileInputStream(userCredentialsPath);
    String fileContents = new String(inputStream.readAllBytes());
    inputStream.close();
    return Arrays.asList(JsonUtils.fromJson(fileContents, UserCredentials[].class).orElseThrow(
        () -> new IOException(String.format("Failed to parse %s", userCredentialsPath))));
  }

  public void writeUserCredentialsToDisk(List<UserCredentials> userCredentialList)
      throws IOException {
    OutputStream outputStream = new FileOutputStream(userCredentialsPath);
    String fileContents = JsonUtils.toJson(userCredentialList.toArray());
    outputStream.write(fileContents.getBytes());
    outputStream.flush();
    outputStream.close();
  }

  public List<UserCredentials> writeNewUserCredentialManagerFileToDisk() throws IOException {
    List<UserCredentials> userCredentialsList = new LinkedList<>();
    writeUserCredentialsToDisk(userCredentialsList);
    return userCredentialsList;
  }
}
