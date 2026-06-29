package br.com.sankhya.ce.templating;

import br.com.sankhya.ce.tuples.Pair;
import com.google.gson.Gson;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EvaluteHtml {

    private static final Gson GSON = new Gson();

    private static final Pattern SCRIPT_PATTERN =
        Pattern.compile(
            "<script\\s+@Server(?:\\s+[^>]*)?>(.*?)</script>",
            Pattern.DOTALL);

    private static final Pattern LITERAL_PATTERN =
        Pattern.compile("<%=([\\s\\S]*?)%>");

    private static final Pattern VARIABLE_PATTERN =
        Pattern.compile("<%-\\s*(.*?)\\s*%>");

    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
    private static final String GLOBAL_VARIABLE_NAME = "$this";
    private final String engineName = engine.getFactory().getEngineName();

    public EvaluteHtml() {
        engine.put(GLOBAL_VARIABLE_NAME, this);

        this.evalJsVoid("/templating/pollyfill.js");
    }

    public void addVar(String varName, Object value) {
        engine.put(varName, value);
    }


    private String getContentFromResource(Class<?> baseClass, String resourcePath) throws Exception {
        InputStream stream = baseClass.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalArgumentException("Arquivo não nencontrado(" + baseClass.getName() + "):" + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private String loadResource(String resourcePath) throws Exception {
        return getContentFromResource(Class.forName(Thread.currentThread().getStackTrace()[2].getClassName()), resourcePath);
    }

    private String getSource(String source) {

        if (!source.startsWith("/"))
            return source;

        try {
            return loadResource(source);
        } catch (Exception e) {
            return source;
        }
    }

    private Optional<String> evalute(String source) {
        String htmlRet = getSource(source);

        htmlRet = replaceExpressions(htmlRet, SCRIPT_PATTERN);
        htmlRet = replaceExpressions(htmlRet, LITERAL_PATTERN);
        htmlRet = replaceExpressions(htmlRet, VARIABLE_PATTERN, GSON::toJson);

        return Optional.of(htmlRet.trim());
    }

    private <T> T evaluteJs(String js) {
        if (js == null) return null;
        return executeScript(js);
    }

    @SafeVarargs
    public final String eval(String html, Pair<String, Object>... args) {

        bindVariables(args);

        engine.put(GLOBAL_VARIABLE_NAME, this);
        Optional<String> evalute = evalute(html);
        return evalute.map(s -> s.replace("\n", " ")).orElse("");

    }

    /**
     * @param js   Source of js code(Can be plain or a path to a file)
     * @param args Arguments to be passed to the js code
     * @return The result of the js code
     */
    @SafeVarargs
    public final <T> T evalJs(String js, Pair<String, Object>... args) {

        js = getSource(js);

        bindVariables(args);

        engine.put(GLOBAL_VARIABLE_NAME, this);

        return evaluteJs(js);
    }

    /**
     * @param js   Source of js code(Can be plain or a path to a file)
     * @param args Arguments to be passed to the js code
     */
    @SafeVarargs
    public final void evalJsVoid(String js, Pair<String, Object>... args) {
        js = getSource(js);

        bindVariables(args);

        engine.put(GLOBAL_VARIABLE_NAME, this);

        evaluteJs(js);
    }


    private String replaceExpressions(String html,
                                      Pattern pattern) {
        return replaceExpressions(html, pattern, Object::toString);
    }

    private String replaceExpressions(String html,
                                      Pattern pattern, Function<Object, String> lambda) {

        Matcher matcher = pattern.matcher(html);

        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {

            Object value = executeScript(matcher.group(1));

            matcher.appendReplacement(
                sb,
                Matcher.quoteReplacement(
                    lambda.apply(value)
                )
            );
        }

        matcher.appendTail(sb);

        return sb.toString();
    }


    @SuppressWarnings("unchecked")
    private <T> T executeScript(String script) {

        try {
            return (T) engine.eval(script);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

    }

    private Object convert(String name, Object value) {
        try {
            if (value == null) {
                return null;
            }

            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).doubleValue();
            }

            if (value instanceof EvaluteHtml) {
                throw new IllegalArgumentException("Não é possível passar um objeto EvaluteHtml");
            }

            engine.put(name, value); // primeiro coloca no engine

            if (value instanceof Collection<?> || value.getClass().isArray()) {
                if (value.getClass().isArray()) {
                    // converte array Java para List temporariamente
                    value = java.util.Arrays.asList((Object[]) value);
                    engine.put(name, value);
                }
                return engine.eval("Java.from(" + name + ")");
            }

            if (value instanceof Map<?, ?>) {
                return engine.eval("Object.from(" + name + ")");
            }

            return value; // tipos primitivos e String já funcionam
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private void bindVariables(Pair<String, Object>[] args) {
        for (Pair<String, Object> pair : args) {
            engine.put(pair.getLeft(), convert(pair.getLeft(), pair.getRight()));
        }

        engine.put(GLOBAL_VARIABLE_NAME, this);
    }


    public String getEngineName() {
        return engineName;
    }

}
