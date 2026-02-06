package br.com.sankhya.ce.sql;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings({"unused"})
public class RunQuery implements Iterable<ResultSet>, AutoCloseable {
    private JdbcWrapper jdbc;
    private final String query;
    private NativeSql sql;
    private final Map<Object, Object> params = new LinkedHashMap<>();
    private Function<NativeSql, NativeSql> callBack = Function.identity();
    private int lastId = -1;


    private final JapeSession.SessionHandle hnd = buildHnd();
    private ResultSet resultSet;
    private boolean status;
    private boolean internalTransaction = true;

    public RunQuery(String query) {
        this.query = query;
    }

    public RunQuery(String query, Function<NativeSql, NativeSql> callBack) {
        this.query = query;
        this.callBack = callBack;
    }

    public boolean isOk() {
        return status;
    }

    public JapeSession.SessionHandle getHnd() {
        return hnd;
    }

    public void setCallBack(Function<NativeSql, NativeSql> callBack) {
        this.callBack = callBack;
    }

    private JapeSession.SessionHandle buildHnd() {
        boolean hasCurrentSession = JapeSession.hasCurrentSession();
        boolean hasTransaction = hasCurrentSession && JapeSession.getCurrentSession().hasTransaction();
        if (hasCurrentSession && hasTransaction) {
            this.internalTransaction = false;
            return JapeSession.getCurrentSession().getTopMostHandle();
        }
        return JapeSession.open();
    }

    public void setParameter(Object value) {
        params.put(++lastId, value);
    }

    public void setParameter(String name, Object value) {
        params.put(name, value);
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public void execute() throws Exception {
        JapeSession.execEnsuringTX(this::executeWithouTransaction);
    }

    public void executeWithouTransaction() {
        try {
            hnd.setFindersMaxRows(-1);
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();
            sql = new NativeSql(jdbc).appendSql(query);

            for (Map.Entry<Object, Object> objectObjectEntry : params.entrySet()) {
                Object key = objectObjectEntry.getKey();
                if (key instanceof Number) {
                    sql.setParameter(((Number) key).intValue(), objectObjectEntry.getValue());
                    continue;
                }
                if (key instanceof String) {
                    sql.setNamedParameter((String) key, objectObjectEntry.getValue());
                }
            }

            sql = callBack.apply(sql);

            SqlCommandType type = getSqlCommandType(query);
            if (canUseExecuteQuery(type)) {
                resultSet = sql.executeQuery();
                status = true;
            } else if (canUseExecuteUpdate(type)) {
                status = sql.executeUpdate();
            } else {
                throw new IllegalArgumentException("Unknown SQL command: " + query);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage(), e);
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
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        MERGE,
        CREATE,
        DROP,
        ALTER,
        SHOW,
        DESCRIBE,
        EXPLAIN,
        UNKNOWN
    }

    public static SqlCommandType getSqlCommandType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return SqlCommandType.UNKNOWN;
        }

        // Remove comentários do início (-- ou /* */)
        String cleaned = removeLeadingComments(sql).trim().toUpperCase();

        // Se vazio depois de limpar
        if (cleaned.isEmpty()) {
            return SqlCommandType.UNKNOWN;
        }

        // Pega a primeira palavra
        String firstWord = cleaned.split("\\s+")[0];

        // Tratar casos especiais que levam a SELECT
        if (firstWord.equals("WITH") || firstWord.equals("EXPLAIN")
            || firstWord.equals("SHOW") || firstWord.equals("DESCRIBE")) {
            return SqlCommandType.SELECT;
        }

        try {
            return SqlCommandType.valueOf(firstWord);
        } catch (IllegalArgumentException e) {
            return SqlCommandType.UNKNOWN;
        }
    }

    private static String removeLeadingComments(String sql) {
        String result = sql.trim();

        // Remove comentários do tipo --
        while (result.startsWith("--")) {
            int newLine = result.indexOf("\n");
            if (newLine == -1) return "";
            result = result.substring(newLine + 1).trim();
        }

        // Remove comentários do tipo /* */
        while (result.startsWith("/*")) {
            int endComment = result.indexOf("*/");
            if (endComment == -1) return "";
            result = result.substring(endComment + 2).trim();
        }

        return result;
    }

    public List<Map<String, Object>> toList() throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData rsmd = getMetaData();
        if (rsmd == null) return results;
        forEach(row -> {
            try {
                results.add(toMap(row));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return results;
    }

    public void forEach(RowConsumer<Map<String, Object>> action) throws SQLException {
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

    public <T> Optional<T> getFirst(ThrowingFunction<ResultSet, T> action) {
        try {
            if (resultSet == null) return Optional.empty();
            if (resultSet.next()) {
                return Optional.of(action.apply(resultSet));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
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
    public void close() {
        try {
            if (resultSet != null) resultSet.close();
        } catch (Exception ignored) {
        }
        try {
            if (sql != null) NativeSql.releaseResources(sql);
        } catch (Exception ignored) {
        }
        try {
            if (jdbc != null) JdbcWrapper.closeSession(jdbc);
        } catch (Exception ignored) {
        }
        if (internalTransaction) {
            try {
                JapeSession.close(hnd);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public @NotNull Iterator<ResultSet> iterator() {
        return new Iterator<ResultSet>() {

            @Override
            public boolean hasNext() {
                try {
                    return !resultSet.isLast();
                } catch (SQLException e) {
                    this.rethrow(e);
                    return false;
                }
            }

            @Override
            public ResultSet next() {
                try {
                    resultSet.next();
                    return resultSet;
                } catch (SQLException e) {
                    this.rethrow(e);
                    return null;
                }
            }

            private void rethrow(SQLException e) {
                throw new RuntimeException(e.getMessage());
            }
        };
    }

    public static Map<String, Object> toMap(ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int cols = rsmd.getColumnCount();
        Map<String, Object> result = createCaseInsensitiveHashMap(cols);

        for (int i = 1; i <= cols; ++i) {
            String propKey = rsmd.getColumnLabel(i);
            if (null == propKey || propKey.isEmpty()) {
                propKey = rsmd.getColumnName(i);
            }

            if (null == propKey || propKey.isEmpty()) {
                propKey = Integer.toString(i);
            }

            result.put(propKey, resultSet.getObject(i));
        }

        return result;
    }

    protected static Map<String, Object> createCaseInsensitiveHashMap(int cols) {
        return new CaseInsensitiveHashMap(cols);
    }

    private static final class CaseInsensitiveHashMap extends LinkedHashMap<String, Object> {
        private static final long serialVersionUID = -2848100435296897392L;
        private final Map<String, String> lowerCaseMap;

        private CaseInsensitiveHashMap(int initialCapacity) {
            super(initialCapacity);
            this.lowerCaseMap = new HashMap<>();
        }

        public boolean containsKey(Object key) {
            Object realKey = this.lowerCaseMap.get(key.toString().toLowerCase(Locale.ENGLISH));
            return super.containsKey(realKey);
        }

        public Object get(Object key) {
            Object realKey = this.lowerCaseMap.get(key.toString().toLowerCase(Locale.ENGLISH));
            return super.get(realKey);
        }

        public Object put(String key, Object value) {
            Object oldKey = this.lowerCaseMap.put(key.toLowerCase(Locale.ENGLISH), key);
            Object oldValue = super.remove(oldKey);
            super.put(key, value);
            return oldValue;
        }

        public void putAll(Map<? extends String, ?> m) {
            m.forEach(this::put);
        }

        public Object remove(Object key) {
            Object realKey = this.lowerCaseMap.remove(key.toString().toLowerCase(Locale.ENGLISH));
            return super.remove(realKey);
        }
    }
}

