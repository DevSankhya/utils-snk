package br.com.sankhya.ce.json;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitário para navegação, leitura e conversão dinâmica de JSON.
 *
 * <p>
 * Permite:
 * </p>
 *
 * <ul>
 *     <li>Navegação por path usando "."</li>
 *     <li>Suporte a arrays via índices</li>
 *     <li>Suporte a wildcard "[*]"</li>
 *     <li>Conversão automática para POJOs</li>
 *     <li>Conversão para listas tipadas</li>
 *     <li>Conversão automática de tipos primitivos</li>
 *     <li>Mapeamento de enums, datas e timestamps</li>
 * </ul>
 *
 * <h3>Exemplos:</h3>
 *
 * <pre>{@code
 * JsonHelper json =
 *      JsonHelper.of(response);
 *
 * Integer id =
 *      json.get("0.id", Integer.class);
 *
 * List<Integer> ids =
 *      json.getList("[*].id", Integer.class);
 *
 * List<String> titles =
 *      json.getList("[*].title", String.class);
 *
 * Usuario usuario =
 *      json.get("usuario", Usuario.class);
 *  }
 * </pre>
 *
 * <h3>Paths suportados:</h3>
 *
 * <pre>
 * cliente.nome
 * itens.0.id
 * itens.[*].id
 * usuarios.[*].empresa.nome
 * </pre>
 *
 * <h3>Limitações:</h3>
 * <ul>
 *     <li>POJOs precisam possuir construtor vazio</li>
 *     <li>Mapeamento é baseado em nomes de campos</li>
 *     <li>Não suporta Set, Queue ou Map genéricos</li>
 *     <li>Não suporta anotações customizadas</li>
 * </ul>
 */
public class JsonHelper {

    // cacheia paths tokenizados para reduzir split()
    private static final Map<String, String[]> PATH_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    private final Object root;

    /**
     * Cria uma nova instância do helper.
     *
     * <p>
     * O objeto informado será normalizado para
     * JSONObject/JSONArray internamente.
     * </p>
     *
     * @param json JSON string, JSONObject,
     *             JSONArray, Collection,
     *             Map ou POJO
     */
    public JsonHelper(Object json) {
        this.root = normalize(json, new IdentityHashMap<>());
    }

    /**
     * Método utilitário para criação fluente.
     *
     * @param json objeto JSON origem
     * @return instância do helper
     */
    public static JsonHelper of(Object json) {
        return new JsonHelper(json);
    }

    private static String[] tokenize(String path) {
        return PATH_CACHE.computeIfAbsent(path, p -> p.split("\\."));
    }

    private static Field[] getFields(Class<?> clazz) {

        return FIELD_CACHE.computeIfAbsent(clazz, c -> {

            List<Field> fields = new ArrayList<>();

            Class<?> current = c;

            while (current != null) {

                fields.addAll(Arrays.asList(current.getDeclaredFields()));

                current = current.getSuperclass();
            }

            Field[] arr = fields.toArray(new Field[0]);

            for (Field f : arr) {
                f.setAccessible(true);
            }

            return arr;
        });
    }

    private static Object normalize(Object value, IdentityHashMap<Object, Boolean> visited) {

        if (value == null || value == JSONObject.NULL) {

            return null;
        }

        if (value instanceof JSONObject || value instanceof JSONArray) {

            return value;
        }

        if (value instanceof String) {

            String s = ((String) value).trim();

            if (s.startsWith("{")) {
                return new JSONObject(s);
            }

            if (s.startsWith("[")) {
                return new JSONArray(s);
            }

            return s;
        }

        if (value instanceof Map) {
            return new JSONObject((Map<?, ?>) value);
        }

        if (value instanceof Collection) {
            return new JSONArray((Collection<?>) value);
        }

        // evita loops infinitos em referências circulares
        if (visited.containsKey(value)) {
            return null;
        }

        visited.put(value, true);

        JSONObject obj = new JSONObject();

        try {

            for (Field field : getFields(value.getClass())) {

                obj.put(field.getName(), normalize(field.get(value), visited));
            }

        } catch (Exception e) {

            throw new RuntimeException(e);
        }

        return obj;
    }

    /**
     * Recupera uma propriedade utilizando path.
     * <p>
     * Exemplos:
     *
     * <pre>
     * cliente.nome
     * itens.0.id
     * itens.[*].id
     * </pre>
     *
     * @param path caminho da propriedade
     * @param json JSON de origem
     * @return wrapper do valor encontrado
     * @throws JSONException caso o caminho não exista
     */
    public static JSONProp getProp(String path, Object json) {

        Object normalized = normalize(json, new IdentityHashMap<>());

        if (path == null || path.trim().isEmpty()) {

            return new JSONProp(normalized);
        }

        List<Object> current = new ArrayList<>();

        current.add(normalized);

        for (String token : tokenize(path)) {

            List<Object> next = new ArrayList<>();

            for (Object obj : current) {

                // wildcard: expande todos elementos do array
                if ("[*]".equals(token)) {

                    if (obj instanceof JSONArray) {

                        JSONArray arr = (JSONArray) obj;

                        for (int i = 0; i < arr.length(); i++) {

                            next.add(arr.get(i));
                        }
                    }

                    continue;
                }

                if (obj instanceof JSONObject) {

                    JSONObject o = (JSONObject) obj;

                    if (o.has(token)) {

                        next.add(o.get(token));
                    }

                    continue;
                }

                if (obj instanceof JSONArray) {

                    try {

                        int idx = Integer.parseInt(token);

                        JSONArray arr = (JSONArray) obj;

                        if (idx >= 0 && idx < arr.length()) {

                            next.add(arr.get(idx));
                        }

                    } catch (Exception ignored) {
                    }
                }
            }

            current = next;
        }

        if (current.isEmpty()) {

            throw new JSONException("Path not found: " + path);
        }

        if (current.size() == 1) {

            return new JSONProp(current.get(0));
        }

        return new JSONProp(new JSONArray(current));
    }

    /**
     * Recupera uma propriedade utilizando path.
     * <p>
     * Exemplos:
     *
     * <pre>
     * cliente.nome
     * itens.0.id
     * itens.[*].id
     * </pre>
     *
     * @param path caminho da propriedade
     * @return wrapper do valor encontrado
     * @throws JSONException caso o caminho não exista
     */
    public JSONProp getProp(String path) {

        return getProp(path, root);
    }

    /**
     * Verifica se um caminho existe.
     *
     * @param path caminho desejado
     * @return true se encontrado
     */
    public boolean has(String path) {

        try {

            getProp(path);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Recupera um valor sem conversão explícita.
     * <p>
     * O tipo retornado depende do JSON.
     *
     * @param path caminho
     * @return valor encontrado
     */
    public <T> T get(String path) {

        return getProp(path).as();
    }

    /**
     * Recupera e converte para o tipo desejado.
     * <p>
     * Exemplo:
     *
     * <pre>
     * Integer id =
     *      json.get(
     *          "0.id",
     *          Integer.class
     *      );
     * </pre>
     *
     * @param path  caminho
     * @param clazz tipo esperado
     * @return objeto convertido
     */
    public <T> T get(String path, Class<T> clazz) {

        return getProp(path).as(clazz);
    }

    /**
     * Recupera uma lista tipada.
     * <p>
     * Exemplo:
     *
     * <pre>{@code
     * List<Integer> ids =
     *     json.getList(
     *         "[*].id",
     *         Integer.class
     *     );
     * }</pre>
     *
     * @param path caminho
     * @param clazz tipo dos itens
     * @return lista convertida
     */
    public <T> List<T> getList(String path, Class<T> clazz) {
        return getProp(path).asList(clazz);
    }

    public <T> List<T> getList(String path) {
        return (List<T>) getProp(path).asList(new TypeRef<Object>() {
        });
    }

    public abstract class TypeRef<T> {

        private final Type type;

        protected TypeRef() {

            ParameterizedType pt =
                (ParameterizedType)
                    getClass().getGenericSuperclass();

            this.type =
                pt.getActualTypeArguments()[0];
        }

        public Type getType() {
            return type;
        }
    }

    /**
     * Recupera valor opcional.
     * <p>
     * Não lança exceções.
     *
     * @param path caminho
     * @return valor ou null
     */
    public <T> T opt(String path) {
        try {
            return get(path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recupera um valor ou utiliza
     * fallback caso não exista.
     *
     * @param path         caminho
     * @param defaultValue valor padrão
     * @return valor encontrado ou default
     */
    @NotNull
    public <T> T getOrDefault(String path, T defaultValue) {
        try {
            T value = get(path);

            return value == null ? defaultValue : value;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Wrapper de resultado para permitir
     * conversões fluentes.
     * <p>
     * Exemplo:
     *
     * <pre>
     * json.getProp("cliente")
     *     .as(Cliente.class);
     * </pre>
     */
    public static class JSONProp {
        private final Object value;

        JSONProp(Object value) {
            this.value = value;
        }

        public boolean isArray() {
            return value instanceof JSONArray;
        }

        /**
         * Retorna o valor bruto.
         * <p>
         * Arrays são convertidos para List.
         *
         * @return valor convertido
         */
        @SuppressWarnings("unchecked")
        public <T> T as() {

            if (value instanceof JSONArray) {

                JSONArray arr = (JSONArray) value;
                List<Object> list = new ArrayList<>(arr.length());

                for (int i = 0; i < arr.length(); i++) {

                    Object item = arr.get(i);

                    if (item instanceof Number) {

                        Number n = (Number) item;

                        if (Math.floor(n.doubleValue()) == n.doubleValue()) {

                            long l = n.longValue();

                            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                                item = (int) l;
                            } else {
                                item = l;
                            }
                        }
                    }

                    list.add(item);
                }

                return (T) list;
            }

            return (T) value;
        }

        /**
         * Converte para o tipo desejado.
         *
         * @param clazz tipo destino
         * @return valor convertido
         */
        @SuppressWarnings("unchecked")
        public <T> T as(Class<T> clazz) {

            if (value instanceof JSONArray) {
                List<T> list = asList(clazz);

                return (T) list;
            }

            return mapValue(value, clazz, null);
        }

        /**
         * Converte JSONArray em lista tipada.
         *
         * @param clazz tipo dos itens
         * @return lista convertida
         */
        public <T> List<T> asList(Class<T> clazz) {

            if (!(value instanceof JSONArray)) {
                throw new IllegalStateException("Value is not array");
            }

            JSONArray arr = (JSONArray) value;

            List<T> list = new ArrayList<>(arr.length());

            for (int i = 0; i < arr.length(); i++) {
                list.add(mapValue(arr.get(i), clazz, null));
            }

            return list;
        }

        public <T> List<T> asList(TypeRef<T> typeRef) {

            Type type = typeRef.getType();

            if (!(type instanceof Class<?>)) {

                throw new IllegalArgumentException(
                    "Only simple types are supported"
                );
            }

            Class<T> clazz = (Class<T>) type;

            return asList(clazz);
        }

        @SuppressWarnings("all")
        private static <T> T mapValue(Object raw, Class<T> type, Type genericType) {
            if (raw == null || raw == JSONObject.NULL) {

                return null;
            }

            if (type.isAssignableFrom(raw.getClass())) {
                return (T) raw;
            }

            if (type == String.class) {
                return (T) raw.toString();
            }

            if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(raw.toString());
            }

            if (Number.class.isAssignableFrom(type) || type.isPrimitive()) {

                Number n = (Number) raw;

                if (type == Integer.class || type == int.class) return (T) Integer.valueOf(n.intValue());

                if (type == Long.class || type == long.class) return (T) Long.valueOf(n.longValue());

                if (type == Double.class || type == double.class) return (T) Double.valueOf(n.doubleValue());

                if (type == Float.class || type == float.class) return (T) Float.valueOf(n.floatValue());

                if (type == Short.class || type == short.class) return (T) Short.valueOf(n.shortValue());

                if (type == Byte.class || type == byte.class) return (T) Byte.valueOf(n.byteValue());
            }

            if (type.isEnum()) {
                return (T) Enum.valueOf((Class<Enum>) type, raw.toString());
            }

            if (type == LocalDate.class) {
                return (T) LocalDate.parse(raw.toString());
            }

            if (type == LocalDateTime.class) {
                return (T) LocalDateTime.parse(raw.toString());
            }

            if (type == Timestamp.class) {
                return (T) Timestamp.valueOf(raw.toString());
            }

            // converte recursivamente JSONObject -> POJO
            if (raw instanceof JSONObject) {

                try {
                    T obj = type.getDeclaredConstructor().newInstance();

                    JSONObject json = (JSONObject) raw;

                    for (Field field : getFields(type)) {

                        if (!json.has(field.getName())) {
                            continue;
                        }

                        Object value = json.get(field.getName());

                        if (List.class.isAssignableFrom(field.getType())) {

                            JSONArray arr = (JSONArray) value;

                            ParameterizedType pt = (ParameterizedType) field.getGenericType();

                            Class<?> itemType = (Class<?>) pt.getActualTypeArguments()[0];

                            List<Object> mapped = new ArrayList<>();

                            for (int i = 0; i < arr.length(); i++) {
                                mapped.add(mapValue(arr.get(i), itemType, null));
                            }

                            field.set(obj, mapped);

                        } else {
                            field.set(obj, mapValue(value, field.getType(), field.getGenericType()));
                        }
                    }

                    return obj;

                } catch (Exception e) {

                    throw new RuntimeException(e);
                }
            }

            return (T) raw;
        }
    }
}
