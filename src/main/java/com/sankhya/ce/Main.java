package com.sankhya.ce;

import com.sankhya.ce.http.Http;
import com.sankhya.ce.json.JsonHelper;
import okhttp3.Headers;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        Triple<String, Headers, List<String>> response = Http.client.get("https://jsonplaceholder.typicode.com/users");
        JsonHelper json = new JsonHelper(response.getLeft());
        List<Double> list = ((List<?>) json.get()).stream().map(it -> (Double) new JsonHelper(it).get("id")).collect(Collectors.toList());
        list.forEach(System.out::println);
    }
}