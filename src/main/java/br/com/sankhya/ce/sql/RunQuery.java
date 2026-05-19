package br.com.sankhya.ce.sql;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.JdbcUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunQuery implements Iterable<ResultSet>, AutoCloseable {

    private static final Logger log = Logger.getLogger(RunQuery.class.getName());

    private final String query;
    private NativeSql sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final List<Object> params = new ArrayList<>();
    private Function<NativeSql, NativeSql> callBack = Function.identity();

    private final JdbcWrapper jdbcWrapper = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
    private boolean localSession;

    private ResultSet resultSet;
    private boolean status;

    public RunQuery(String query) {
        this.query = query;
    }

    public RunQuery(String query, Object[] params) {
        this.query = query;
        this.addParameters(params);
    }

    public RunQuery(String query, Function<NativeSql, NativeSql> callBack) {
        this.query = query;
        this.callBack = callBack;
    }

    public boolean isOk() {
        return status;
    }

    public void setCallBack(Function<NativeSql, NativeSql> callBack) {
        this.callBack = callBack;
    }

    public RunQuery setParameter(Object value) {
        params.add(value);
        return this;
    }

    public RunQuery addParameters(Object[] values) {
        Collections.addAll(params, values);
        return this;
    }

    public RunQuery addParameters(Map<String, Object> params) {
        this.namedParams.putAll(params);
        return this;
    }

    public RunQuery setParameter(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public RunQuery execute() throws Exception {
        try {
            this.localSession = jdbcWrapper.getConnection() == null;

            if (localSession) {
                jdbcWrapper.openSession();
            }

            SqlCommandType type = getSqlCommandType(query);
            if (canUseExecuteQuery(type)) {
                resultSet = executeDQL(query);
                status = true;
            } else if (canUseExecuteUpdate(type)) {
                status = executeDML(query);
            } else {
                throw new IllegalArgumentException("Unknown SQL command: " + query);
            }
            return this;
        } catch (Exception e) {
            log.log(Level.SEVERE, String.format("Query execution failed: %s", query), e);
            close();
            throw e;
        }
    }

    /**
     * Executa um comando DML (INSERT, UPDATE, DELETE, etc.).
     * <p>
     * Este método é de uso interno e pressupõe que uma sessão JDBC já esteja ativa.
     * Para uso externo, prefira {@link #execute()}.
     */
    private boolean executeDML(String query) throws Exception {
        this.sql = new NativeSql(jdbcWrapper);
        this.sql.appendSql(query);
        this.sql = callBack.apply(this.sql);
        bindParameters(this.sql);
        return this.sql.executeUpdate();
    }

    /**
     * Executa um comando DQL (SELECT, WITH, EXPLAIN, etc.).
     * <p>
     * Este método é de uso interno e pressupõe que uma sessão JDBC já esteja ativa.
     * Para uso externo, prefira {@link #execute()}.
     */
    private ResultSet executeDQL(String query) throws Exception {
        this.sql = new NativeSql(jdbcWrapper);
        this.sql.appendSql(query);
        this.sql = callBack.apply(this.sql);
        bindParameters(this.sql);
        return this.sql.executeQuery();
    }

    /**
     * Atalho estático para executar um DML sem precisar gerenciar a instância manualmente.
     */
    public static boolean execute(String query, Object... params) throws Exception {
        try (RunQuery runQuery = new RunQuery(query, params).execute()) {
            return runQuery.isOk();
        }
    }

    public boolean canUseExecuteQuery(SqlCommandType type) {
        return type == SqlCommandType.SELECT;
    }

    public boolean canUseExecuteUpdate(SqlCommandType type) {
        switch (type) {
            case INSERT:
            case UPDATE:
            case DELETE:
            case MERGE:
            case CREATE:
            case DROP:
            case ALTER:
                return true;
            default:
                return false;
        }
    }

    public enum SqlCommandType {
        SELECT, INSERT, UPDATE, DELETE, MERGE, CREATE, DROP, ALTER, SHOW, DESCRIBE, EXPLAIN, UNKNOWN
    }

    public static SqlCommandType getSqlCommandType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SqlCommandType.UNKNOWN;
        }

        String cleaned = removeLeadingComments(sql).trim().toUpperCase();

        if (cleaned.isEmpty()) {
            return SqlCommandType.UNKNOWN;
        }

        String firstWord = cleaned.split("\\s+")[0];

        // WITH pode prefixar INSERT/UPDATE/DELETE — faz look-ahead para o comando real
        if (firstWord.equals("WITH")) {
            return resolveWithCommand(cleaned);
        }

        if (firstWord.equals("EXPLAIN") || firstWord.equals("SHOW") || firstWord.equals("DESCRIBE")) {
            return SqlCommandType.SELECT;
        }

        try {
            return SqlCommandType.valueOf(firstWord);
        } catch (IllegalArgumentException e) {
            return SqlCommandType.UNKNOWN;
        }
    }

    /**
     * Faz look-ahead no corpo de um WITH para determinar o comando real
     * (SELECT, INSERT, UPDATE ou DELETE).
     */
    private static SqlCommandType resolveWithCommand(String upperSql) {
        // Procura o primeiro SELECT, INSERT, UPDATE ou DELETE fora de parênteses
        int depth = 0;
        String[] tokens = upperSql.split("\\s+");
        for (String token : tokens) {
            depth += countChar(token, '(') - countChar(token, ')');
            if (depth == 0) {
                String bare = token.replaceAll("[^A-Z]", "");
                switch (bare) {
                    case "SELECT":
                        return SqlCommandType.SELECT;
                    case "INSERT":
                        return SqlCommandType.INSERT;
                    case "UPDATE":
                        return SqlCommandType.UPDATE;
                    case "DELETE":
                        return SqlCommandType.DELETE;
                }
            }
        }
        // Se não achou nada conclusivo, assume SELECT (comportamento mais seguro)
        return SqlCommandType.SELECT;
    }

    private static int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }

    /**
     * Remove comentários de linha (--) e de bloco (/* *\/) do início do SQL,
     * alternando entre os dois tipos até não restar nenhum.
     */
    private static String removeLeadingComments(String sql) {
        String result = sql.trim();

        boolean removed = true;
        while (removed) {
            removed = false;

            while (result.startsWith("--")) {
                int newLine = result.indexOf('\n');
                if (newLine == -1) return "";
                result = result.substring(newLine + 1).trim();
                removed = true;
            }

            while (result.startsWith("/*")) {
                int endComment = result.indexOf("*/");
                if (endComment == -1) return "";
                result = result.substring(endComment + 2).trim();
                removed = true;
            }
        }

        return result;
    }

    /**
     * Converte o ResultSet atual em uma lista de mapas case-insensitive.
     * Só deve ser chamado após um SELECT bem-sucedido.
     *
     * @throws IllegalStateException se não houver ResultSet disponível.
     */
    public List<Map<String, Object>> toList() throws SQLException {
        if (resultSet == null) {
            throw new IllegalStateException("toList() só pode ser chamado após um SELECT bem-sucedido.");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        forEach(row -> {
            results.add(row);
            return RowConsumer.Action.CONTINUE;
        });
        return results;
    }

    private void bindParameters(NativeSql ns) {
        this.namedParams.forEach(ns::setNamedParameter);
        this.params.forEach(ns::addParameter);
    }

    public void forEach(RowConsumer<Map<String, Object>> action) throws SQLException {
        if (resultSet == null) {
            throw new IllegalStateException("forEach() só pode ser chamado após um SELECT bem-sucedido.");
        }
        while (resultSet.next()) {
            Map<String, Object> row = toMap(resultSet);
            RowConsumer.Action result = action.accept(row);
            if (result == RowConsumer.Action.CONTINUE) continue;
            if (result == RowConsumer.Action.BREAK) break;
        }
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    public <T> Optional<T> getFirst(ThrowingFunction<ResultSet, T> action) throws Exception {
        if (resultSet == null) return Optional.empty();
        if (resultSet.next()) {
            return Optional.ofNullable(action.apply(resultSet));
        }
        return Optional.empty();
    }

    /**
     * Busca um único valor de uma tabela com condição WHERE.
     * O tipo de retorno deve ser especificado pelo caller via cast ou inferência.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getFirst(String table, String field, String where, Object... params) throws Exception {
        String sql = String.format("SELECT %s AS COLUNA FROM %s WHERE %s", field, table, where);
        try (RunQuery runQuery = new RunQuery(sql).addParameters(params).execute()) {
            return (Optional<T>) runQuery.getFirst(rs -> rs.getObject("COLUNA"));
        }
    }

    public interface RowConsumer<T> {
        enum Action {CONTINUE, BREAK}

        Action accept(T value) throws SQLException;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return resultSet != null ? resultSet.getMetaData() : null;
    }

    @Override
    public void close() throws SQLException {
        NativeSql.releaseResources(this.sql);

        if (resultSet != null && !resultSet.isClosed()) {
            JdbcUtils.closeResultSet(this.resultSet);
        }
        if (localSession) {
            log.info(String.format("%s local session closed", jdbcWrapper));
            JdbcWrapper.closeSession(jdbcWrapper);
        }
    }

    /**
     * Itera sobre as linhas do ResultSet como mapas case-insensitive.
     * Só disponível após um SELECT bem-sucedido.
     */
    @Override
    public @NotNull Iterator<ResultSet> iterator() {
        if (resultSet == null) {
            throw new IllegalStateException("iterator() só pode ser chamado após um SELECT bem-sucedido.");
        }

        return new Iterator<ResultSet>() {

            private boolean fetched = false;
            private boolean hasNext;

            @Override
            public boolean hasNext() {
                if (!fetched) {
                    try {
                        hasNext = resultSet.next();
                        fetched = true;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
                return hasNext;
            }

            @Override
            public ResultSet next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                fetched = false;
                return resultSet;
            }
        };
    }

    public static Map<String, Object> toMap(ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int cols = rsmd.getColumnCount();
        Map<String, Object> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (int i = 1; i <= cols; i++) {
            String propKey = rsmd.getColumnLabel(i);
            if (propKey == null || propKey.isEmpty()) {
                propKey = rsmd.getColumnName(i);
            }
            if (propKey == null || propKey.isEmpty()) {
                propKey = Integer.toString(i);
            }
            result.put(propKey, resultSet.getObject(i));
        }

        return result;
    }
}
