package com.sankhya.ce.json;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("unused")
public class JsonHelper {


    private static final Gson gson = new Gson();
    private final Object json;

    public JsonHelper(Object json) {
        this.json = json;
    }

    public static JsonHelper of(Object json) {
        return new JsonHelper(json);
    }

    public static JSONProp getProp(String name, Object json) {
        JSONObject jsonObject;
        if (name.trim().isEmpty()) {
            return new JSONProp(json);
        }
        String[] props = name.trim().split("\\.");
        if (json instanceof String) {
            boolean isJsonArray = isJsonArray((String) json);
            if (isJsonArray) {
                jsonObject = convertArrayToObject(new JSONArray((String) json));
            } else {
                jsonObject = new JSONObject((String) json);
            }
            return getJsoNprop(jsonObject, props);
        }
        if (json instanceof JSONObject) {
            jsonObject = (JSONObject) json;
        } else if (json instanceof JSONArray) {
            jsonObject = convertArrayToObject((JSONArray) json);
        } else {
            jsonObject = new JSONObject(gson.toJson(json));
        }

        return getJsoNprop(jsonObject, props);
    }

    private static JSONObject convertArrayToObject(JSONArray json) {

        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < json.length(); i++) {
            jsonObject.put(String.valueOf(i), json.get(i));
        }
        return jsonObject;
    }

    private static JSONProp convertToJSONProp(JSONObject json) {
        JSONArray jsonArray = new JSONArray();
        for (Object key : json.keySet()) {
            jsonArray.put(json.get((String) key));
        }
        return new JSONProp(jsonArray);
    }

    private static JSONProp getJsoNprop(JSONObject jsonObject, String[] props) {
        if (props.length == 0) {
            return new JSONProp(jsonObject);
        }
        String prop = props[0];

        boolean hasProp = jsonObject.has(prop);
        if (hasProp) {
            String[] newProps = Arrays.copyOfRange(props, 1, props.length);
            Object propValue = jsonObject.get(prop);

            if (propValue instanceof JSONObject || propValue instanceof JSONArray) {
                JSONObject jsonObject1 = propValue instanceof JSONObject ? ((JSONObject) propValue) : convertArrayToObject((JSONArray) propValue);
                return getJsoNprop(jsonObject1, newProps);
            }
            return new JSONProp(propValue);
        }
        throw new JSONException("Property \"" + prop + "\" not found");
    }

    public static <T> T get(String name, Object json) {
        return getProp(name, json).as();
    }

    public static JSONProp asJSONprop(String json) {
        if (isJsonArray(json)) {
            return new JSONProp(new JSONArray(json));
        }
        return new JSONProp(new JSONObject(json));
    }

    private static boolean isJsonArray(String json) {
        try {
            new JSONArray(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return gson.toJson(this.json);
    }

    public JSONProp getProp(String name) {
        JSONObject jsonObject;
        if (name.trim().isEmpty()) {
            return new JSONProp(json);
        }
        String[] props = name.trim().split("\\.");
        if (json instanceof String) {
            boolean isJsonArray = isJsonArray((String) json);
            if (isJsonArray) {
                jsonObject = convertArrayToObject(new JSONArray((String) json));
            } else {
                jsonObject = new JSONObject((String) json);
            }
            return getJsoNprop(jsonObject, props);
        }
        if (json instanceof JSONObject) {
            jsonObject = (JSONObject) json;
        } else {
            jsonObject = new JSONObject(gson.toJson(json));
        }

        return getJsoNprop(jsonObject, props);
    }

    public static boolean has(String name, Object json) {
        try {
            getProp(name, json);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public boolean has(String name) {
        return has(name, json);
    }

    public <T> T get(String name) {
        return getProp(name, json).as();
    }

    @SuppressWarnings("unchecked")
    public <T> T get() {
        JSONProp prop = getProp("", json);
        if (prop.isArray()) {
            return (T) prop.asList();
        }
        return getProp("", json).as();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<?> clazz) {
        JSONProp prop = getProp("", json);
        if (prop.isArray()) {
            return (T) prop.asList(clazz);
        }
        return getProp("", json).as();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, Class<?> clazz) {
        JSONProp prop = getProp(name, json);
        if (prop.isArray()) {
            return (T) prop.asList(clazz);
        }
        return (T) getProp(name, json).as(clazz);
    }

    @SuppressWarnings("unused")
    public static class JSONProp {
        private final Object obj;

        public JSONProp(Object obj) {
            this.obj = obj;
        }

        public <T> List<T> asList(Class<T> clazz) {
            JSONArray jsonArray = obj instanceof JSONArray ? (JSONArray) obj : new JSONArray(obj.toString());
            ListOfSomething<T> tListOfSomething = new ListOfSomething<>(clazz);
            return gson.fromJson(jsonArray.toString(), tListOfSomething);
        }

        public List<Object> asList() {
            JSONArray jsonArray = new JSONArray(obj.toString());
            List<Object> array = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                Object item = gson.fromJson(jsonArray.get(i).toString(), Object.class);
                array.add(item);
            }
            return array;
        }

        @Override
        public String toString() {
            return obj.toString();
        }

        public Double asDouble() {
            return (Double) obj;
        }

        public String asString() {
            return obj.toString();
        }

        public Boolean asBoolean() {
            return (Boolean) obj;
        }

        public Integer asInt() {
            return (Integer) obj;
        }

        @SuppressWarnings("unchecked")
        public <T> T as() {
            if (obj instanceof JSONArray) {
                return (T) asList();
            }
            return (T) obj;
        }

        @SuppressWarnings("unchecked")
        public <T> T as(Class<T> clazz) {
            if (obj instanceof JSONArray) {
                return (T) asList(clazz);
            }
            return gson.fromJson(obj.toString(), clazz);
        }

        public List<Object> list() {
            return asList();
        }

        public boolean isArray() {
            return isJsonArray(obj.toString());
        }

        public <T> T asObject(Class<T> clazz) {
            return gson.fromJson(obj.toString(), clazz);
        }

        public JsonHelper asObject() {
            return new JsonHelper(obj);
        }

        public boolean isList() {
            return obj instanceof JSONArray;
        }

    }
}

class ListOfSomething<X> implements ParameterizedType {

    private final Class<?> wrapped;

    public ListOfSomething(Class<X> wrapped) {
        this.wrapped = wrapped;
    }

    public Type[] getActualTypeArguments() {
        return new Type[]{wrapped};
    }

    public Type getRawType() {
        return List.class;
    }

    public Type getOwnerType() {
        return null;
    }

}