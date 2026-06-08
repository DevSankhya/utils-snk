package br.com.sankhya.ce.sql.enums;

/**
 * Enumeração dos tipos de comandos SQL suportados.
 */
public enum SqlCommandType {

    SELECT(SqlCommandCategory.DQL),

    INSERT(SqlCommandCategory.DML), UPDATE(SqlCommandCategory.DML), DELETE(SqlCommandCategory.DML), MERGE(SqlCommandCategory.DML),

    CREATE(SqlCommandCategory.DDL), DROP(SqlCommandCategory.DDL), ALTER(SqlCommandCategory.DDL), TRUNCATE(SqlCommandCategory.DDL),

    SHOW(SqlCommandCategory.METADATA), DESCRIBE(SqlCommandCategory.METADATA), EXPLAIN(SqlCommandCategory.METADATA),

    UNKNOWN(SqlCommandCategory.UNKNOWN);

    private final SqlCommandCategory category;

    SqlCommandType(SqlCommandCategory category) {
        this.category = category;
    }

    public SqlCommandCategory getCategory() {
        return category;
    }

    /**
     * Detecta automaticamente o tipo de um comando SQL.
     *
     * @param sql SQL a ser analisado.
     * @return Tipo detectado.
     */
    public static SqlCommandType getSqlCommandType(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return UNKNOWN;
        }

        String cleaned = removeLeadingComments(sql).trim().toUpperCase();

        if (cleaned.isEmpty()) {
            return UNKNOWN;
        }

        String firstWord = cleaned.split("\\s+")[0];

        /*
         * WITH pode prefixar SELECT/INSERT/UPDATE/DELETE.
         */
        if ("WITH".equals(firstWord)) {
            return resolveWithCommand(cleaned);
        }

        try {
            return valueOf(firstWord);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    /**
     * Faz look-ahead no corpo de um WITH para determinar
     * o comando real.
     * <p>
     */
    private static SqlCommandType resolveWithCommand(String upperSql) {
        int depth = 0;

        String[] tokens = upperSql.split("\\s+");

        for (String token : tokens) {

            depth += countChar(token, '(');
            depth -= countChar(token, ')');

            if (depth == 0) {

                String bare = token.replaceAll("[^A-Z]", "");

                switch (bare) {
                    case "SELECT":
                        return SELECT;

                    case "INSERT":
                        return INSERT;

                    case "UPDATE":
                        return UPDATE;

                    case "DELETE":
                        return DELETE;
                    case "MERGE":
                        return MERGE;
                }
            }
        }

        /*
         * Comportamento conservador:
         * WITH normalmente é SELECT.
         */
        return SELECT;
    }

    /**
     * Conta ocorrências de um caractere.
     */
    private static int countChar(String s, char c) {
        int count = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }

        return count;
    }

    /**
     * Remove comentários iniciais do SQL.
     */
    private static String removeLeadingComments(String sql) {

        String result = sql.trim();

        boolean removed = true;

        while (removed) {

            removed = false;

            while (result.startsWith("--")) {

                int newLine = result.indexOf('\n');

                if (newLine == -1) {
                    return "";
                }

                result = result.substring(newLine + 1).trim();

                removed = true;
            }

            while (result.startsWith("/*")) {

                int endComment = result.indexOf("*/");

                if (endComment == -1) {
                    return "";
                }

                result = result.substring(endComment + 2).trim();

                removed = true;
            }
        }

        return result;
    }

    public boolean isQuery() {
        return category == SqlCommandCategory.DQL
            || category == SqlCommandCategory.METADATA;
    }

    public boolean isReadOnly() {
        return isQuery();
    }

    public boolean isWriteOnly() {
        return isDML()
            || isDDL();
    }

    public boolean isSelect() {
        return this == SELECT;
    }

    public boolean isDML() {
        return category == SqlCommandCategory.DML;
    }

    public boolean isDDL() {
        return category == SqlCommandCategory.DDL;
    }

    public boolean isDQL() {
        return category == SqlCommandCategory.DQL;
    }

    public boolean isMetadata() {
        return category == SqlCommandCategory.METADATA;
    }
}
