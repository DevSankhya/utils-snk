package com.sankhya.ce.concurrent;

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
        ExecutorService executorService = Executors.newFixedThreadPool(10);
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
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Task task : chunk) {
                String description = task.getDescription();

                if (description != null && !description.isEmpty()) {
                    System.out.println("Description: "+description);
                }
                CompletableFuture<Void> future = CompletableFuture.runAsync(getRunnable(task), executorService);
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
}
