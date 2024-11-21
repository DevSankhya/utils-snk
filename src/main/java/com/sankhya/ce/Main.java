package com.sankhya.ce;

import com.sankhya.ce.concurrent.Async;
import com.sankhya.ce.concurrent.Task;

import java.util.List;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        Async async = new Async();
        int[] numbers = IntStream.range(1, 100).toArray();

        for (int number : numbers) {
            System.out.println(number);
        }
        for (int number : numbers) {
            async.addTask(new Task() {
                @Override
                public void action() throws Exception {
                    System.out.println("Executing task " + number);
                    sleep();
                }
                @Override
                public int getOrder() {
                    return number;
                }
            });
        }
        async.runChunked(150);
    }

    public static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
