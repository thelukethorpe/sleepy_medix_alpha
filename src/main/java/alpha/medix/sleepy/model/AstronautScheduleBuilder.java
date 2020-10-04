package alpha.medix.sleepy.model;

import alpha.medix.sleepy.model.Schedule.Activity;
import alpha.medix.sleepy.model.Schedule.Slot;
import alpha.medix.sleepy.model.Schedule.Type;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AstronautScheduleBuilder {

  private static final int NULL_RESPONSE_VALUE = -1;

  private final List<Integer> chronoTypeQuestionResponseValues;

  public AstronautScheduleBuilder() {
    int expectedNumberOfQuestionResponses = CHRONO_TYPE_QUESTIONS.size();
    chronoTypeQuestionResponseValues = new ArrayList<>(expectedNumberOfQuestionResponses);
    for (int i = 0; i < expectedNumberOfQuestionResponses; i++) {
      chronoTypeQuestionResponseValues.add(NULL_RESPONSE_VALUE);
    }
  }

  public synchronized void setQuestionResponse(int questionIndex, String response) {
    ChronoTypeQuestion chronoTypeQuestion = CHRONO_TYPE_QUESTIONS.get(questionIndex);
    int responseIndex = chronoTypeQuestion.possibleResponses.indexOf(response);
    int responseValue = NULL_RESPONSE_VALUE;
    if (responseIndex >= 0) {
      responseValue = chronoTypeQuestion.start + responseIndex * chronoTypeQuestion.increment;
    }
    chronoTypeQuestionResponseValues.set(questionIndex, responseValue);
  }

  public synchronized boolean isReady() {
    boolean haveAllResponsesBeenProvided = chronoTypeQuestionResponseValues.stream()
        .allMatch(value -> value != NULL_RESPONSE_VALUE);
    return haveAllResponsesBeenProvided;
  }

  public synchronized Schedule buildWith(String username, long workDurationInMinutes) {
    if (!this.isReady()) {
      return null;
    }
    Schedule schedule = new Schedule(username, Type.ASTRONAUT, workDurationInMinutes);
    int chronoTypeScore = chronoTypeQuestionResponseValues.stream().reduce(0, Integer::sum);
    if (chronoTypeScore < 31) {
      addTimetableForDefiniteEvening(schedule, workDurationInMinutes);
    } else if (chronoTypeScore < 42) {
      addTimetableForModerateEvening(schedule, workDurationInMinutes);
    } else if (chronoTypeScore < 58) {
      addTimetableForIntermediate(schedule, workDurationInMinutes);
    } else if (chronoTypeScore < 69) {
      addTimetableForModerateMorning(schedule, workDurationInMinutes);
    } else {
      addTimetableForDefiniteMorning(schedule, workDurationInMinutes);
    }
    return schedule;
  }

  public static Schedule updateWithNap(Schedule schedule) {
    List<Slot> slots = schedule.getSlots();
    Map<Activity, LocalTime> activityToStartTime = slots.stream()
        .collect(Collectors.toMap(Slot::getActivity, Slot::getStartTime));

    Schedule updatedSchedule = new Schedule(schedule.getUsername(), Type.ASTRONAUT,
        schedule.getWorkDurationInMinutes());
    LocalTime bedTime = activityToStartTime.get(SLEEP_ACTIVITY);

    addTimetableWithNap(updatedSchedule, bedTime, updatedSchedule.getWorkDurationInMinutes());
    return updatedSchedule;
  }

  private void addTimetableForDefiniteEvening(Schedule schedule, long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, LocalTime.of(1, 30), workDurationInMinutes);
  }

  private void addTimetableForModerateEvening(Schedule schedule, long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, LocalTime.of(0, 30), workDurationInMinutes);
  }

  private void addTimetableForIntermediate(Schedule schedule, long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, LocalTime.of(23, 30), workDurationInMinutes);

  }

  private void addTimetableForModerateMorning(Schedule schedule, long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, LocalTime.of(23, 15), workDurationInMinutes);
  }

  private void addTimetableForDefiniteMorning(Schedule schedule, long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, LocalTime.of(21, 45), workDurationInMinutes);
  }

  private void addTimetableWithoutNap(Schedule schedule, LocalTime bedTime,
      long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, bedTime, workDurationInMinutes, false);
  }

  private static void addTimetableWithNap(Schedule schedule, LocalTime bedTime,
      long workDurationInMinutes) {
    addTimetableWithoutNap(schedule, bedTime, workDurationInMinutes, true);
  }

  private static void addTimetableWithoutNap(Schedule schedule, LocalTime bedTime,
      long workDurationInMinutes, boolean napIsNeeded) {
    LocalTime wakeupTime = bedTime.plusMinutes(SLEEP_DURATION);
    Activity wakeupActivity = Activity.of("Wake Up");
    schedule.addActivity(wakeupTime, wakeupActivity);

    LocalTime breakfastTime = wakeupTime.plusMinutes(WAKEUP_DURATION);
    Activity breakfastActivity = Activity.of("Breakfast", BREAKFAST_TIPS);
    schedule.addActivity(breakfastTime, breakfastActivity);

    long morningDurationInMinutes = Duration.ofHours(5).toMinutes();
    long afternoonDurationInMinutes = Duration.ofDays(1).toMinutes() - morningDurationInMinutes
        - ESSENTIAL_ACTIVITY_DURATION_IN_MINUTES;

    LocalTime morningTime = breakfastTime.plusMinutes(BREAKFAST_DURATION);
    if (workDurationInMinutes > 0) {
      Activity workActivity = Activity.of("Work Before Lunch");
      schedule.addActivity(morningTime, workActivity);

      // TODO verbose logic but it works
      if (workDurationInMinutes < morningDurationInMinutes) {
        LocalTime freeTime = morningTime.plusMinutes(workDurationInMinutes);
        Activity freeActivity = Activity.of("Free Time Before Lunch");
        schedule.addActivity(freeTime, freeActivity);
      }

    } else {
      Activity freeActivity = Activity.of("Free Time Before Lunch");
      schedule.addActivity(morningTime, freeActivity);
    }

    LocalTime lunchTime = morningTime.plusMinutes(morningDurationInMinutes);
    Activity lunchActivity = Activity.of("Lunch", LUNCH_TIPS);
    schedule.addActivity(lunchTime, lunchActivity);

    LocalTime afternoonTime;
    if (napIsNeeded) {
      LocalTime napTime = lunchTime.plusMinutes(LUNCH_DURATION);
      Activity napActivity = NAP_ACTIVITY;
      schedule.addActivity(napTime, napActivity);
      afternoonTime = napTime.plusMinutes(NAP_DURATION);
    } else {
      afternoonTime = lunchTime.plusMinutes(LUNCH_DURATION);
    }

    if (workDurationInMinutes <= morningDurationInMinutes) {
      Activity freeActivity = Activity.of("Free Time After Lunch");
      schedule.addActivity(afternoonTime, freeActivity);
    } else {
      Activity workActivity = Activity.of("Work After Lunch");
      schedule.addActivity(afternoonTime, workActivity);

      LocalTime freeTime = afternoonTime
          .plusMinutes(workDurationInMinutes - morningDurationInMinutes);
      Activity freeActivity = Activity.of("Free Time After Lunch");
      schedule.addActivity(freeTime, freeActivity);
    }

    LocalTime exerciseTime = afternoonTime.plusMinutes(afternoonDurationInMinutes);
    Activity exerciseActivity = Activity.of("Exercise");
    schedule.addActivity(exerciseTime, exerciseActivity);

    LocalTime dinnerTime = exerciseTime.plusMinutes(EXERCISE_DURATION);
    Activity dinnerActivity = Activity.of("Dinner", DINNER_TIPS);
    schedule.addActivity(dinnerTime, dinnerActivity);

    LocalTime restTime = dinnerTime.plusMinutes(DINNER_DURATION);
    Activity restActivity = Activity.of("Rest", REST_TIPS);
    schedule.addActivity(restTime, restActivity);

    Activity sleepActivity = SLEEP_ACTIVITY;
    schedule.addActivity(bedTime, sleepActivity);
  }

  private static final long SLEEP_DURATION = Duration.ofHours(8).toMinutes();
  private static final long WAKEUP_DURATION = Duration.ofMinutes(10).toMinutes();
  private static final long BREAKFAST_DURATION = Duration.ofMinutes(20).toMinutes();
  private static final long LUNCH_DURATION = Duration.ofMinutes(20).toMinutes();
  private static final long EXERCISE_DURATION = Duration.ofMinutes(150).toMinutes();
  private static final long DINNER_DURATION = Duration.ofMinutes(20).toMinutes();
  private static final long REST_DURATION = Duration.ofMinutes(160).toMinutes();

  public static final long ESSENTIAL_ACTIVITY_DURATION_IN_MINUTES
      = SLEEP_DURATION
      + WAKEUP_DURATION
      + BREAKFAST_DURATION
      + LUNCH_DURATION
      + EXERCISE_DURATION
      + DINNER_DURATION
      + REST_DURATION;

  private static final long NAP_DURATION = Duration.ofMinutes(20).toMinutes();

  public static final List<ChronoTypeQuestion> CHRONO_TYPE_QUESTIONS = List.of(
      ChronoTypeQuestion
          .withUniformIncrements(
              "What time would you get up if you were entirely free to plan your day?",
              0,
              1,
              "Midday – 5:00 am",
              "11:00 – 11:59 am",
              "9:45 – 10:59 am",
              "7:45 – 9:44 am",
              "6:30 – 7:44 am",
              "5:00 – 6:29 am"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "What time would you go to bed if you were entirely free to plan your evening?",
              0,
              1,
              "3:00 am – 8:00 pm",
              "1:45 – 2:59 am",
              "12:30 – 1:44 am",
              "10:15 pm – 12:29 am",
              "9:00 – 10:14 pm",
              "8:00 – 8:59 pm "
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "If there is a specific time at which you have to get up in the morning, to what extent do you\n"
                  + "depend on being woken up by an alarm clock?",
              1,
              1,
              "Very dependent",
              "Fairly dependent",
              "Slightly dependent",
              "Not at all dependent"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "How easy do you find it to get up in the morning (when you are not woken up\n"
                  + "unexpectedly)?",
              1,
              1,
              "Not at all easy",
              "Not very easy",
              "Fairly easy",
              "Very easy "
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "How alert do you feel during the first half hour after you wake up in the morning?",
              1,
              1,
              "Not at all alert",
              "Slightly alert",
              "Fairly alert",
              "Very alert "
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "How hungry do you feel during the first half-hour after you wake up in the morning?",
              1,
              1,
              "Not at all hungry",
              "Slightly hungry",
              "Fairly hungry",
              "Very hungry"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "During the first half-hour after you wake up in the morning, how tired do you feel?",
              1,
              1,
              "Very tired",
              "Fairly tired",
              "Fairly refreshed",
              "Very refreshed"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "If you have no commitment the next day, what time would you go to bed compared to your\n"
                  + "usual bedtime?",
              1,
              1,
              "More than two hours later",
              "1-2 hours later",
              "Less than one hour later",
              "Seldom or never later"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "You have decided to engage in some physical exercise. A friend suggests that you do this\n"
                  + "for one hour twice a week and the best time for him/her is between 7:00 – 8:00 am. Bearing\n"
                  + "in mind nothing but your own internal “clock”, how do you think you would perform?",
              1,
              1,
              "Would find it very difficult",
              "Would find it difficult",
              "Would be in reasonable form",
              "Would be in good form"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "At what time of day do you feel you become tired as a result of need for sleep?",
              1,
              1,
              "2:00 – 3:00 am",
              "12:45 – 1:59 am",
              "10:15 pm – 12:44 am",
              "9:00 – 10:14 pm",
              "8:00 – 8:59 pm"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "You want to be at your peak performance for a test that you know is going to be mentally\n"
                  + "exhausting and will last for two hours. You are entirely free to plan your day. Considering\n"
                  + "only your own internal “clock”, which ONE of the four testing times would you choose?",
              1,
              1,
              "7:00 – 9:00 pm",
              "3:00 – 5:00 pm",
              "11:00 am – 1:00 pm",
              "8:00 – 10:00 am"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "If you got into bed at 11:00 pm, how tired would you be?",
              1,
              1,
              "Not at all tired",
              "A little tired",
              "Fairly tired",
              "Very tired"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "For some reason, you have gone to bed several hours later than usual, but there is no\n"
                  + "need to get up at any particular time the next morning. Which ONE of the following are you\n"
                  + "most likely to do?\n",
              1,
              1,
              "Will NOT wake up until later than usual",
              "Will wake up at usual time but will fall asleep again",
              "Will wake up at usual time and will doze thereafter",
              "Will wake up at usual time, but will NOT fall back asleep"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "One night you have to remain awake between 4:00 – 6:00 am in order to carry out a night\n"
                  + "watch. You have no commitments the next day. Which ONE of the alternatives will suite\n"
                  + "you best?\n",
              1,
              1,
              "Would NOT go to bed until watch was over",
              "Would take a nap before and sleep after",
              "Would take a good sleep before and nap after",
              "Would sleep only before watch"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "You have to do two hours of hard physical work. You are entirely free to plan your day and\n"
                  + "considering only your own internal “clock” which ONE of the following times would you\n"
                  + "choose?",
              1,
              1,
              "7:00 – 9:00 pm",
              "3:00 – 5:00 pm",
              "11:00 am – 1:00 pm",
              "8:00 – 10:00 am"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "You have decided to engage in hard physical exercise. A friend suggests that you do this\n"
                  + "for one hour twice a week and the best time for him/her is between 10:00 – 11:00 pm.\n"
                  + "Bearing in mind nothing else but your own internal “clock”, how well do you think you\n"
                  + "would perform?",
              1,
              1,
              "Would find it very difficult",
              "Would find it difficult",
              "Would be in reasonable form",
              "Would be in good form"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "Suppose that you can choose your school hours. Assume that you went to school for five\n"
                  + "hours per day and that school was interesting and enjoyable. Which five consecutive\n"
                  + "hours would you select?\n",
              1,
              1,
              "5 hours starting between 5:00 pm – 3:59 am",
              "5 hours starting between 2:00 – 4:59 pm",
              "5 hours starting between 9:00 am – 1:59 pm",
              "5 hours starting between 8:00 – 8:59 am",
              "5 hours starting between 4:00 – 7:59 am"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "At what time of the day do you think that you reach your “feeling best” peak?",
              2,
              1,
              "5:00 – 9:59 pm",
              "10:00 am – 4:59 pm",
              "8:00 – 9:59 am",
              "5:00 – 7:59 am"
          ),
      ChronoTypeQuestion
          .withUniformIncrements(
              "One hears about “morning” and “evening” types of people. Which ONE of these types do\n"
                  + "you consider yourself to be?",
              0,
              2,
              "Definitely an “evening” type",
              "Rather more an “evening” type than a “morning” type",
              "Rather more a “morning” type than an “evening” type",
              "Definitely a “morning” type"
          )
  );

  private static final String[] BREAKFAST_TIPS = new String[]{
      "In order to stay alert and improve performance we recommend eating a low-carb, lean, protein-rich, and choline-rich meal, such as:",
      "•\tEggs",
      "•\tLean Meat",
      "•\tMilk",
      "•\tLow Fat Cheese"
  };

  private static final String[] LUNCH_TIPS = new String[]{
      "In order to stay alert and improve performance we recommend eating a Low-carb, choline-rich meal such as:",
      "•\tMeat",
      "•\tPoultry",
      "•\tFish",
      "•\tDairy Products",
      "•\tEggs",
      "•\tNo Caffeine After 2pm"
  };

  private static final String[] DINNER_TIPS = new String[]{
      "In order to stay alert and improve performance we recommend eating a Tryptophan-rich, low-caffeine meal such as:",
      "•\tEggs",
      "•\tCheese",
      "•\tFish"
  };

  private static final String[] REST_TIPS = new String[]{
      "Rest recommendation:",
      "1.\tTurn off all electronic devices an hour before sleep where possible.",
      "2.\tIf you have to use electronic devices, please wear blue light blocking glasses.",
      "3.\tWear eyemask and earplugs to sleep.",
      "4.\tSurround yourself with familiar smells which remind you of your own home."
  };

  private static final Activity NAP_ACTIVITY = Activity.of("Nap");
  private static final Activity SLEEP_ACTIVITY = Activity.of("Sleep");

  public static class ChronoTypeQuestion {

    private final String question;
    private final int start;
    private final int increment;
    private final List<String> possibleResponses;

    public ChronoTypeQuestion(String question, int start, int increment,
        String... possibleResponses) {
      this.question = question;
      this.start = start;
      this.increment = increment;
      this.possibleResponses = List.of(possibleResponses);
    }

    private static ChronoTypeQuestion withUniformIncrements(String question, int start,
        int increment, String... options) {
      return new ChronoTypeQuestion(question, start, increment, options);
    }

    public String getQuestion() {
      return question;
    }

    public List<String> getPossibleResponses() {
      return possibleResponses;
    }
  }
}
