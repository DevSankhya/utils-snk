package br.com.sankhya.ce.snk;

import br.com.sankhya.ws.ServiceContext;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import okhttp3.*;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SaveRecord {
    HashMap<String, Object> campos = new HashMap<>();
    List<HashMap<String, Object>> linhas = new ArrayList<>();
    private String contentType = "application/json";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private long timeOut = 60;
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(timeOut, TimeUnit.SECONDS).writeTimeout(timeOut, TimeUnit.SECONDS).readTimeout(timeOut, TimeUnit.SECONDS).build();
    private String lastUrlCalled = "";
    private String entidade;


    public SaveRecord(String entidade) {
        this.entidade = entidade;
    }

    public SaveRecord(String entidade, HashMap<String, Object> campos) {
        this.campos = campos;
        this.entidade = entidade;
    }

    public SaveRecord(String entidade, List<HashMap<String, Object>> linhas) {
        this.linhas = linhas;
        this.entidade = entidade;
    }

    public Object save() throws Exception {
        if (entidade == null || entidade.isEmpty()) throw new Exception("Entidade não informada");
        List<Record> records = new ArrayList<>();
        HashMap<String, Object> values = new HashMap<>();
        List<String> fields = new ArrayList<>(campos.keySet());
        long fieldIndex = 0;
        for (String campo : fields) {
            values.put(String.valueOf(fieldIndex), campos.get(campo));
            fieldIndex++;
        }

        records.add(new Record(values));

        RequestBodySNK requestBody = new RequestBodySNK();
        requestBody.setDataSetId("00D");
        requestBody.setEntityName(entidade);
        requestBody.setStandAlone(false);
        requestBody.setFields(new ArrayList<>(fields));
        requestBody.setRecords(records);

        Body body = new Body("DatasetSP.save", requestBody);

        Gson gson = new Gson();
        Triple<String, Headers, List<String>> response = post("/mge/service.sbr?outputType=json&serviceName=DatasetSP.save", gson.toJson(body));

        ResponseSNK responseSNK = gson.fromJson(response.getFirst(), ResponseSNK.class);
        if (Objects.equals(responseSNK.status, "1")) {
            return responseSNK.responseBody.result.get(0);
        } else {
            throw new Exception(responseSNK.getStatusMessage());
        }
    }

    public void saveAll(long size) throws Exception {
        if (entidade == null || entidade.isEmpty()) throw new Exception("Entidade não informada");
        if (linhas.isEmpty()) throw new Exception("Nenhuma linha para salvar");
        List<String> fields = new ArrayList<>(linhas.get(0).keySet());
        List<List<HashMap<String, Object>>> chunks = generateChunks(linhas, (int) size);
        for (List<HashMap<String, Object>> chunk : chunks) {
            List<Record> records = new ArrayList<>();
            for (HashMap<String, Object> linha : chunk) {
                HashMap<String, Object> values = new HashMap<>();
                long fieldIndex = 0;
                for (String campo : fields) {
                    values.put(String.valueOf(fieldIndex), linha.get(campo));
                    fieldIndex++;
                }
                records.add(new Record(values));
            }
            RequestBodySNK requestBody = new RequestBodySNK();
            requestBody.setDataSetId("00D");
            requestBody.setEntityName(entidade);
            requestBody.setStandAlone(false);
            requestBody.setFields(new ArrayList<>(fields));
            requestBody.setRecords(records);

            Body body = new Body("DatasetSP.save", requestBody);

            Gson gson = new Gson();
            Triple<String, Headers, List<String>> response = post("/mge/service.sbr?outputType=json&serviceName=DatasetSP.save", gson.toJson(body));

            ResponseSNK responseSNK = gson.fromJson(response.getFirst(), ResponseSNK.class);
            if (!Objects.equals(responseSNK.status, "1")) {
                throw new Exception(responseSNK.getStatusMessage());
            }
        }

    }

    private List<List<HashMap<String, Object>>> generateChunks(List<HashMap<String, Object>> tasks, int chunkSize) {
        List<List<HashMap<String, Object>>> chunks = new ArrayList<>();
        int i = 0;
        while (i < tasks.size()) {
            chunks.add(tasks.subList(i, Math.min(i + chunkSize, tasks.size())));
            i += chunkSize;
        }
        return chunks;
    }

    public void setLinhas(List<HashMap<String, Object>> linhas) {
        this.linhas = linhas;
    }

    public void setCampo(String campo, Object valor) {
        campos.put(campo, valor);
    }

    public Object getCampo(String campo) {
        return campos.get(campo);
    }

    public HashMap<String, Object> getCampos() {
        return campos;
    }

    public void setCampos(HashMap<String, Object> campos) {
        this.campos = campos;
    }

    public void limparCampos() {
        campos.clear();
    }

    public void removerCampo(String campo) {
        campos.remove(campo);
    }

    public boolean contemCampo(String campo) {
        return campos.containsKey(campo);
    }

    public int quantidadeCampos() {
        return campos.size();
    }

    private Pair<String, String> getLoginInfo() {
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

    private Triple<String, Headers, List<String>> post(String url, String reqBody) throws IOException {
        // Tratamento de paramentros query
        Map<String, String> query = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("Connection", "close");
        headers.put("Content-Type", contentType);
        headers.put("Accept", "application/json");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Cache-Control", "no-cache");
        String reqUrl = getUrl(url);
        Pair<String, String> loginInfo = getLoginInfo();
        query.put("jsessionid", loginInfo.getLeft());
        query.put("mgeSession", loginInfo.getLeft());
        headers.put("cookie", "JSESSIONID=" + loginInfo.getRight());
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
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                assert response.body() != null;
                return Triple.of(response.body().string(), response.headers(), response.headers().values("Set-Cookie"));
            }
            throw new IOException("Erro ao executar requisição(" + lastUrlCalled + "):" + response);
        } catch (IOException e) {
            throw new IOException("Erro ao executar requisição(" + lastUrlCalled + "):" + e);
        }
    }

    private static String getUrl(String url) {
        String reqUrl = url;
        String baseurl = ServiceContext.getCurrent().getHttpRequest().getLocalAddr();
        String porta = String.valueOf(ServiceContext.getCurrent().getHttpRequest().getLocalPort());
        String protocol = ServiceContext.getCurrent().getHttpRequest().getProtocol().split("/")[0].toLowerCase();
        String localHost = protocol + "://" + baseurl + ":" + porta;
        if (url.charAt(0) != '/' && !url.startsWith("http")) reqUrl = localHost + "/" + url;
        if (url.charAt(0) == '/' && !url.startsWith("http")) reqUrl = localHost + url;
        return reqUrl;
    }

    public static class Pair<F, S> implements Serializable {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public static <F, S> Pair<F, S> of(F first, S second) {
            return new Pair<>(first, second);
        }

        public String toString() {
            return "Pair(" + first + ", " + second + ")";
        }

        public F getLeft() {
            return first;
        }

        public S getRight() {
            return second;
        }

        public F component1() {
            return first;
        }

        public S component2() {
            return second;
        }
    }

    public static class Triple<F, S, T> implements Serializable {
        private final F first;
        private final S second;
        private final T third;

        public Triple(F first, S second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;

        }

        public static <F, S, T> Triple<F, S, T> of(F first, S second, T third) {
            return new Triple<>(first, second, third);
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }

        public T getThird() {
            return third;
        }


        @Override
        public String toString() {
            return "(" + first + "," + second + "," + third + ")";

        }


        public F component1() {
            return first;
        }

        public S component2() {
            return second;
        }

        public T component3() {
            return third;
        }

    }


    // Body


    public static class Body {
        private String serviceName;
        private RequestBodySNK requestBody;

        public Body(String serviceName, RequestBodySNK requestBody) {
            this.serviceName = serviceName;
            this.requestBody = requestBody;
        }

        public String getServiceName() {
            return this.serviceName;
        }

        public RequestBodySNK getRequestBody() {
            return this.requestBody;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public void setRequestBody(RequestBodySNK requestBody) {
            this.requestBody = requestBody;
        }
    }

    public static class RequestBodySNK {
        @SerializedName("dataSetID")
        private String dataSetId;
        private String entityName;
        private Boolean standAlone;
        private List<String> fields;
        private List<Record> records;
        private String ignoreListenerMethods;

        public RequestBodySNK(String dataSetId, String entityName, Boolean standAlone, List<String> fields, List<Record> records, String ignoreListenerMethods) {
            this.dataSetId = dataSetId;
            this.entityName = entityName;
            this.standAlone = standAlone;
            this.fields = fields;
            this.records = records;
            this.ignoreListenerMethods = ignoreListenerMethods;
        }

        public RequestBodySNK() {

        }

        public String getDataSetId() {
            return this.dataSetId;
        }

        public String getEntityName() {
            return this.entityName;
        }

        public Boolean getStandAlone() {
            return this.standAlone;
        }

        public List<String> getFields() {
            return this.fields;
        }

        public List<Record> getRecords() {
            return this.records;
        }

        public String getIgnoreListenerMethods() {
            return this.ignoreListenerMethods;
        }

        public void setDataSetId(String dataSetId) {
            this.dataSetId = dataSetId;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public void setStandAlone(Boolean standAlone) {
            this.standAlone = standAlone;
        }

        public void setFields(List<String> fields) {
            this.fields = fields;
        }

        public void setRecords(List<Record> records) {
            this.records = records;
        }

        public void setIgnoreListenerMethods(String ignoreListenerMethods) {
            this.ignoreListenerMethods = ignoreListenerMethods;
        }
    }

    public static class Record {
        private HashMap<String, Object> values;

        public Record(HashMap<String, Object> values) {
            this.values = values;
        }

        public Record() {

        }

        public HashMap<String, Object> getValues() {
            return this.values;
        }

        public void setValues(HashMap<String, Object> values) {
            this.values = values;
        }
    }


    public static class ResponseSNK {
        private String serviceName;
        private String status;
        private String pendingPrinting;
        private String transactionId;
        private ResponseBodySNK responseBody;
        private String statusMessage;


        public ResponseSNK(String serviceName, String status, String pendingPrinting, String transactionId, ResponseBodySNK responseBody) {
            this.serviceName = serviceName;
            this.status = status;
            this.pendingPrinting = pendingPrinting;
            this.transactionId = transactionId;
            this.responseBody = responseBody;
        }

        public String getServiceName() {
            return this.serviceName;
        }

        public String getStatus() {
            return this.status;
        }

        public String getPendingPrinting() {
            return this.pendingPrinting;
        }

        public String getTransactionId() {
            return this.transactionId;
        }

        public ResponseBodySNK getResponseBody() {
            return this.responseBody;
        }

        public String getStatusMessage() {
            return this.statusMessage;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public void setPendingPrinting(String pendingPrinting) {
            this.pendingPrinting = pendingPrinting;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public void setResponseBody(ResponseBodySNK responseBody) {
            this.responseBody = responseBody;
        }

        public void setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
        }
    }

    public static class ResponseBodySNK {
        private String total;
        private List<List<String>> result;

        public ResponseBodySNK(String total, List<List<String>> result) {
            this.total = total;
            this.result = result;
        }

        public String getTotal() {
            return this.total;
        }

        public List<List<String>> getResult() {
            return this.result;
        }

        public void setTotal(String total) {
            this.total = total;
        }

        public void setResult(List<List<String>> result) {
            this.result = result;
        }
    }
}
