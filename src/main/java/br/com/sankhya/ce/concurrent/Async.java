package br.com.sankhya.ce.concurrent;

import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.modelcore.util.AsyncAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Async {
    List<Task> tasks = new ArrayList<>();
    private final Comparator<Task> comparator = Comparator.comparingInt(Task::getOrder);

    public Async(@NotNull Task[] tasks) {
        this.tasks = Arrays.stream(tasks).sorted(comparator).collect(Collectors.toList());
    }

    public Async(@NotNull List<Task> tasks) {

        this.tasks = tasks.stream().sorted(comparator).collect(Collectors.toList());
    }

    public Async() {

    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    /**
     * Run the tasks asynchronously
     */
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Task task : tasks) {
            String description = task.getDescription();
            if (description != null)
                System.out.println(description);

            CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task), executorService);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
                join();
        executorService.shutdown();
    }

    private static @NotNull Runnable getRunnable(Task task) {
        return () -> {
            try {
                if (task != null)
                    task.action();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Run the tasks synchronously
     */
    public void runSync() throws Exception {
        for (Task task : tasks) {
            String description = task.getDescription();
            if (description != null)
                System.out.println(description);
            task.action();
        }
    }

    /**
     * Run with chunks of tasks
     *
     * @param chunkSize the size of the chunk
     */
    public void runChunked(int chunkSize) {
        List<List<Task>> chunks = generateChunks(tasks, chunkSize);
        for (List<Task> chunk : chunks) {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Task task : chunk) {
                String description = task.getDescription();

                if (description != null && !description.isEmpty()) {
                    System.out.println("Description: " + description);
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task));
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
                    join();
            executorService.shutdown();
        }
    }

    public void runChunked() {
        List<List<Task>> chunks = generateChunks(tasks, 5);
        for (List<Task> chunk : chunks) {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Task task : chunk) {
                String description = task.getDescription();

                if (description != null && !description.isEmpty()) {
                    System.out.println("Description: " + description);
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task));
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).
                    join();
            executorService.shutdown();
        }
    }

    private List<List<Task>> generateChunks(List<Task> tasks, int chunkSize) {
        List<List<Task>> chunks = new ArrayList<>();
        int i = 0;
        while (i < tasks.size()) {
            chunks.add(tasks.subList(i, Math.min(i + chunkSize, tasks.size())));
            i += chunkSize;
        }
        return chunks;
    }


    public static void async(
        String name, // Nome da tasnk
        String domain, // Dominio da task(Pode ser qualquer coisa at� onde sei, mas � para ser um string sem espa�os como um ID. Syspdv usa "dvc" por exemplo)
        Runnable body // C�digo que vai ser executado no AsyncAction
    ) {
        AsyncAction.AsyncTask task = new AsyncAction.AsyncTask(name, true, true);
        task.setTaskBody(() -> {
            JapeSession.SessionHandle hnd = null;
            try {
                // Abre uma sess�o(N�o significa que vai abrir uma transa��o
                hnd = JapeSession.open();
                // Garante que vai ter transa��o ativa(Abre uma nova se n�o existir ou usa a correte se existir)
                hnd.execEnsuringTX(() -> {
                    body.run();
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (hnd != null)
                    JapeSession.close(hnd);
            }
        });
        try {
            AsyncAction.addTask(domain, task); // Adiciona a task a fila de execu��o
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
