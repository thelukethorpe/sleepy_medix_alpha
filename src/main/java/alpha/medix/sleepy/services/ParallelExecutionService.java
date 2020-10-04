package alpha.medix.sleepy.services;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.concurrent.Task;

public class ParallelExecutionService {

  private final Supplier<Executor> executorFactory = () -> Executors
      .newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
      });

  public <T> void executeInParallel(Supplier<T> heavyWork, Consumer<Object> workHandler) {
    Task<T> task = new Task<>() {
      @Override
      protected T call() {
        return heavyWork.get();
      }
    };

    task.setOnSucceeded(
        workerStateEvent -> workHandler.accept(workerStateEvent.getSource().getValue()));

    executorFactory.get().execute(task);
  }
}
