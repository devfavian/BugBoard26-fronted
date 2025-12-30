package application.util;

import java.util.function.Consumer;

import javafx.concurrent.Task;

public final class AsyncRunner {
    private AsyncRunner() {}

    public static <T> void run(Task<T> task,
                               Consumer<T> onSuccess,
                               Consumer<Throwable> onFailure) {
        task.setOnSucceeded(e -> {
            if (onSuccess != null) {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(e -> {
            if (onFailure != null) {
                onFailure.accept(task.getException());
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
