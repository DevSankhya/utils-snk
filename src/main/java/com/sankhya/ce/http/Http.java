package com.sankhya.ce.http;

import br.com.sankhya.ws.ServiceContext;
import com.google.gson.Gson;
import com.sankhya.ce.tuples.Pair;
import com.sankhya.ce.tuples.Triple;
import okhttp3.*;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.InflaterSource;
import okio.Okio;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

@SuppressWarnings({"unused"})
public class Http {
    public static final Http client = new Http();
    private static final Gson gson = new Gson();
    private static final String regexContainsProtocol = "(^http://)|(^https://)";
    private static String localHost = "";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private long timeOut = 60;
    private final TimeUnit timeUnit = TimeUnit.SECONDS;
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(timeOut, TimeUnit.SECONDS).writeTimeout(timeOut, TimeUnit.SECONDS).readTimeout(timeOut, TimeUnit.SECONDS).build();
    private Map<String, String> headersParams = new HashMap<>();
    private Params queryParams = new Params();
    private String contentType = "application/json";
    private boolean interno = false;
    private String lastUrlCalled = "";

    public Http(Map<String, String> headersParams) {
        this.headersParams = headersParams;
    }

    public Http() {

    }

    public Http(Params queryParams) {

        this.queryParams = queryParams;
    }

    public Http(boolean interno) {
        this.interno = interno;
        isInterno(interno);
    }

    public Http(Map<String, String> headersParams, Params queryParams) {
        this.headersParams = headersParams;
        this.queryParams = queryParams;
    }

    public Http(Map<String, String> headersParams, Params queryParams, boolean interno) {
        this.headersParams = headersParams;
        this.queryParams = queryParams;
        this.interno = interno;
        isInterno(interno);
    }

    private static void isInterno(boolean interno) {
        if (interno) {
            String baseurl = ServiceContext.getCurrent().getHttpRequest().getLocalAddr();
            String porta = String.valueOf(ServiceContext.getCurrent().getHttpRequest().getLocalPort());
            String protocol = ServiceContext.getCurrent().getHttpRequest().getProtocol().split("/")[0].toLowerCase();
            localHost = protocol + "://" + baseurl + ":" + porta;
        }
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addHeader(String key, String value) {
        headersParams.put(key, value);
    }

    public void addHeaders(HashMap<String, String> headers) {
        headersParams.putAll(headers);
    }

    public Map<String, String> getHeadersParams() {
        headersParams.put("Connection", "close");
        headersParams.put("Content-Type", contentType);
        headersParams.put("Accept", "application/json");
        headersParams.put("Accept-Encoding", "gzip, deflate");
        headersParams.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
        headersParams.put("Cache-Control", "no-cache");
        return headersParams;
    }

    public Pair<String, String> getLoginInfo() {
        Cookie cookie = null;
        Cookie[] cookies = ServiceContext.getCurrent().getHttpRequest().getCookies();
        for (Cookie c : cookies) {
            if (c.getName().equals("JSESSIONID")) {
                cookie = c;
                break;
            }
        }
        String session = ServiceContext.getCurrent().getHttpSessionId();
        String cookieValue = String.valueOf(cookie != null ? cookie.getValue() : null);
        return Pair.of(session, cookieValue);
    }

    public <T> Triple<T, Headers, List<String>> post(String url, Object reqBody, Class<T> clazz) throws Exception {
        // Tratamento de paramentros query
        Map<String, String> query = new HashMap<>(queryParams);
        Map<String, String> headers = new HashMap<>(getHeadersParams());
        String reqUrl = url;

        if (interno) {
            if (url.charAt(0) != '/' && !url.startsWith("http")) reqUrl = localHost + "/" + url;
            if (url.charAt(0) == '/' && !url.startsWith("http")) reqUrl = localHost + url;
            Pair<String, String> loginInfo = getLoginInfo();
            query.put("jsessionid", loginInfo.getLeft());
            query.put("mgeSession", loginInfo.getLeft());
            headers.put("cookie", "JSESSIONID=" + loginInfo.getRight());
        }
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(reqUrl)).newBuilder();
        String jsonBody = gson.toJson(reqBody);
        for (Map.Entry<String, String> entry : query.entrySet()) {
            httpBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        HttpUrl urlWithQueryParams = httpBuilder.build();

        // Define o contentType
        MediaType mediaTypeParse = MediaType.parse(contentType);
        // Constrói o corpo da requisição
        RequestBody body = RequestBody.create(mediaTypeParse, jsonBody);
        lastUrlCalled = urlWithQueryParams.toString();
        Request.Builder requestBuild = new Request.Builder().url(urlWithQueryParams).post(body);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuild.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = requestBuild.build();
        try (Response response = unzip(okHttpClient.newCall(request).execute())) {
            assert response.body() != null;
            String responseJson = response.body().string();
            return Triple.of(gson.fromJson(responseJson, clazz), response.headers(), response.headers().values("Set-Cookie"));
        }
    }

    public Triple<String, Headers, List<String>> post(String url, String reqBody) throws IOException {
        // Tratamento de paramentros query
        Map<String, String> query = new HashMap<>(queryParams);
        Map<String, String> headers = new HashMap<>(getHeadersParams());
        String reqUrl = url;

        if (interno) {
            if (url.charAt(0) != '/' && !url.startsWith("http")) reqUrl = localHost + "/" + url;
            if (url.charAt(0) == '/' && !url.startsWith("http")) reqUrl = localHost + url;
            Pair<String, String> loginInfo = getLoginInfo();
            query.put("jsessionid", loginInfo.getLeft());
            query.put("mgeSession", loginInfo.getLeft());
            headers.put("cookie", "JSESSIONID=" + loginInfo.getRight());
        }
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(reqUrl)).newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            httpBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        HttpUrl urlWithQueryParams = httpBuilder.build();
        // Instância o client
        // Define o contentType
        MediaType mediaTypeParse = MediaType.parse(contentType);
        // Constrói o corpo da requisição
        RequestBody body = RequestBody.create(mediaTypeParse, reqBody);

        lastUrlCalled = urlWithQueryParams.toString();

        Request.Builder requestBuild = new Request.Builder().url(urlWithQueryParams).post(body);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuild.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = requestBuild.build();
        try (Response response = unzip(okHttpClient.newCall(request).execute())) {
            assert response.body() != null;
            response.close();
            response.close();
            return Triple.of(response.body().string(), response.headers(), response.headers().values("Set-Cookie"));
        } catch (IOException e) {
            throw new IOException("Erro ao executar requisição(" + lastUrlCalled + "):" + e);
        } finally {
            okHttpClient.dispatcher().executorService().shutdown();
        }
    }

    public String getLastUrlCalled() {
        return lastUrlCalled;
    }

    public <T> Triple<T, Headers, List<String>> get(String url, Class<T> clazz) throws IOException {
        // Tratamento de paramentros query
        Map<String, String> query = new HashMap<>(queryParams);
        Map<String, String> headers = new HashMap<>(getHeadersParams());
        String reqUrl = url;
        if (interno) {
            if (url.charAt(0) != '/' && !url.startsWith("http")) reqUrl = localHost + "/" + url;
            if (url.charAt(0) == '/' && !url.startsWith("http")) reqUrl = localHost + url;
            Pair<String, String> loginInfo = getLoginInfo();
            query.put("jsessionid", loginInfo.getLeft());
            query.put("mgeSession", loginInfo.getLeft());
            headers.put("cookie", "JSESSIONID=" + loginInfo.getRight());
        }
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(reqUrl)).newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            httpBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        HttpUrl urlWithQueryParams = httpBuilder.build();

        lastUrlCalled = urlWithQueryParams.toString();

        Request.Builder requestBuild = new Request.Builder().url(urlWithQueryParams).get();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuild.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = requestBuild.build();
        try (Response response = unzip(okHttpClient.newCall(request).execute())) {
            assert response.body() != null;
            String responseJson = response.body().string();
            return Triple.of(gson.fromJson(responseJson, clazz), response.headers(), response.headers().values("Set-Cookie"));
        } catch (Exception e) {
            throw new IOException("Erro ao executar requisição(" + lastUrlCalled + "):" + e);
        } finally {
            okHttpClient.dispatcher().executorService().shutdown();
        }
    }

    public Triple<String, Headers, List<String>> get(String url) throws IOException {
        // Tratamento de paramentros query
        Map<String, String> query = new HashMap<>(queryParams);
        Map<String, String> headers = new HashMap<>(getHeadersParams());
        String reqUrl = url;
        if (interno) {
            if (url.charAt(0) != '/' && !url.startsWith("http")) reqUrl = localHost + "/" + url;
            if (url.charAt(0) == '/' && !url.startsWith("http")) reqUrl = localHost + url;
            Pair<String, String> loginInfo = getLoginInfo();
            query.put("jsessionid", loginInfo.getLeft());
            query.put("mgeSession", loginInfo.getLeft());
            headers.put("cookie", "JSESSIONID=" + loginInfo.getRight());
        }
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(reqUrl)).newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            httpBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        HttpUrl urlWithQueryParams = httpBuilder.build();

        lastUrlCalled = urlWithQueryParams.toString();

        // Constrói o corpo da requisição
        Request.Builder requestBuild = new Request.Builder().url(urlWithQueryParams).get();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuild.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = requestBuild.build();
        try (Response response = unzip(okHttpClient.newCall(request).execute())) {
            assert response.body() != null;
            Headers headersResponse = response.headers();
            String body = response.body().string();
            return Triple.of(body, headersResponse, headersResponse.values("Set-Cookie"));
        } catch (IOException e) {
            throw new IOException("Erro ao executar requisição(" + lastUrlCalled + "):" + e);
        }
    }

    public Triple<String, Headers, List<String>> get(String url, boolean interno) throws IOException {
        // Tratamento de paramentros query
        Map<String, String> query = new HashMap<>(queryParams);
        Map<String, String> headers = new HashMap<>(getHeadersParams());
        String reqUrl = url;
        if (interno) {
            if (url.charAt(0) != '/' && !url.startsWith("http")) reqUrl = localHost + "/" + url;
            if (url.charAt(0) == '/' && !url.startsWith("http")) reqUrl = localHost + url;
            Pair<String, String> loginInfo = getLoginInfo();
            query.put("jsessionid", loginInfo.getLeft());
            query.put("mgeSession", loginInfo.getLeft());
            headers.put("cookie", "JSESSIONID=" + loginInfo.getRight());
        }
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(reqUrl)).newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            httpBuilder.addQueryParameter(entry.getKey(), entry.getValue());
        }
        HttpUrl urlWithQueryParams = httpBuilder.build();

        lastUrlCalled = urlWithQueryParams.toString();
        // Constrói o corpo da requisição
        Request.Builder requestBuild = new Request.Builder().url(urlWithQueryParams).get();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuild.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = requestBuild.build();
        try (Response response = unzip(okHttpClient.newCall(request).execute())) {
            assert response.body() != null;
            return Triple.of(response.body().string(), response.headers(), response.headers().values("Set-Cookie"));
        } catch (IOException e) {
            throw new IOException("Erro ao executar requisição(" + lastUrlCalled + "):" + e);
        } finally {
            okHttpClient.dispatcher().executorService().shutdown();
        }
    }

    private Response unzip(final Response response) throws IOException {
        if (response.body() == null) {
            return response;
        }

        //check if we have gzip response
        String contentEncoding = response.headers().get("Content-Encoding");
        //this is used to decompress gzipped responses
        if (contentEncoding != null && contentEncoding.equals("gzip")) {

            long contentLength = response.body().contentLength();
            GzipSource responseBody = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder().build();
            return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(Objects.requireNonNull(response.body().contentType()).toString(), contentLength, Okio.buffer(responseBody))).build();
        } else if (contentEncoding != null && contentEncoding.equals("deflate")) {
            long contentLength = response.body().contentLength();
            Inflater inflater = new Inflater(true);
            InflaterSource responseBody = new InflaterSource(response.body().source(), inflater);
            Headers strippedHeaders = response.headers().newBuilder().build();
            return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(Objects.requireNonNull(response.body().contentType()).toString(), contentLength, Okio.buffer(responseBody))).build();
        }
        return response;
    }
}
