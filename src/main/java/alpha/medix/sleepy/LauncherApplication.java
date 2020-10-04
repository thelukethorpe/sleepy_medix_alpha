package alpha.medix.sleepy;

import alpha.medix.sleepy.model.AstronautScheduleBuilder;
import alpha.medix.sleepy.model.AstronautScheduleBuilder.ChronoTypeQuestion;
import alpha.medix.sleepy.model.Options;
import alpha.medix.sleepy.model.Options.Theme;
import alpha.medix.sleepy.model.Schedule;
import alpha.medix.sleepy.model.Schedule.Slot;
import alpha.medix.sleepy.model.Schedule.Type;
import alpha.medix.sleepy.model.ScheduleManager;
import alpha.medix.sleepy.model.Style;
import alpha.medix.sleepy.model.UserCredentialManager;
import alpha.medix.sleepy.model.UserCredentials;
import alpha.medix.sleepy.services.DateTimeService;
import alpha.medix.sleepy.services.MediaService;
import alpha.medix.sleepy.services.OptionsService;
import alpha.medix.sleepy.services.ParallelExecutionService;
import alpha.medix.sleepy.services.UserCredentialsService;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class LauncherApplication extends Application {

  public static final String WINDOW_TITLE = "Sleepy Medix Alpha";
  public static final int WINDOW_WIDTH = 1280;
  public static final int WINDOW_HEIGHT = 720;

  private static final char CONSOLE_TICK_SYMBOL = '✔';
  private static final char CONSOLE_TIME_SYMBOL = '❗';
  private static final char CONSOLE_CROSS_SYMBOL = '✘';
  private static final ImmutableMap<Character, String> CONSOLE_SYMBOL_TO_CSS = ImmutableMap.<Character, String>builder()
      .put(CONSOLE_TICK_SYMBOL,
          Style.css(
              Style.TEXT_COLOUR.setTo("green"),
              Style.TEXT_WEIGHT.setTo("bold")
          ))
      .put(CONSOLE_TIME_SYMBOL,
          Style.css(
              Style.TEXT_COLOUR.setTo("darkblue"),
              Style.TEXT_WEIGHT.setTo("bold")
          ))
      .put(CONSOLE_CROSS_SYMBOL,
          Style.css(
              Style.TEXT_COLOUR.setTo("red"),
              Style.TEXT_WEIGHT.setTo("bold")
          ))
      .build();

  private static final String LAUNCHER_ASSETS_PATH = "./assets/";
  private static final String LAUNCHER_CONFIG_PATH = "./config/";

  private static final ImmutableMap<Theme, String> THEME_TO_LAUNCHER_LOOP_FILENAME = ImmutableMap.<Theme, String>builder()
      .put(Theme.CLASSIC,
          "launcherLoops/Classic.mp4")
      .build();

  private final DateTimeService dateTimeService = new DateTimeService();
  private final MediaService mediaService = new MediaService(LAUNCHER_ASSETS_PATH);
  private final OptionsService optionsService = new OptionsService(LAUNCHER_CONFIG_PATH);
  private final ParallelExecutionService parallelExecutionService = new ParallelExecutionService();
  private final UserCredentialsService userCredentialsService = new UserCredentialsService(
      LAUNCHER_CONFIG_PATH);

  private final Image titleLogo = mediaService.loadImage("Logo.png");
  private final Image optionsLogo = mediaService.loadImage("OptionsSmall.png");
  private final Image icon = mediaService.loadImage("Icon.png");

  private final Pane root;
  private final Scene scene;
  private final Options options;
  private final UserCredentialManager userCredentialManager;
  private final ScheduleManager scheduleManager;
  private final MediaView animatedBackground;
  private final ListView<String> console;
  private final TextField usernameTextField;
  private Stage launcherStage;

  public LauncherApplication() {
    this.root = new StackPane();
    this.scene = new Scene(root);
    this.options = optionsService.loadDefaultOptions();
    this.userCredentialManager = new UserCredentialManager();
    this.scheduleManager = new ScheduleManager();
    this.animatedBackground = new MediaView();
    this.console = new ListView<>();
    this.usernameTextField = new TextField();
  }

  @Override
  public void start(Stage stage) {
    this.launcherStage = stage;

    setAnimatedBackground();
    setLoginInterface();
    setTitleLogo();
    setConsole();
    setOptionsButton();

    stage.setScene(scene);
    stage.setTitle(WINDOW_TITLE);
    stage.setWidth(WINDOW_WIDTH);
    stage.setHeight(WINDOW_HEIGHT);
    stage.setResizable(false);
    stage.getIcons().add(icon);

    consoleGreen("Welcome to the Sleepy Medix Scheduler!");
    updateOptionsTo(loadOptionsFromDisk());
    updateUserCredentialsManagerWith(loadUserCredentialsFromDisk());

    showLauncher();
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    try {
      optionsService.writeOptionsFileToDisk(options);
      userCredentialsService.writeUserCredentialsToDisk(userCredentialManager.toList());
    } catch (IOException ignored) {
    }
  }

  private void showLauncher() {
    animatedBackground.getMediaPlayer().play();
    launcherStage.show();
  }

  private void hideLauncher() {
    launcherStage.hide();
    animatedBackground.getMediaPlayer().pause();
  }

  public static void main(String[] args) {
    launch();
  }

  private void setAnimatedBackground() {
    MediaPlayer defaultLoop = mediaService
        .loadVideoLoop(THEME_TO_LAUNCHER_LOOP_FILENAME.get(Theme.CLASSIC));
    animatedBackground.setMediaPlayer(defaultLoop);
    root.getChildren().add(animatedBackground);
  }

  private void setLoginInterface() {
    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);

    Consumer<Control> leftDimensionSetter = control -> {
      control.setMinWidth(110);
    };

    Consumer<Control> rightDimensionSetter = control -> {
      control.setMinWidth(200);
    };

    Function<String, Label> labelFactory = text -> {
      Label label = new Label(text);
      label.styleProperty().setValue(Style.css(
          Style.TEXT_WEIGHT.setTo("bold"),
          Style.TEXT_COLOUR.setTo("#91007E")
      ));
      leftDimensionSetter.accept(label);
      return label;
    };

    usernameTextField.textProperty()
        .addListener((observableValue, oldText, newText) -> options.setUsername(newText));
    rightDimensionSetter.accept(usernameTextField);
    pane.add(labelFactory.apply("Username:"), 0, 1);
    pane.add(usernameTextField, 1, 1);

    PasswordField passwordField = new PasswordField();
    rightDimensionSetter.accept(passwordField);
    pane.add(labelFactory.apply("Password:"), 0, 2);
    pane.add(passwordField, 1, 2);

    Button registerNewUserButton = new Button("Register New User");
    leftDimensionSetter.accept(registerNewUserButton);
    pane.add(registerNewUserButton, 0, 4);
    registerNewUserButton.setOnAction(actionEvent -> {
      Optional<String> confirmedPasswordResult = confirmPasswordUsingDialog(
          usernameTextField.getText());
      confirmedPasswordResult.ifPresent(confirmedPassword -> {
        String username = usernameTextField.getText();
        String password = passwordField.getText();
        if (!confirmedPassword.equals(password)) {
          consoleError("Failed to register new user. Reason: Passwords did not match.");
          return;
        }
        if (!userCredentialManager.registerNewUser(username, password)) {
          consoleError(
              "Failed to register new user. Reason: User with username \"%s\" already exists.",
              username);
          return;
        }
        consoleGreen("Successfully registered new user with username \"%s\"!", username);
      });
    });

    Button loginButton = new Button("Login");
    rightDimensionSetter.accept(loginButton);
    pane.add(loginButton, 1, 4);
    loginButton.setOnAction(actionEvent -> {
      String username = usernameTextField.getText();
      String password = passwordField.getText();
      if (!userCredentialManager.verifyExistingUser(username, password)) {
        consoleError("Failed to login. Reason: invalid credentials.");
        return;
      }
      consoleGreen("Successfully logged in as \"%s\"!", username);
      hideLauncher();
      schedulingStageFor(username).showAndWait();
      showLauncher();
    });

    pane.setAlignment(Pos.CENTER);
    root.getChildren().add(pane);

  }

  private void setTitleLogo() {
    ImageView imageView = new ImageView(titleLogo);
    StackPane.setAlignment(imageView, Pos.TOP_CENTER);
    StackPane.setMargin(imageView, new Insets(100));
    root.getChildren().add(imageView);
  }

  private void setConsole() {
    console.setCellFactory(console -> new ListCell<>() {
      @Override
      protected void updateItem(String message, boolean empty) {
        super.updateItem(message, empty);
        setText(empty ? null : message);

        if (!empty) {
          String css = CONSOLE_SYMBOL_TO_CSS.get(message.charAt(0));
          styleProperty().setValue(css);
        }
      }
    });

    console.setMaxWidth(WINDOW_WIDTH >> 1);
    console.setMaxHeight(WINDOW_HEIGHT >> 2);
    StackPane.setAlignment(console, Pos.BOTTOM_CENTER);
    StackPane.setMargin(console, new Insets(50));
    root.getChildren().add(console);
  }

  private void setOptionsButton() {
    Button optionsButton = new Button();
    optionsButton.setGraphic(new ImageView(optionsLogo));
    StackPane.setAlignment(optionsButton, Pos.TOP_RIGHT);
    StackPane.setMargin(optionsButton, new Insets(10));

    optionsButton.setOnAction(actionEvent -> {
      Optional<Options> newOptions = loadOptionsFromDialog();
      newOptions.ifPresent(this::updateOptionsTo);
    });

    root.getChildren().add(optionsButton);
  }

  private Options loadOptionsFromDisk() {
    Options options = null;
    consoleLog("Loading options from disk.");
    try {
      options = optionsService.loadOptionsFromDisk();
    } catch (IOException e) {
      consoleError("Failed to load options from disk. Reason: %s.", e.getMessage());
    }
    if (options != null) {
      consoleGreen("Successfully loaded options from disk!");
    } else {
      consoleLog("Writing new options file to disk.");
      try {
        options = optionsService.writeNewOptionsFileToDisk();
        consoleGreen("Successfully setup new options file!");
      } catch (IOException e) {
        consoleError("Failed to write new options file to disk. Reason: %s.", e.getMessage());
        consoleLog(
            "Loading a default set of options. Any changes you make this session may not be saved.");
        options = optionsService.loadDefaultOptions();
        consoleGreen("Successfully loaded default options!");
      }
    }

    return options;
  }

  private Optional<Options> loadOptionsFromDialog() {
    Dialog<Options> optionsDialog = new Dialog<>();

    ButtonType applyButtonType = new ButtonType("Apply", ButtonData.OK_DONE);

    Stage stage = (Stage) optionsDialog.getDialogPane().getScene().getWindow();
    optionsDialog.setTitle("Options");
    stage.getIcons().add(optionsLogo);

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);

    Slider volumeSlider = new Slider(Options.VOLUME_MIN, Options.VOLUME_MAX, options.getVolume());
    pane.add(new Label("Volume:"), 0, 0);
    pane.add(volumeSlider, 1, 0);

    ComboBox<Theme> themeComboBox = new ComboBox<>(
        FXCollections.observableArrayList(Theme.values()));
    themeComboBox.setValue(options.getTheme());
    themeComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Theme theme) {
        return theme.displayText();
      }

      @Override
      public Theme fromString(String string) {
        return Theme.valueOf(string);
      }
    });
    pane.add(new Label("Theme:"), 0, 1);
    pane.add(themeComboBox, 1, 1);

    optionsDialog.getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

    optionsDialog.setResultConverter(buttonType -> {
      if (buttonType == applyButtonType) {
        return Options.where()
            .volumeIsSetTo(volumeSlider.getValue())
            .themeIsSetTo(themeComboBox.getValue())
            .usernameIsSetTo(usernameTextField.getText())
            .build();
      }
      return null;
    });

    optionsDialog.getDialogPane().setContent(pane);

    return optionsDialog.showAndWait();
  }

  private void updateOptionsTo(Options newOptions) {
    animatedBackground.getMediaPlayer().setVolume(newOptions.getVolume());
    usernameTextField.setText(newOptions.getUsername());
    boolean themeNeedsToChange = !options.getTheme().equals(newOptions.getTheme());
    options.updateTo(newOptions);
    if (themeNeedsToChange) {
      changeThemeTo(newOptions.getTheme());
    }
  }

  private void changeThemeTo(Theme theme) {
    MediaPlayer oldLoop = animatedBackground.getMediaPlayer();
    MediaPlayer newLoop = mediaService.loadVideoLoop(THEME_TO_LAUNCHER_LOOP_FILENAME.get(theme));
    animatedBackground.setMediaPlayer(newLoop);
    newLoop.setVolume(options.getVolume());
    oldLoop.pause();
    newLoop.play();
  }

  private Optional<String> confirmPasswordUsingDialog(String username) {
    Dialog<String> confirmedPasswordDialog = new Dialog<>();

    ButtonType confirmButtonType = new ButtonType("Confirm", ButtonData.OK_DONE);

    Stage stage = (Stage) confirmedPasswordDialog.getDialogPane().getScene().getWindow();
    confirmedPasswordDialog.setTitle("Confirm Password");
    stage.getIcons().add(optionsLogo);

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);

    PasswordField confirmedPasswordField = new PasswordField();
    pane.add(
        new Label(String.format("Please confirm the password for the new user, \"%s\":", username)),
        0, 0);
    pane.add(confirmedPasswordField, 0, 1);

    confirmedPasswordDialog.getDialogPane().getButtonTypes()
        .addAll(confirmButtonType, ButtonType.CANCEL);

    confirmedPasswordDialog.setResultConverter(buttonType -> {
      if (buttonType == confirmButtonType) {
        return confirmedPasswordField.getText();
      }
      return null;
    });

    confirmedPasswordDialog.getDialogPane().setContent(pane);

    return confirmedPasswordDialog.showAndWait();
  }

  private List<UserCredentials> loadUserCredentialsFromDisk() {
    List<UserCredentials> userCredentialsList = null;
    consoleLog("Loading options from disk.");
    try {
      userCredentialsList = userCredentialsService.loadUserCredentialsFromDisk();
    } catch (IOException e) {
      consoleError("Failed to load user credentials from disk. Reason: %s.", e.getMessage());
    }
    if (userCredentialsList != null) {
      consoleGreen("Successfully loaded user credentials from disk!");
    } else {
      consoleLog("Writing new user credentials file to disk.");
      try {
        userCredentialsList = userCredentialsService.writeNewUserCredentialManagerFileToDisk();
        consoleGreen("Successfully setup new user credentials file!");
      } catch (IOException e) {
        consoleError("Failed to write new user credentials file to disk. Reason: %s.",
            e.getMessage());
        consoleLog(
            "Loading an empty set of user credentials. Any changes you make this session may not be saved.");
        userCredentialsList = new LinkedList<>();
        consoleGreen("Successfully loaded default options!");
      }
    }

    return userCredentialsList;
  }

  private void updateUserCredentialsManagerWith(List<UserCredentials> userCredentialsList) {
    userCredentialManager.updateWith(userCredentialsList);
  }

  private Stage schedulingStageFor(String username) {
    int windowWidth = WINDOW_WIDTH >> 1;
    int windowHeight = WINDOW_HEIGHT >> 1;

    double componentWidth = windowWidth * 0.75d;
    double componentHeight = windowHeight >> 1;

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);
    pane.setAlignment(Pos.CENTER);

    Scene scene = new Scene(pane);
    Stage stage = new Stage();

    stage.setScene(scene);
    stage.setTitle(String.format("Schedules for %s", username));
    stage.setWidth(windowWidth);
    stage.setHeight(windowHeight);
    stage.setResizable(false);
    stage.getIcons().add(icon);

    ListView<Schedule> schedules = new ListView<>();
    schedules.setMinWidth(componentWidth);
    schedules.setMaxHeight(componentHeight);
    GridPane.setHalignment(schedules, HPos.CENTER);
    pane.add(schedules, 0, 0);

    Button viewSelectedScheduleButton = new Button();
    viewSelectedScheduleButton.setText("View Selected Schedule");
    viewSelectedScheduleButton.setAlignment(Pos.CENTER);
    viewSelectedScheduleButton.setMinWidth(componentWidth);
    viewSelectedScheduleButton.setOnAction(actionEvent -> {
      Schedule schedule = schedules.getSelectionModel().getSelectedItem();
      displayScheduleInDialog(schedule);
    });
    pane.add(viewSelectedScheduleButton, 0, 1);

    Button updateSelectedScheduleButton = new Button();
    updateSelectedScheduleButton.setText("Update Selected Schedule");
    updateSelectedScheduleButton.setAlignment(Pos.CENTER);
    updateSelectedScheduleButton.setMinWidth(componentWidth);
    updateSelectedScheduleButton.setOnAction(actionEvent -> {
      Schedule schedule = schedules.getSelectionModel().getSelectedItem();
      Optional<Schedule> updatedScheduleResult = Optional.empty();
      switch (schedule.getType()) {
        case ASTRONAUT:
          updatedScheduleResult = updateAstronautScheduleFromDialog(schedule);
          break;
      }
      updatedScheduleResult.ifPresent(updatedSchedule -> {
//        if (updatedSchedule.equals(schedule)) {
//          return;
//        }
        scheduleManager.add(username, updatedSchedule);
        schedules.getItems().clear();
        schedules.getItems().addAll(scheduleManager.getSchedulesFor(username));
      });
    });
    pane.add(updateSelectedScheduleButton, 0, 2);

    Button createNewScheduleButton = new Button();
    createNewScheduleButton.setText("Create New Schedule");
    createNewScheduleButton.setAlignment(Pos.CENTER);
    createNewScheduleButton.setMinWidth(componentWidth);
    createNewScheduleButton.setOnAction(actionEvent -> {
      Optional<Schedule.Type> scheduleTypeResult = loadScheduleTypeFromDialog();
      scheduleTypeResult.ifPresent(scheduleType -> {
        Optional<Schedule> scheduleResult = Optional.empty();
        switch (scheduleType) {
          case ASTRONAUT:
            scheduleResult = createAstronautScheduleFromDialog(username);
            break;
        }
        scheduleResult.ifPresent(schedule -> {
          scheduleManager.add(username, schedule);
          schedules.getItems().clear();
          schedules.getItems().addAll(scheduleManager.getSchedulesFor(username));
        });
      });
    });
    pane.add(createNewScheduleButton, 0, 3);

    return stage;
  }

  private void displayScheduleInDialog(Schedule schedule) {
    Dialog<Void> displayDialog = new Dialog<>();

    double width = WINDOW_WIDTH >> 1;

    Stage stage = (Stage) displayDialog.getDialogPane().getScene().getWindow();
    displayDialog.setTitle(schedule.toString());
    stage.getIcons().add(optionsLogo);

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);

    ListView<Slot> scheduleSlots = new ListView<>();
    scheduleSlots.getItems().addAll(schedule.getSlots());
    scheduleSlots.setMinWidth(width);
    scheduleSlots.setMaxWidth(width);
    scheduleSlots.setMinHeight(300);
    scheduleSlots.setMaxHeight(300);

    ListView<String> slotTips = new ListView<>();
    slotTips.setMinWidth(width);
    slotTips.setMaxWidth(width);
    slotTips.setMinHeight(150);
    slotTips.setMaxHeight(150);
    slotTips.setCellFactory(console -> new ListCell<>() {
      @Override
      protected void updateItem(String message, boolean empty) {
        super.updateItem(message, empty);
        setText(empty ? null : message);

        if (!empty) {
          if (message.contains("\t")) {
            styleProperty().setValue(Style.css());
          } else {
            String css = Style.css(
                Style.TEXT_WEIGHT.setTo("bold")
            );
            styleProperty().setValue(css);
          }
        }
      }
    });

    scheduleSlots.getSelectionModel().selectedItemProperty().addListener(
        (observableValue, oldSlot, newSlot) -> {
          slotTips.getItems().clear();
          slotTips.getItems().addAll(newSlot.getActivity().getTips());
        });

    pane.add(scheduleSlots, 0, 0);
    pane.add(slotTips, 0, 1);

    displayDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    displayDialog.getDialogPane().setContent(pane);

    displayDialog.showAndWait();
  }

  private Optional<Schedule> updateAstronautScheduleFromDialog(Schedule schedule) {
    Dialog<Schedule> updateScheduleDialog = new Dialog<>();

    Stage stage = (Stage) updateScheduleDialog.getDialogPane().getScene().getWindow();
    updateScheduleDialog.setTitle(String.format("Updating %s", schedule.toString()));
    stage.getIcons().add(optionsLogo);

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);
    pane.add(new Label("Did you sleep well last night?"), 0, 0);

    ButtonType yesButton = ButtonType.YES;
    ButtonType noButton = ButtonType.NO;
    updateScheduleDialog.getDialogPane().getButtonTypes().addAll(yesButton, noButton);

    updateScheduleDialog.setResultConverter(buttonType -> {
      if (buttonType == noButton) {
        return AstronautScheduleBuilder.updateWithNap(schedule);
      }
      return null;
    });

    updateScheduleDialog.getDialogPane().setContent(pane);

    return updateScheduleDialog.showAndWait();
  }

  private Optional<Schedule.Type> loadScheduleTypeFromDialog() {
    Dialog<Schedule.Type> scheduleTypeDialog = new Dialog<>();

    ButtonType okButtonType = new ButtonType("OK", ButtonData.OK_DONE);

    Stage stage = (Stage) scheduleTypeDialog.getDialogPane().getScene().getWindow();
    scheduleTypeDialog.setTitle("Please select a Schedule Type");
    stage.getIcons().add(optionsLogo);

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);

    ComboBox<Schedule.Type> scheduleTypeComboBox = new ComboBox<>(
        FXCollections.observableArrayList(Schedule.Type.values()));
    scheduleTypeComboBox.setValue(Type.ASTRONAUT);
    scheduleTypeComboBox.setConverter(new StringConverter<>() {
      @Override
      public String toString(Schedule.Type scheduleType) {
        return scheduleType.displayText();
      }

      @Override
      public Schedule.Type fromString(String string) {
        return Schedule.Type.valueOf(string);
      }
    });
    pane.add(new Label("Schedule Type:"), 0, 0);
    pane.add(scheduleTypeComboBox, 1, 0);

    scheduleTypeDialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

    scheduleTypeDialog.setResultConverter(buttonType -> {
      if (buttonType == okButtonType) {
        return scheduleTypeComboBox.getValue();
      }
      return null;
    });

    scheduleTypeDialog.getDialogPane().setContent(pane);

    return scheduleTypeDialog.showAndWait();
  }

  private Optional<Schedule> createAstronautScheduleFromDialog(String username) {
    Dialog<Schedule> scheduleDialog = new Dialog<>();

    ButtonType submitButtonType = new ButtonType("Submit", ButtonData.OK_DONE);

    Stage stage = (Stage) scheduleDialog.getDialogPane().getScene().getWindow();
    scheduleDialog.setTitle(String.format("Schedule Survey for %s", username));
    scheduleDialog.setHeight(WINDOW_HEIGHT);
    stage.getIcons().add(optionsLogo);

    scheduleDialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);
    Node submitButton = scheduleDialog.getDialogPane().lookupButton(submitButtonType);
    submitButton.setDisable(true);

    GridPane pane = new GridPane();
    pane.setHgap(10);
    pane.setVgap(10);

    int y = 0;

    Label workingHoursQuestionLabel = new Label();
    workingHoursQuestionLabel.setText("How many hours do you need to work in the day?");

    double maximumWorkingHours = Duration.hours(24).toHours() - Duration
        .minutes(AstronautScheduleBuilder.ESSENTIAL_ACTIVITY_DURATION_IN_MINUTES).toHours();
    Slider workingHoursSlider = new Slider();
    workingHoursSlider.setMin(0);
    workingHoursSlider.setMax(maximumWorkingHours);
    workingHoursSlider.setValue(maximumWorkingHours / 2);

    Label workingHoursLabel = new Label();
    Consumer<Number> updateWorkingHoursLabel = number -> {
      workingHoursLabel.setText(String.format("%f Hours", number.doubleValue()));
    };
    updateWorkingHoursLabel.accept(workingHoursSlider.getValue());
    workingHoursSlider.valueProperty().addListener((observableValue, oldValue, newValue) -> {
      updateWorkingHoursLabel.accept(newValue);
    });

    pane.add(workingHoursQuestionLabel, 0, y);
    pane.add(workingHoursSlider, 1, y++);
    pane.add(workingHoursLabel, 1, y++);

    AstronautScheduleBuilder astronautScheduleBuilder = new AstronautScheduleBuilder();
    List<ChronoTypeQuestion> chronoTypeQuestions = AstronautScheduleBuilder.CHRONO_TYPE_QUESTIONS;
    String questionPlaceholder = "-";

    for (int i = 0; i < chronoTypeQuestions.size(); i++, y++) {
      final int index = i;
      ChronoTypeQuestion chronoTypeQuestion = chronoTypeQuestions.get(index);

      Label questionLabel = new Label();
      questionLabel.setMinHeight(Region.USE_PREF_SIZE);
      questionLabel.setText(chronoTypeQuestion.getQuestion());

      ComboBox<String> responseComboBox = new ComboBox<>(FXCollections
          .observableArrayList(
              getPossibleChronoTypeQuestionResponsesFrom(questionPlaceholder, chronoTypeQuestion)));
      responseComboBox.setValue(questionPlaceholder);
      responseComboBox.valueProperty().addListener(
          (observableValue, oldValue, newValue) -> {
            astronautScheduleBuilder.setQuestionResponse(index, responseComboBox.getValue());
            submitButton.setDisable(!astronautScheduleBuilder.isReady());
          });

      pane.add(questionLabel, 0, y);
      pane.add(responseComboBox, 1, y);
    }

    scheduleDialog.setResultConverter(buttonType -> {
      if (buttonType == submitButtonType) {
        long workDurationInMinutes = Math
            .round(Duration.hours(workingHoursSlider.getValue()).toMinutes());
        return astronautScheduleBuilder.buildWith(username, workDurationInMinutes);
      }
      return null;
    });

    ScrollPane scrollPane = new ScrollPane(pane);
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(false);
    scrollPane.setMinWidth(WINDOW_WIDTH * 0.75d);
    scheduleDialog.getDialogPane().setContent(scrollPane);

    return scheduleDialog.showAndWait();
  }

  private Collection<String> getPossibleChronoTypeQuestionResponsesFrom(String placeholder,
      ChronoTypeQuestion chronoTypeQuestion) {
    List<String> possibleChronoTypeQuestionResponses = new LinkedList<>(
        chronoTypeQuestion.getPossibleResponses());
    possibleChronoTypeQuestionResponses.add(0, placeholder);
    return possibleChronoTypeQuestionResponses;
  }

  private void consoleWrite(char symbol, String message, Object... arguments) {
    Platform
        .runLater(() -> console.getItems().add(String.format(symbol + " " + message, arguments)));
  }

  private void consoleGreen(String greenMessage, Object... arguments) {
    consoleWrite(CONSOLE_TICK_SYMBOL, greenMessage, arguments);
  }

  private void consoleLog(String logMessage, Object... arguments) {
    consoleWrite(CONSOLE_TIME_SYMBOL, logMessage, arguments);
  }

  private void consoleError(String errorMessage, Object... arguments) {
    consoleWrite(CONSOLE_CROSS_SYMBOL, errorMessage, arguments);
  }
}