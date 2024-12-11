package com.sankhya.ce.sql;

import com.sankhya.ce.data.ConvertHelper;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ResolveSqlTypes {
    List<Object> values = new ArrayList<>();
    String query = "";
    Database type = Database.ORACLE;

    public enum Database {
        ORACLE,
        MSSQL,
        UNKNOW
    }

    public ResolveSqlTypes(Database db) {
        type = db;
    }

    public ResolveSqlTypes() {
    }

    public List<Object> getValues() {
        return values;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }


    public void addValue(Object value) {
        values.add(value);
    }

    public String replaceParameters(String sql, Object... parameters) {
        StringBuilder result = new StringBuilder();
        int paramIndex = 0;

        for (int i = 0; i < sql.length(); i++) {
            char currentChar = sql.charAt(i);

            if (currentChar == '?' && i + 1 < sql.length() && sql.charAt(i + 1) == '?') {
                result.append("?");
                i++; // Skip the next '?'
            } else if (currentChar == '?' && paramIndex < parameters.length) {
                Object parameter = parameters[paramIndex++];
                result.append(formatParameter(parameter));
            } else {
                result.append(currentChar);
            }
        }

        if (paramIndex < parameters.length) {
            throw new IllegalArgumentException("Not all parameters were used in the query.");
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
            return type == Database.ORACLE ? getOracleDate(timestamp) : getMssqlDate(timestamp);
        } else if (parameter instanceof Date) {
            return type == Database.ORACLE ? getOracleDate((Date) parameter) : getMssqlDate((Date) parameter);
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
