package br.com.sankhya.ce.sql;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.Date;

public class ResolveSqlTypes {
    Database type;

    public enum Database {
        ORACLE,
        MSSQL,
        UNKNOW
    }

    public ResolveSqlTypes(Database db) {
        type = db;
    }


    public String replaceParameters(String sql, Object... parameters) {
        StringBuilder result = new StringBuilder();
        int paramIndex = 0;
        if (sql == null)
            return null;

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
            return (Boolean) parameter ? "'S'" : "'N'";
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
