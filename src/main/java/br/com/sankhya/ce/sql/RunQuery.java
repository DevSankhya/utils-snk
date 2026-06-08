package br.com.sankhya.ce.sql;

import br.com.sankhya.ce.sql.enums.SqlCommandType;
import br.com.sankhya.ce.sql.interfaces.ResultSetConsumer;
import br.com.sankhya.ce.sql.interfaces.ResultSetPredicate;
import br.com.sankhya.ce.sql.interfaces.ThrowingFunction;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.JdbcUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunQuery implements Iterable<ResultSet>, AutoCloseable {

    private static final Logger log = Logger.getLogger(RunQuery.class.getName());

    private final String query;
    private NativeSql sql;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final List<Object> params = new ArrayList<>();
    private Function<NativeSql, NativeSql> callBack = Function.identity();

    private final JdbcWrapper jdbcWrapper = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();
    private boolean localSession;

    private ResultSet resultSet;
    private boolean status;

    /**
     * Cria uma nova instância de execução SQL.
     *
     * @param query SQL a ser executado.
     */
    public RunQuery(String query) {
        this.query = query;
    }

    /**
     * Cria uma nova instância com parâmetros posicionais.
     *
     * @param query  SQL a ser executado.
     * @param params Parâmetros posicionais.
     */
    public RunQuery(String query, Object[] params) {
        this.query = query;
        this.addParameters(params);
    }

    /**
     * Cria uma nova instância com callback de customização do {@link NativeSql}.
     *
     * @param query    SQL a ser executado.
     * @param callBack Callback para customizar o {@link NativeSql}.
     */
    public RunQuery(String query, Function<NativeSql, NativeSql> callBack) {
        this.query = query;
        this.callBack = callBack;
    }


    /**
     * Retorna o status da última execução.
     *
     * @return true caso a execução tenha sido bem-sucedida.
     */
    public boolean isOk() {
        return status;
    }

    /**
     * Define um callback de customização do {@link NativeSql}.
     *
     * @param callBack Callback de customização.
     */
    public void setCallBack(Function<NativeSql, NativeSql> callBack) {
        this.callBack = callBack;
    }

    /**
     * Adiciona um parâmetro posicional.
     *
     * @param value Valor do parâmetro.
     * @return Instância atual.
     */
    public RunQuery setParameter(Object value) {
        params.add(value);
        return this;
    }

    /**
     * Adiciona múltiplos parâmetros posicionais.
     *
     * @param values Lista de parâmetros.
     * @return Instância atual.
     */
    public RunQuery addParameters(Object[] values) {
        Collections.addAll(params, values);
        return this;
    }

    /**
     * Adiciona múltiplos parâmetros nomeados.
     *
     * @param params Mapa de parâmetros.
     * @return Instância atual.
     */
    public RunQuery addParameters(Map<String, Object> params) {
        this.namedParams.putAll(params);
        return this;
    }

    /**
     * Define um parâmetro nomeado.
     *
     * @param name  Nome do parâmetro.
     * @param value Valor do parâmetro.
     * @return Instância atual.
     */
    public RunQuery setParameter(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    /**
     * Retorna o {@link ResultSet} atual.
     *
     * @return ResultSet da consulta executada.
     */
    public ResultSet getResultSet() {
        return resultSet;
    }

    /**
     * Executa automaticamente o SQL detectando o tipo do comando.
     * <p>
     * SELECTs utilizam executeQuery().
     * DMLs utilizam executeUpdate().
     * <p>
     * Obs: Recomendado o uso de  try-with-resources
     *
     * @return Instância atual.
     * @throws Exception Caso ocorra erro na execução.
     */
    public RunQuery execute() throws Exception {
        try {
            this.localSession = jdbcWrapper.getConnection() == null;

            if (localSession) {
                jdbcWrapper.openSession();
            }

            SqlCommandType type = SqlCommandType.getSqlCommandType(query);
            if (type.isQuery()) {
                resultSet = executeRead(query);
                status = true;
            } else if (type.isWriteOnly()) {
                status = executeWrite(query);
            } else {
                throw new IllegalArgumentException("Unknown SQL command: " + type);
            }
            return this;
        } catch (Exception e) {
            log.log(Level.SEVERE, String.format("Query execution failed: %s", query), e);
            close();
            throw e;
        }
    }

    /**
     * Executa um comando DML (INSERT, UPDATE, DELETE, etc.).
     *
     * @param query SQL DML.
     * @return true caso executeUpdate() retorne sucesso.
     * @throws Exception Caso ocorra erro.
     */
    private boolean executeWrite(String query) throws Exception {
        this.sql = new NativeSql(jdbcWrapper);
        this.sql.appendSql(query);
        this.sql = callBack.apply(this.sql);
        bindParameters(this.sql);
        return this.sql.executeUpdate();
    }

    /**
     * Executa um comando DQL (SELECT).
     *
     * @param query SQL SELECT.
     * @return ResultSet retornado pela consulta.
     * @throws Exception Caso ocorra erro.
     */
    private ResultSet executeRead(String query) throws Exception {
        this.sql = new NativeSql(jdbcWrapper);
        this.sql.appendSql(query);
        this.sql = callBack.apply(this.sql);
        bindParameters(this.sql);
        return this.sql.executeQuery();
    }

    /**
     * Executa rapidamente um comando DML.
     *
     * @param query  SQL a ser executado.
     * @param params Parâmetros posicionais.
     * @return true caso executado com sucesso.
     * @throws Exception Caso ocorra erro.
     */
    public static boolean execute(String query, Object... params) throws Exception {
        try (RunQuery runQuery = new RunQuery(query, params).execute()) {
            return runQuery.isOk();
        }
    }


    /**
     * Converte todo o ResultSet em uma lista de mapas case-insensitive.
     *
     * @return Lista contendo todas as linhas.
     * @throws SQLException Caso ocorra erro.
     */
    public List<Map<String, Object>> toList() throws SQLException {
        if (resultSet == null) {
            throw new IllegalStateException("toList() só pode ser chamado após um SELECT bem-sucedido.");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        forEach(row -> {
            try {
                results.add(toMap(row));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return results;
    }

    /**
     * Realiza o bind de parâmetros nomeados e posicionais.
     *
     * @param ns Instância NativeSql.
     */
    private void bindParameters(NativeSql ns) {
        this.namedParams.forEach(ns::setNamedParameter);
        this.params.forEach(ns::addParameter);
    }


    /**
     * Executa uma query SELECT rapidamente.
     *
     * @param sql    SQL SELECT.
     * @param params Parâmetros posicionais.
     * @return Instância pronta para iteração.
     * @throws Exception Caso ocorra erro.
     */
    @SuppressWarnings("all")
    public static RunQuery query(String sql, Object... params) throws Exception {
        return new RunQuery(sql).addParameters(params).execute();
    }

    /**
     * Itera sobre todas as linhas do ResultSet.
     *
     * @param action Callback executado para cada linha.
     * @throws Exception Caso ocorra erro.
     */
    public void iterate(ResultSetConsumer action) throws Exception {
        try (RunQuery query = this) {
            for (ResultSet rs : query) {
                action.accept(rs);
            }
        }
    }

    /**
     * Itera até que o callback retorne false.
     *
     * @param action Callback condicional.
     * @throws Exception Caso ocorra erro.
     */
    public void iterateUntil(ResultSetPredicate action) throws Exception {
        try (RunQuery query = this) {
            for (ResultSet rs : query) {
                if (!action.test(rs)) {
                    break;
                }
            }
        }
    }

    /**
     * Executa uma query e consome os resultados automaticamente.
     *
     * @param sql    SQL SELECT.
     * @param action Callback executado para cada linha.
     * @param params Parâmetros posicionais.
     * @throws Exception Caso ocorra erro.
     */
    public static void query(String sql, Consumer<ResultSet> action, Object... params) throws Exception {
        try (RunQuery runQuery = new RunQuery(sql).addParameters(params).execute()) {
            runQuery.forEach(action);
        }
    }


    /**
     * Executa uma query e consome os resultados automaticamente.
     *
     * @param sql    SQL SELECT.
     * @param params Parâmetros posicionais.
     * @param action Callback executado para cada linha.
     * @throws Exception Caso ocorra erro.
     */
    public static void query(String sql, Object[] params, Consumer<ResultSet> action) throws Exception {
        try (RunQuery runQuery = new RunQuery(sql).addParameters(params).execute()) {
            runQuery.forEach(action);
        }
    }


    /**
     * Retorna o primeiro resultado da consulta.
     *
     * @param action Conversor do ResultSet.
     * @param <T>    Tipo de retorno.
     * @return Primeiro valor encontrado.
     * @throws Exception Caso ocorra erro.
     */
    public <T> Optional<T> getFirst(ThrowingFunction<ResultSet, T> action) throws Exception {
        if (resultSet == null) return Optional.empty();
        if (resultSet.next()) {
            return Optional.ofNullable(action.apply(resultSet));
        }
        return Optional.empty();
    }

    /**
     * Busca um único valor em uma tabela.
     *
     * @param table  Nome da tabela.
     * @param field  Campo desejado.
     * @param where  Condição WHERE.
     * @param params Parâmetros posicionais.
     * @param <T>    Tipo de retorno.
     * @return Valor encontrado.
     * @throws Exception Caso ocorra erro.
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getFirst(String table, String field, String where, Object... params) throws Exception {
        String sql = String.format("SELECT %s AS COLUNA FROM %s WHERE %s", field, table, where);
        try (RunQuery runQuery = new RunQuery(sql).addParameters(params).execute()) {
            return (Optional<T>) runQuery.getFirst(rs -> rs.getObject("COLUNA"));
        }
    }

    /**
     * Busca um único valor em uma tabela.
     *
     * @param sql    Nome da tabela.
     * @param action Função a ser executada com o resultado.
     * @param <T>    Tipo de retorno.
     * @return Valor encontrado.
     * @throws Exception Caso ocorra erro.
     */
    public static <T> Optional<T> getFirst(String sql, ThrowingFunction<ResultSet, T> action) throws Exception {
        try (RunQuery runQuery = new RunQuery(sql).execute()) {
            return runQuery.getFirst(action);
        }
    }


    /**
     * Retorna o metadata do ResultSet atual.
     *
     * @return Metadata da consulta.
     * @throws SQLException Caso ocorra erro.
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        return resultSet != null ? resultSet.getMetaData() : null;
    }

    /**
     * Fecha todos os recursos JDBC utilizados.
     *
     * @throws SQLException Caso ocorra erro.
     */
    @Override
    public void close() throws SQLException {


        if (resultSet != null && !resultSet.isClosed()) {
            JdbcUtils.closeResultSet(this.resultSet);
        }
        if (this.sql != null) {
            NativeSql.releaseResources(this.sql);
        }
        if (localSession) {
            JdbcWrapper.closeSession(jdbcWrapper);
        }
    }

    /**
     * Retorna um iterator para percorrer o ResultSet.
     *
     * @return Iterator do ResultSet.
     */
    @Override
    public @NotNull Iterator<ResultSet> iterator() {
        if (resultSet == null) {
            throw new IllegalStateException("iterator() só pode ser chamado após um SELECT bem-sucedido.");
        }

        return new Iterator<ResultSet>() {

            private boolean fetched = false;
            private boolean hasNext;

            @Override
            public boolean hasNext() {
                if (!fetched) {
                    try {
                        hasNext = resultSet.next();
                        fetched = true;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
                return hasNext;
            }

            @Override
            public ResultSet next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                fetched = false;
                return resultSet;
            }
        };
    }

    /**
     * Converte uma linha do ResultSet em mapa case-insensitive.
     *
     * @param resultSet Linha atual do ResultSet.
     * @return Mapa contendo colunas e valores.
     * @throws SQLException Caso ocorra erro.
     */
    public static Map<String, Object> toMap(ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int cols = rsmd.getColumnCount();
        Map<String, Object> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (int i = 1; i <= cols; i++) {
            String propKey = rsmd.getColumnLabel(i);
            if (propKey == null || propKey.isEmpty()) {
                propKey = rsmd.getColumnName(i);
            }
            if (propKey == null || propKey.isEmpty()) {
                propKey = Integer.toString(i);
            }
            result.put(propKey, resultSet.getObject(i));
        }

        return result;
    }
}
