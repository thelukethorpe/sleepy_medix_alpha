package alpha.medix.sleepy.services;

import alpha.medix.sleepy.model.Options;
import alpha.medix.sleepy.model.Options.Field;
import alpha.medix.sleepy.model.Options.Theme;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

public class OptionsService {

  private static final Options DEFAULT_OPTIONS = Options.where()
      .volumeIsSetTo(Options.VOLUME_MAX)
      .themeIsSetTo(Theme.CLASSIC)
      .usernameIsSetTo("")
      .build();

  private final String optionsPath;

  public OptionsService(String configPath) {
    this.optionsPath = configPath + "/options.properties";
  }

  public Options loadOptionsFromDisk() throws IOException {
    InputStream inputStream = new FileInputStream(optionsPath);
    Properties properties = new Properties();
    properties.load(inputStream);

    double volume = parseVolumeFrom(properties);
    Theme theme = parseThemeFrom(properties);
    String username = parseUsernameFrom(properties);

    return Options.where()
        .volumeIsSetTo(volume)
        .themeIsSetTo(theme)
        .usernameIsSetTo(username)
        .build();
  }

  private double parseVolumeFrom(Properties properties) throws IOException {
    double volume;
    try {
      volume = Double.parseDouble(
          properties.getProperty(
              Field.VOLUME.name(),
              DEFAULT_OPTIONS.getVolume().toString()
          )
      );
      if (volume < Options.VOLUME_MIN || Options.VOLUME_MAX < volume) {
        throw new IOException(
            String.format("Volume option must be between %f and %f.", Options.VOLUME_MIN,
                Options.VOLUME_MAX));
      }
    } catch (NumberFormatException e) {
      throw new IOException("Volume option must be a number.");
    }
    return volume;
  }

  private Theme parseThemeFrom(Properties properties) throws IOException {
    Theme theme;
    try {
      theme = Theme.valueOf(properties.getProperty(
          Field.THEME.name(),
          DEFAULT_OPTIONS.getTheme().name()
      ));
    } catch (IllegalArgumentException e) {
      String possibleThemes = Arrays
          .stream(Theme.values())
          .map(Theme::name)
          .collect(Collectors.joining(", ", "[", "]"));
      throw new IOException(String.format("Theme must be one of: %s.", possibleThemes));
    }
    return theme;
  }

  private String parseUsernameFrom(Properties properties) {
    return properties.getProperty(
        Field.USERNAME.name(),
        DEFAULT_OPTIONS.getUsername()
    );
  }

  public Options loadDefaultOptions() {
    return DEFAULT_OPTIONS.copy();
  }

  public void writeOptionsFileToDisk(Options options) throws IOException {
    OutputStream outputStream = new FileOutputStream(optionsPath);
    Properties properties = new Properties();

    properties.setProperty(Field.VOLUME.name(), options.getVolume().toString());
    properties.setProperty(Field.THEME.name(), options.getTheme().toString());
    properties.setProperty(Field.USERNAME.name(), options.getUsername());

    properties.store(outputStream, "Sleepy Medix Scheduler options file.");
  }

  public Options writeNewOptionsFileToDisk() throws IOException {
    Options defaultOptions = loadDefaultOptions();
    writeOptionsFileToDisk(defaultOptions);
    return defaultOptions;
  }
}
