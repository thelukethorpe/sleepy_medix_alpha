package alpha.medix.sleepy.model;

public class Options {

  public static final double VOLUME_MIN = 0.0d;
  public static final double VOLUME_MAX = 1.0d;

  public enum Field {
    VOLUME,
    THEME,
    USERNAME;
  }

  private Theme theme;

  private double volume;
  private String username;

  public Options(Theme theme, double volume, String username) {
    this.theme = theme;
    this.volume = volume;
    this.username = username;
  }

  public Options copy() {
    return new Options(theme, volume, username);
  }

  public Theme getTheme() {
    return theme;
  }

  public Double getVolume() {
    return volume;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void updateTo(Options that) {
    this.theme = that.theme;
    this.volume = that.volume;
    this.username = that.username;
  }

  public enum Theme {
    CLASSIC("Classic");

    private final String displayText;

    Theme(String displayText) {
      this.displayText = displayText;
    }

    public String displayText() {
      return displayText;
    }
  }

  public static OptionsBuilder where() {
    return new OptionsBuilder();
  }

  public static class OptionsBuilder {

    private Theme theme;
    private double volume;
    private String username;

    public OptionsBuilder themeIsSetTo(Theme theme) {
      this.theme = theme;
      return this;
    }

    public OptionsBuilder volumeIsSetTo(double volume) {
      this.volume = volume;
      return this;
    }

    public OptionsBuilder usernameIsSetTo(String username) {
      this.username = username;
      return this;
    }

    public Options build() {
      return new Options(theme, volume, username);
    }
  }
}
