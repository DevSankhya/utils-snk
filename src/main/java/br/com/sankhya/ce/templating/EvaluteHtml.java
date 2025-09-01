package br.com.sankhya.ce.templating;

import br.com.sankhya.ce.tuples.Pair;

import javax.script.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.InputStream;

public class EvaluteHtml {

    final String regex = "<script (@Server)(?:\\s+(?:[\\s \"=A-Za-z0-9_@.\\/#&+-]*\\s*)?>|>)((?:.|\\n)*?)<\\/script>$";
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
    private final String globalVaribleName = "$this";
    private final String engineName = engine.getFactory().getEngineName();

    public EvaluteHtml() {
        engine.put(globalVaribleName, this);

        this.evalJsVoid("/templating/pollyfill.js");
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

    private String getSource(String str) {
        try {
            Paths.get(str); // Check if is a valid path
            return loadResource(str);
        } catch (Exception ex) {
            return str;
        }
    }

    private Optional<String> evalute(String source) {
        String htmlRet = getSource(source);


        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(htmlRet);

        while (matcher.find()) {
            String type = matcher.group(1);
            String script = matcher.group(2);

            if ("@Server".equals(type)) { // Processa o script, mas não retorna nada
                this.evalJsVoid(script);
                htmlRet = htmlRet.replace(matcher.group(0), "");
            }
        }

        final String regexVar = "<%=((?:.|\\n)*?)%>$";

        final Pattern patternVar = Pattern.compile(regexVar, Pattern.MULTILINE);
        final Matcher matcherVar = patternVar.matcher(htmlRet);

        while (matcherVar.find()) {
            String script = matcherVar.group(1);
            try {
                Object res = engine.eval(script);

                if (res == null) res = "null";

                htmlRet = htmlRet.replace(matcherVar.group(0), res.toString());
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.of(htmlRet.trim());
    }

    @SuppressWarnings("unchecked")
    private <T> T evaluteJs(String js) {
        if (js == null) return null;
        try {
            return (T) engine.eval(js);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public final String eval(String html, Pair<String, Object>... args) {
        for (Pair<String, Object> pair : args) {
            String name = pair.getLeft();
            Object value = toJsValue(name, pair.getRight());
            engine.put(name, value);
        }

        engine.put(globalVaribleName, this);
        Optional<String> evalute = evalute(html);
        return evalute.map(s -> s.replace("\n", " ")).orElse("");

    }

    private Object toJsValue(String name, Object value) {
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

    /**
     * @param js   Source of js code(Can be plain or a path to a file)
     * @param args Arguments to be passed to the js code
     * @return The result of the js code
     */
    @SafeVarargs
    public final <T> T evalJs(String js, Pair<String, Object>... args) {

        js = getSource(js);

        for (Pair<String, Object> pair : args) {
            String name = pair.getLeft();
            Object value = toJsValue(name, pair.getRight());
            engine.put(name, value);
        }
        engine.put(globalVaribleName, this);

        return evaluteJs(js);
    }

    /**
     * @param js   Source of js code(Can be plain or a path to a file)
     * @param args Arguments to be passed to the js code
     */
    @SafeVarargs
    public final void evalJsVoid(String js, Pair<String, Object>... args) {

        js = getSource(js);

        for (Pair<String, Object> pair : args) {
            String name = pair.getLeft();
            Object value = toJsValue(name, pair.getRight());
            engine.put(name, value);
        }
        engine.put(globalVaribleName, this);

        evaluteJs(js);
    }

    public String getEngineName() {
        return engineName;
    }

}
