package alpha.medix.sleepy.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ScheduleManager {
  private final Map<String, List<Schedule>> usernameToSchedules;

  public ScheduleManager() {
    this.usernameToSchedules = new HashMap<>();
  }

  public List<Schedule> getSchedulesFor(String username) {
    return usernameToSchedules.get(username);
  }

  public void add(String username, Schedule schedule) {
    List<Schedule> schedules = getSchedulesFor(username);
    if (schedules == null) {
      schedules = new LinkedList<>();
      usernameToSchedules.put(username, schedules);
    }
    schedules.add(schedule);
  }
}
