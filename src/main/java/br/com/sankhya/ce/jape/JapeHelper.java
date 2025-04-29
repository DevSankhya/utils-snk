package br.com.sankhya.ce.jape;

import br.com.sankhya.ce.sql.Clauses;
import br.com.sankhya.ce.sql.ResolveSqlTypes;
import br.com.sankhya.ce.sql.RunQuery;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;
import org.jdom.Element;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
public class JapeHelper {
    /**
     * Método para criar um novo registro na instância informada.
     *
     * @param values:   HashMap<String, Object>: Nomes e valores dos campos.
     * @param instance: String: instancia a ser criado o novo registro
     * @return String
     * @author Luis Ricardo Alves Santos
     */
    private static DynamicVO createNewLine(HashMap<String, Object> values, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        StringBuilder listValues = new StringBuilder();
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidCreateVO fluidCreateVO = instanciaDAO.create();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidCreateVO.set(name, value);
                listValues.append(name).append("= ").append(value).append("\n");
            }
            return fluidCreateVO.save();
        } catch (Exception e) {
            if (e.getMessage().contains("transação ativa")) return createNewLine(values, instance, true);
            throw new MGEModelException("createNewLine Error:" + e.getMessage() + "\n Values:\n" + listValues);
        } finally {
            JapeSession.close(hnd);
        }
    }

    private static DynamicVO createNewLine(HashMap<String, Object> values, String instance, Boolean repeat) throws MGEModelException {

        JapeSession.SessionHandle hnd = null;
        StringBuilder listValues = new StringBuilder();
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidCreateVO fluidCreateVO = instanciaDAO.create();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidCreateVO.set(name, value);
                listValues.append(name).append("= ").append(value).append("\n");
            }
            return fluidCreateVO.save();
        } catch (Exception e) {
            throw new MGEModelException("createNewLine Error:" + e.getMessage() + "\n Values:\n" + listValues);
        } finally {
            JapeSession.close(hnd);
        }
    }

    private static DynamicVO createNewLine(String instance, DynamicVO vo) throws MGEModelException {

        JapeHelper.setSessionProperties();
        try {
            EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
            PersistentLocalEntity entity = dwfFacade.createEntity(instance, (EntityVO) vo);

            return (DynamicVO) entity.getValueObject();
        } catch (Exception e) {
            throw new MGEModelException("createNewLine Error:" + e.getMessage());
        }
    }

    private static JSONObject insertJDBC(HashMap<String, Object> values, String table, Boolean repeat) throws MGEModelException {

        StringBuilder listValues = new StringBuilder();
        NativeSql sql = null;
        try {
            JdbcWrapper jdbc = null;
            EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
            jdbc = dwfEntityFacade.getJdbcWrapper();
            jdbc.openSession();

            sql = new NativeSql(jdbc);
//            PreparedStatement preparedStatement = jdbc.getPreparedStatement("");
            Set<String> fieldsSet = values.keySet();
            String fields = Clauses.toSqlInClause(fieldsSet);
            String params = Clauses.toSqlInClause(fieldsSet.stream().map(item -> "?").collect(Collectors.toList()));
            List<Object> insertValues = new ArrayList<>();
            String insert = "INSERT INTO " + table + " " + fields + " VALUES " + params + ";";
            sql.appendSql(insert);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                listValues.append(name).append("= ").append(value).append("\n");
            }

            sql.executeUpdate();
            String codition = fieldsSet.stream().map(o -> o + "=?").collect(Collectors.joining());

            sql = new NativeSql(jdbc);
            String select = "Select * from " + table + "  where  " + codition + ";";
            sql.appendSql(select);
            NativeSql finalSql = sql;


            RunQuery runQuery = new RunQuery(select, (nativeSql -> {
                values.values().forEach(nativeSql::addParameter);
                return nativeSql;
            }), false);

            List<JSONObject> results = runQuery.toList();

            if (results.isEmpty()) return null;
            return results.get(0);
        } catch (Exception e) {
            throw new MGEModelException("createNewLine Error:" + e.getMessage() + "\n Values:\n" + listValues);
        } finally {
            NativeSql.releaseResources(sql);
            NativeSql.releaseResources(sql);
        }
    }


    /**
     * Retorna o valor de um campo(PK)
     *
     * @param name      Nome da propriedade
     * @param it        DynamicVO do registro
     * @param instancia Instância - Default: Instância atual
     * @return [T]
     */
    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T getCampo(String name, DynamicVO it, String instancia) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            DynamicVO fluidGetVO = instanciaDAO.findByPK(it.getPrimaryKey());
            return (T) fluidGetVO.getProperty(name);
        } catch (Exception e) {
            throw new MGEModelException("getCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna o valor de um campo(Where)
     *
     * @param name      Nome do campo
     * @param where     Condição para retornar o registro
     * @param instancia Instância - Default: Instância atual
     * @return [T]
     */
    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T getCampo(String name, String where, String instancia) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            return (T) fluidGetVO.getProperty(name);
        } catch (Exception e) {
            throw new MGEModelException("getCampo Error(Entity: " + instancia + "):" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    public static <T> T getCampo(String name, String where, String instancia, boolean canBeNull) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            try {

                return (T) fluidGetVO.getProperty(name);
            } catch (Exception e) {
                return null;
            }
        } catch (Exception e) {
            throw new MGEModelException("getCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna o DynamicVO baseado em uma consulta
     *
     * @param instancia Nome da instancia
     * @param where     Condição para retornar o registro
     */
    @SuppressWarnings({"unused"})
    public static DynamicVO getVO(String instancia, String where) throws MGEModelException {
        DynamicVO dynamicVo;
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            dynamicVo = instanciaDAO.findOne(where);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MGEModelException("Erro getVO(" + instancia + "): " + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
        return dynamicVo;
    }


    private static ResolveSqlTypes.Database getDialect() {
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        JdbcWrapper jdbcWrapper = dwfFacade.getJdbcWrapper();
        String databaseProductName = "unknown";
        try {
            if (jdbcWrapper.isOracle()) return ResolveSqlTypes.Database.ORACLE;
            return ResolveSqlTypes.Database.MSSQL;
        } catch (Exception e) {
            return ResolveSqlTypes.Database.UNKNOW;
        } finally {
            jdbcWrapper.closeSession();
        }
    }

    /**
     * Retorna o DynamicVO baseado em uma consulta
     *
     * @param instancia Nome da instancia
     * @param where     Condição para retornar o registro
     */
    @SuppressWarnings({"unused"})
    public static DynamicVO getVO(String instancia, String where, Object... values) throws MGEModelException {
        DynamicVO dynamicVo;
        JapeSession.SessionHandle hnd = null;
        ResolveSqlTypes resolveSqlTypes = new ResolveSqlTypes(getDialect());
        where = resolveSqlTypes.replaceParameters(where, values);
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            dynamicVo = instanciaDAO.findOne(where);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MGEModelException("Erro getVO(" + instancia + "): " + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
        return dynamicVo;
    }


    public static boolean deleteVO(DynamicVO vo, String instance) throws MGEModelException {

        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper empresaDAO = JapeFactory.dao(instance);
            return empresaDAO.delete(vo.getPrimaryKey());
        } catch (Exception e) {
            throw new MGEModelException("deleteVO error(" + instance + "):" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Alterar o valor do campo informado
     *
     * @param name     Nome do campo
     * @param value    Valor
     * @param vo       DynamicVO do item a ser atualizado
     * @param instance Instância - Default: Instância atual
     */
    public static void setCampo(String name, Object value, DynamicVO vo, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdateByPK(vo.getPrimaryKey());
            fluidupdate.set(name, value);
            fluidupdate.update();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MGEModelException("setCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Retorna os registros baseados em uma consulta
     *
     * @param instancia Nome da instancia
     * @param where     Condição para retornar o registro
     */
    public static Collection<DynamicVO> getVOs(String instancia, String where, Object... values) throws MGEModelException {
        Collection<DynamicVO> dynamicVo;
        JapeSession.SessionHandle hnd = null;
        ResolveSqlTypes resolveSqlTypes = new ResolveSqlTypes(getDialect());
        where = resolveSqlTypes.replaceParameters(where, values);
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instancia);
            dynamicVo = instanciaDAO.find(where);
        } catch (Exception e) {
            throw new MGEModelException("Erro getVOs(" + instancia + "): " + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
        return dynamicVo;
    }

    /**
     * Alterar o valor do campo informado
     *
     * @param name     Nome do campo
     * @param value    Valor
     * @param where    condição a ser cumprida
     * @param instance Instância - Default: Instância atual
     */
    public static void setCampo(String name, Object value, String where, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdateByPK(fluidGetVO.getPrimaryKey());
            fluidupdate.set(name, value);
            fluidupdate.update();
        } catch (Exception e) {
            throw new MGEModelException("setCampo Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Alterar o valor de mutiplos campos informados
     *
     * @param values   Hashmap com nome e valor do campo
     * @param vo       DynamicVO do item a ser atualizado
     * @param instance Instância - Default: Instância atual
     * @return DynamicVO
     */
    public static DynamicVO setCampos(HashMap<String, Object> values, DynamicVO vo, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdate(vo);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidupdate.set(name, value);
            }
            fluidupdate.update();
            return instanciaDAO.findByPK(vo.getPrimaryKey());
        } catch (Exception e) {
            throw new MGEModelException("setCampos Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    /**
     * Alterar o valor de mutiplos campos informados
     *
     * @param values   Hashmap com nome e valor do campo
     * @param where    condição do item a ser atualizado
     * @param instance Instância - Default: Instância atual
     * @return DynamicVO
     */
    public static DynamicVO setCampos(HashMap<String, Object> values, String where, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);
            FluidUpdateVO fluidupdate = instanciaDAO.prepareToUpdate(fluidGetVO);
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidupdate.set(name, value);
            }
            fluidupdate.update();
            return instanciaDAO.findOne(where);
        } catch (Exception e) {
            throw new MGEModelException("setCampos Error:" + e.getMessage());
        } finally {
            JapeSession.close(hnd);
        }
    }

    private static void setSessionProperties() {
        AuthenticationInfo auth = new AuthenticationInfo("SUP", BigDecimal.ZERO, BigDecimal.ZERO, 1);
        auth.makeCurrent();

        ServiceContext sctx = new ServiceContext(null);
        sctx.setAutentication(AuthenticationInfo.getCurrent());
        Element bodyElem = new Element("serviceRequest");
        Element requestBody = new Element("requestBody");
        bodyElem.addContent(requestBody);
        sctx.setRequestBody(bodyElem);
        sctx.makeCurrent();
        try {
            SPBeanUtils.setupContext(sctx);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class CreateNewLine {
        private String instance;
        private HashMap<String, Object> values = new HashMap<>();
        private DynamicVO vo;

        public CreateNewLine(String instance) {
            this.instance = instance;
        }

        public DynamicVO save() throws MGEModelException {
            return createNewLine(values, instance);
        }

        public DynamicVO saveVO() throws MGEModelException {
            return createNewLine(instance, this.vo);
        }

        public void setVO(DynamicVO vo) {
            this.vo = vo;
        }

        public void set(String label, Object value) {
            values.put(label, value);
        }

        public void flush() {
            values = new HashMap<>();
        }

        public void remove(String label) {
            values.remove(label);
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }
    }

}
