package br.com.sankhya.ce.concurrent;

public class TaskHelper implements Task {

    int order = 0;
    String description = null;

    Runnable task;

    public TaskHelper(int order, String description, Runnable task) {
        this.order = order;
        this.description = description;
        this.task = task;
    }

    public TaskHelper(int order, Runnable task) {
        this.order = order;
        this.task = task;
    }

    public TaskHelper(String description, Runnable task) {
        this.description = description;
        this.task = task;
    }

    public TaskHelper(Runnable task) {
        this.task = task;
    }

    @Override
    public void action() {
        this.task.run();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getDescription() {
        return "";
    }
}
