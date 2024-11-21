package com.sankhya.ce.concurrent;

public interface Task {
    /**
     * The action to be executed.
     *
     * @throws Exception if an error occurs during the execution of the action.
     */
    void action() throws Exception;

    /**
     * The order of the task.
     *
     * @return the order of the task.
     */
    default int getOrder() {
        return 0;
    }

    /**
     * The description of the task.
     *
     * @return the description of the task.
     */
    default String getDescription() {
        return null;
    };
}
