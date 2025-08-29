package br.com.sankhya.ce.sql;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused"})
public class RunQuery implements Iterable<ResultSet>, AutoCloseable {
    private JdbcWrapper jdbc;
    private final String query;
    private NativeSql sql;

    private Function<NativeSql, NativeSql> callBack = (n) -> n;

    private final JapeSession.SessionHandle hnd = buildtHnd();
    private ResultSet resultSet = null;
    private boolean status;
    private boolean internalTransaction = true;


    public RunQuery(String query) {
        try {
            this.query = query;
        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage());
        }
    }

    public RunQuery(String query, Function<NativeSql, NativeSql> callBack) {
        try {
            this.query = query;
            this.callBack = callBack;
        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage());
        }
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


    public JapeSession.SessionHandle buildtHnd() {
        boolean hasCurrentSession = JapeSession.hasCurrentSession();
        boolean hasTransaction = false;
        if (hasCurrentSession) hasTransaction = JapeSession.getCurrentSession().hasTransaction();
        if (hasCurrentSession && hasTransaction) {
            this.internalTransaction = false;
            return JapeSession.getCurrentSession().getTopMostHandle();
        } else return JapeSession.open();
    }


    private NativeSql runCallBack(NativeSql sql) {
        if (callBack != null) {
            return callBack.apply(sql);
        }
        return sql;
    }


    public void execute() {
        try {
            hnd.setFindersMaxRows(-1);
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();
            sql = new NativeSql(jdbc);
            sql.appendSql(query);

            sql = runCallBack(sql);

            SqlCommandType type = getSqlCommandType(query);
            if (canUseExecuteQuery(type)) {
                resultSet = sql.executeQuery();
                status = true;
            } else if (canUseExecuteUpdate(type)) {
                status = sql.executeUpdate();
            } else {
                throw new Exception("Unknow command");
            }

        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage());
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

    public List<JSONObject> toList() throws SQLException {
        List<JSONObject> json = new ArrayList<>();
        ResultSetMetaData rsmd = getMetaData();
        if (rsmd == null) return json;
        forEach(row -> {
            int numColumns;
            try {
                numColumns = rsmd.getColumnCount();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            JSONObject obj = new JSONObject();
            for (int i = 1; i <= numColumns; i++) {
                String columnName;
                try {
                    columnName = rsmd.getColumnName(i);
                    obj.put(columnName, row.getObject(columnName));
                    json.add(obj);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return json;
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        return resultSet != null ? resultSet.getMetaData() : null;
    }

    /**
     * Closes the database connection and clears the ResultSet data
     */
    public void close() {
//        if (resultSet != null) closeResultSet(resultSet);
        if (sql != null) NativeSql.releaseResources(sql);
        if (jdbc != null) JdbcWrapper.closeSession(jdbc);

        if (internalTransaction)
            JapeSession.close(hnd);
    }

    private static void closeResultSet(ResultSet rset) {
        if (rset != null) {
            try {
                rset.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public @NotNull Iterator<ResultSet> iterator() {
        return new Iterator<ResultSet>() {
            private boolean hasNext = advance();

            private boolean advance() {
                try {
                    return resultSet != null && resultSet.next();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public ResultSet next() {
                if (!hasNext) {
                    throw new IllegalStateException("No more rows available.");
                }
                ResultSet current = resultSet;
                hasNext = advance(); // move para o próximo registro
                return current;
            }
        };
    }


    @Override
    public Spliterator<ResultSet> spliterator() {
        return Iterable.super.spliterator();
    }
}

