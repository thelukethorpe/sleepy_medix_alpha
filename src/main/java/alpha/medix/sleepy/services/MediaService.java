package alpha.medix.sleepy.services;

import java.io.File;
import java.net.MalformedURLException;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class MediaService {

  private final String pathToAssets;

  public MediaService(String pathToAssets) {
    this.pathToAssets = pathToAssets;
  }

  private String getNormalizedPathFromFilename(String filename) {
    String pathToFile = pathToAssets + filename;
    File file = new File(pathToFile);
    try {
      return file.toURI().toURL().toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Failed to parse media at \"" + pathToFile + ".\"");
    }
  }

  public MediaPlayer loadVideoLoop(String filename) {
    Media video = new Media(getNormalizedPathFromFilename(filename));

    MediaPlayer player = new MediaPlayer(video);
    player.setCycleCount(MediaPlayer.INDEFINITE);

    return player;
  }

  public Image loadImage(String filename) {
    return new Image(getNormalizedPathFromFilename(filename));
  }

}
