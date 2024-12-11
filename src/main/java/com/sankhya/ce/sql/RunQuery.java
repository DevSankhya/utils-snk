package com.sankhya.ce.sql;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings({"unused"})
public class RunQuery {
    private final JdbcWrapper jdbc;
    private NativeSql sql;
    private final JapeSession.SessionHandle hnd = JapeSession.open();
    private ResultSet resultSet = null;
    private boolean status;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    private final boolean closeStream;
    public static Function<NativeSql, NativeSql> QueryCallback = n -> n;

    public RunQuery(String query, boolean closeStream, Function<NativeSql, NativeSql> callBack, boolean update) {
        this.closeStream = closeStream;
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

    public RunQuery(String query, Function<NativeSql, NativeSql> callBack, boolean update) {
        this.closeStream = true;
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
        this.closeStream = true;
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

    /**
     * Iterates through the query results.
     * NOTE: After execution, the connection will be closed, which prevents the ResultSet from being used again.
     * This behavior can be avoided by passing the [closeStream] parameter as `[false]` when instantiating the class
     *
     * @author Luis Ricardo Alves Santos
     */
    public void forEach(Consumer<ResultSet> action) throws SQLException {
        if (resultSet == null) return;
        while (resultSet.next()) {
            action.accept(resultSet);
        }
        if (this.closeStream) {
            close();
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
        if (resultSet != null) closeResultSet(resultSet);
        if (sql != null) NativeSql.releaseResources(sql);
        if (jdbc != null) JdbcWrapper.closeSession(jdbc);

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
}

