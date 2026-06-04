package br.com.sankhya.ce.sql.enums;

/**
 * Enumeração dos tipos de comandos SQL suportados.
 */
public enum SqlCommandType {
    SELECT, INSERT, UPDATE, DELETE, MERGE, CREATE, DROP, ALTER, SHOW, DESCRIBE, EXPLAIN, UNKNOWN;

    /**
     * Detecta automaticamente o tipo de um comando SQL.
     *
     * @param sql SQL a ser analisado.
     * @return Tipo detectado.
     */
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
     *
     * @param upperSql SQL em uppercase.
     * @return Tipo real do comando.
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

    /**
     * Conta ocorrências de um caractere em uma string.
     *
     * @param s String analisada.
     * @param c Caractere procurado.
     * @return Quantidade de ocorrências.
     */
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

}

