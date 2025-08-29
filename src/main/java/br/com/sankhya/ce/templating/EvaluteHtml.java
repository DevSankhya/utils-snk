package br.com.sankhya.ce.templating;

import br.com.sankhya.ce.tuples.Pair;
//import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.InputStream;

public class EvaluteHtml {

    private final String regex = "<script #(?:\\s*(?:[\\s \"=A-Za-z0-9_@.\\/#&+-]*\\s*)?>|>)((.|\\n)*?)<\\/script>$";
    private final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
    private final String globalVaribleName = "$this";
    private final String engineName = engine.getFactory().getEngineName();

    public EvaluteHtml() {
        engine.put(globalVaribleName, new EvaluateIntern(engine));
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

    private boolean isValidPath(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }

    private String evalute(String source) {
        String htmlRet = source;
        if (isValidPath(source)) {
            try {
                htmlRet = loadResource(source);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Matcher matcher = pattern.matcher(htmlRet);
        while (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String value = matcher.group(1);
                try {
                    Object res = engine.eval(value);
                    htmlRet = htmlRet.replace(matcher.group(0), res.toString());
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return htmlRet.trim();
    }

    private String evaluteJs(String js) {
        if (js == null) return null;
        try {
            Object res = engine.eval(js);
            return res != null ? res.toString().trim() : null;
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    @SafeVarargs
    public final String eval(String html, Pair<String, Object>... args) {
        for (Pair<String, Object> pair : args) {
            String name = pair.getLeft();
            Object value = pair.getRight();

            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).doubleValue();
            }
            if (value instanceof EvaluteHtml) {
                throw new IllegalArgumentException("Não é possivel passar um objeto EvaluteHtml");
            }
            if (value instanceof Collection<?> || value != null && value.getClass().isArray()) {
                if (value.getClass().isArray()) {
                    value = java.util.Arrays.asList((Object[]) value);
                }
                engine.put(name, value);
                try {
                    value = engine.eval("eval(\"\"+" + name + ")");
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                }
            }

            engine.put(name, value);
        }

        engine.put(globalVaribleName, new EvaluateIntern(engine));
        return evalute(html).replace("\n", "");
    }

    @SafeVarargs
    public final String evalJs(String js, Pair<String, Object>... args) {
        for (Pair<String, Object> pair : args) {
            String name = pair.getLeft();
            Object value = pair.getRight();

            if (value instanceof BigDecimal) {
                value = ((BigDecimal) value).doubleValue();
            }
            if (value instanceof EvaluteHtml) {
                throw new IllegalArgumentException("Não é possivel passar um objeto EvaluteHtml");
            }
            if (value instanceof Collection<?> || value != null && value.getClass().isArray()) {
                if (value.getClass().isArray()) {
                    value = java.util.Arrays.asList((Object[]) value);
                }
                engine.put(name, value);
                try {
                    value = engine.eval("eval(\"\"+" + name + ")");
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                }
            }

            engine.put(name, value);
        }

        engine.put(globalVaribleName, new EvaluateIntern(engine));
        String value = evaluteJs(js);
        return value != null ? value.replace("\n", "") : null;
    }

    public String getEngineName() {
        return engineName;
    }

    public static class EvaluateIntern {
        private final ScriptEngine engine;

        public EvaluateIntern(ScriptEngine engine) {
            this.engine = engine;
        }

        public Object eval(String js) {
            try {
                return engine.eval(js);
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        }

//        public ScriptObjectMirror getArgs() {
//            Bindings variables = engine.getBindings(ScriptContext.ENGINE_SCOPE);
//            ScriptEngine newEngine = new ScriptEngineManager().getEngineByName("js");
//            try {
//                ScriptObjectMirror obj = (ScriptObjectMirror) newEngine.eval("({})");
//                obj.putAll(variables);
//                return obj;
//            } catch (ScriptException e) {
//                throw new RuntimeException(e);
//            }
//        }

        public Object get(String name) {
            return engine.get(name);
        }
    }
}
