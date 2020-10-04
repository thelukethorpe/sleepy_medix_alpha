package alpha.medix.sleepy.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateTimeService {

  private final DateTimeFormatter timeFormatter;

  public DateTimeService() {
    this.timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
  }

  public String currentTime() {
    return LocalDateTime.now().format(timeFormatter);
  }

}
