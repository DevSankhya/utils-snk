package com.sankhya.ce;

import com.sankhya.ce.concurrent.Async;
import com.sankhya.ce.concurrent.TaskHelper;
import com.sankhya.ce.http.Http;
import com.sankhya.ce.json.JsonHelper;
import com.sankhya.ce.tuples.Triple;
import okhttp3.Headers;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        List<Double> list = doRequest();
        long start = System.currentTimeMillis();
        Async async = new Async();

        for (Double id : list) {
            int finalId = id.intValue();
            async.addTask(new TaskHelper(finalId, () -> {
                Optional<String> name = getName(finalId);
                name.ifPresent(s -> System.out.println("Nome do usuário:" + s));
            }));
        }
        async.runChunked(list.size());

        // Sync
//        for (Double id : list) {
//            int finalId = id.intValue();
//            System.out.println("Nome do usuário:" + getName(finalId));
//        }
        System.out.println("Tempo de execução: " + (System.currentTimeMillis() - start) + "ms");
    }

    private static @NotNull List<Double> doRequest() throws IOException {
        Triple<String, Headers, List<String>> response = Http.client.get("https://jsonplaceholder.typicode.com/users");
        JsonHelper json = new JsonHelper(response.getLeft());
        return ((List<?>) json.get()).stream().map(it -> (Double) new JsonHelper(it).get("id")).collect(Collectors.toList());
    }

    public static @NotNull Optional<String> getName(Object id) {
        try {
            Triple<String, Headers, List<String>> response = Http.client.get("https://jsonplaceholder.typicode.com/users/" + id);
            JsonHelper json = new JsonHelper(response.getLeft());
            return Optional.ofNullable(json.get("name"));
        } catch (Exception e) {
            return Optional.empty();
        }
    }


}