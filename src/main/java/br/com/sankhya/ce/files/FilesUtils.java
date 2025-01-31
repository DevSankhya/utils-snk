package br.com.sankhya.ce.files;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
public class FilesUtils {
    public static String getContentFromResource(Class<?> baseClass, String resourcePath) throws IllegalArgumentException {
        InputStream stream = baseClass.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Arquivo não nencontrado(" + baseClass.getName() + "):" + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
    }

    public static String loadResource(Class<?> baseClass, String resourcePath) throws IllegalArgumentException {
        return getContentFromResource(baseClass, resourcePath);
    }

    public static String loadResource(String resourcePath) throws Exception {
        return getContentFromResource(Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()), resourcePath);
    }

}
