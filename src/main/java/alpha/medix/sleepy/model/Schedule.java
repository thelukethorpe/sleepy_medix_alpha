package alpha.medix.sleepy.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Schedule {

  private final String username;
  private final Type type;
  private final long workDurationInMinutes;
  private final LocalDateTime timeOfCreation;
  private final SortedMap<LocalTime, Activity> startTimeToActivity;

  public Schedule(String username, Type type, long workDurationInMinutes) {
    this.username = username;
    this.type = type;
    this.workDurationInMinutes = workDurationInMinutes;
    this.timeOfCreation = LocalDateTime.now();
    this.startTimeToActivity = new TreeMap<>();
  }

  public String getUsername() {
    return username;
  }

  public long getWorkDurationInMinutes() {
    return workDurationInMinutes;
  }

  public Type getType() {
    return type;
  }

  public void addActivity(LocalTime startTime, Activity activity) {
    startTimeToActivity.put(startTime, activity);
  }

  public List<Slot> getSlots() {
    return startTimeToActivity.entrySet().stream()
        .map(entry -> new Slot(entry.getKey(), entry.getValue())).collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return String.format("Schedule created at %s.",
        timeOfCreation.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
  }

  public enum Type {
    ASTRONAUT("Astronaut");

    private final String displayText;

    Type(String displayText) {
      this.displayText = displayText;
    }

    public String displayText() {
      return displayText;
    }
  }

  public static class Activity {

    private final String title;
    private final List<String> tips;

    private Activity(String title, List<String> tips) {
      this.title = title;
      this.tips = tips;
    }

    public static Activity of(String title, String... tips) {
      return new Activity(title, List.of(tips));
    }

    public String getTitle() {
      return title;
    }

    public List<String> getTips() {
      return tips;
    }

    @Override
    public int hashCode() {
      return Objects.hash(title, tips);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof Activity) {
        Activity that = (Activity) obj;
        return this.title.equals(that.title) && this.tips.equals(that.tips);
      }
      return false;
    }
  }

  public static class Slot {

    private final LocalTime startTime;
    private final Activity activity;

    public Slot(LocalTime startTime, Activity activity) {
      this.startTime = startTime;
      this.activity = activity;
    }

    public LocalTime getStartTime() {
      return startTime;
    }

    public Activity getActivity() {
      return activity;
    }

    @Override
    public String toString() {
      return String.format("%s at %s", activity.title,
          startTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
    }
  }
}
