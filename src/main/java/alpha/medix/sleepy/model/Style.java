package alpha.medix.sleepy.model;

public enum Style {
  TEXT_COLOUR("fx-text-fill"),
  TEXT_WEIGHT("fx-font-weight"),
  TEXT_STYLE("fx-font-style"),
  TEXT_SIZE("fx-font-size");

  Style(String property) {
    this.property = property;
  }

  private final String property;

  public static String css(String... styleProperties) {
    return String.join("\n", styleProperties);
  }

  public String setTo(String value) {
    return String.format("-%s: %s;", property, value);
  }
}