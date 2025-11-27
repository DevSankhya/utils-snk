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
        JapeSession.execEnsuringTX(() -> {
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
        });
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
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData rsmd = getMetaData();
        if (rsmd == null) return result;

        this.forEach((RowConsumer<Map<String, Object>>) (row) -> {
            return RowConsumer.Action.CONTINUE;
        });
        forEach(row -> {
            try {
                result.add(toMap(row));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return result;
    }

    public void forEach(RowConsumer<Map<String, Object>> action) throws SQLException {
        while (resultSet.next()) {
            Map<String, Object> row = toMap(resultSet);
            RowConsumer.Action result = action.accept(row);
            if (result == RowConsumer.Action.CONTINUE) continue;
            if (result == RowConsumer.Action.BREAK) break;
        }
    }

    private Map<String, Object> toMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        for (int i = 1; i <= numColumns; i++) {
            String columnName = rsmd.getColumnName(i);
            row.put(columnName, rs.getObject(columnName));
        }
        return row;
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
            private boolean hasNextChecked = false;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                if (!hasNextChecked) {
                    try {
                        hasNext = resultSet != null && resultSet.next();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    hasNextChecked = true;
                }
                return hasNext;
            }

            @Override
            public ResultSet next() {
                if (!hasNext()) throw new NoSuchElementException("No more rows available.");
                hasNextChecked = false;
                return resultSet;
            }
        };
    }
}

