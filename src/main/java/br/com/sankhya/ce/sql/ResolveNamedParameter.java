package br.com.sankhya.ce.sql;

import br.com.sankhya.ce.tuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResolveNamedParameter {
    ResolveSqlTypes.Database type;
    List<Pair<String, Object>> parameters = new ArrayList<>();

    public ResolveNamedParameter(ResolveSqlTypes.Database db) {
        type = db;
    }

    public void set(String key, Object value) {
        parameters.add(new Pair<>(key, value));
    }

    public final String replaceNamedParameters(String sql) {
        if (parameters.isEmpty()) return sql;

        StringBuilder result = new StringBuilder();
        for (Pair<String, Object> parameter : parameters) {
            String key = parameter.getLeft();
            Object value = parameter.getRight();
            int index = sql.indexOf(":" + key.trim());
            if (index != -1) {
                result.append(sql, 0, index);
                result.append(formatParameter(value));
                sql = sql.substring(index + key.length());
            }
        }

        return result.toString();
    }

    private String formatParameter(Object parameter) {
        if (parameter == null) {
            return "NULL";
        } else if (parameter instanceof String) {
            return "'" + escapeString((String) parameter) + "'";
        } else if (parameter instanceof Number) {
            return parameter.toString();
        } else if (parameter instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) parameter;
            return type == ResolveSqlTypes.Database.ORACLE ? getOracleDate(timestamp) : getMssqlDate(timestamp);
        } else if (parameter instanceof Date) {
            return type == ResolveSqlTypes.Database.ORACLE ? getOracleDate((Date) parameter) : getMssqlDate((Date) parameter);
        } else if (parameter instanceof Boolean) {
            return (Boolean) parameter ? "1" : "0";
        } else {
            throw new IllegalArgumentException("Unsupported parameter type: " + parameter.getClass().getName());
        }
    }

    private static @NotNull String getOracleDate(Timestamp timestamp) {
        return "CAST(TO_TIMESTAMP('" + timestamp + "', 'YYYY-MM-DD HH24:MI:SS.ff3') AS DATE)";
    }

    private static @NotNull String getOracleDate(Date date) {
        Timestamp timestamp = new Timestamp(date.getTime());
        return "CAST(TO_TIMESTAMP('" + timestamp + "', 'YYYY-MM-DD HH24:MI:SS.ff3') AS DATE)";
    }

    private static @NotNull String getMssqlDate(Date date) {
        Timestamp timestamp = new Timestamp(date.getTime());
        return "Convert(datetime,'" + timestamp + "',121)";
    }

    private static @NotNull String getMssqlDate(Timestamp timestamp) {
        return "Convert(datetime,'" + timestamp + "',121)";
    }


    private static String escapeString(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("'", "''");
    }
}
