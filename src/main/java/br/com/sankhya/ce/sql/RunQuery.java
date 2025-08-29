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
    private final JdbcWrapper jdbc;
    private NativeSql sql;
    private final JapeSession.SessionHandle hnd = buildtHnd();
    private ResultSet resultSet = null;
    private boolean status;
    private boolean internalTransaction = true;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public static Function<NativeSql, NativeSql> QueryCallback = n -> n;

    public RunQuery(String query) {
        try {
            hnd.setFindersMaxRows(-1);
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();
            sql = new NativeSql(jdbc);
            sql.appendSql(query);
            resultSet = sql.executeQuery();
            status = true;
        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage());
        }
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

    public RunQuery(String query, Function<NativeSql, NativeSql> callBack, boolean update) {
        try {
            hnd.setFindersMaxRows(-1);
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();
            sql = new NativeSql(jdbc);
            sql.appendSql(query);
            if (callBack != null) {
                sql = callBack.apply(sql);
            }
            if (!update) {
                resultSet = sql.executeQuery();
                status = true;
            } else {
                status = sql.executeUpdate();
                close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage());
        }
    }

    public RunQuery(String query, boolean update) {
        try {
            hnd.setFindersMaxRows(-1);
            EntityFacade entity = EntityFacadeFactory.getDWFFacade();
            jdbc = entity.getJdbcWrapper();
            jdbc.openSession();
            sql = new NativeSql(jdbc);
            sql.appendSql(query);
            if (!update) {
                resultSet = sql.executeQuery();
                status = true;
            } else {
                status = sql.executeUpdate();
                close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during query execution: " + e.getMessage());
        }
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

