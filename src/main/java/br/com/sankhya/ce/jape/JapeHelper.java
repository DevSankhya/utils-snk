package br.com.sankhya.ce.jape;

import br.com.sankhya.ce.sql.ResolveSqlTypes;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PKNullElementError;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.vo.VOProperty;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jdom.Element;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings({"unused"})
public class JapeHelper {


    public static class SessionManager {
        private boolean internalTransaction = true;
        private JapeSession.SessionHandle hnd;

        public SessionManager() {
            hnd = open();
        }

        public JapeSession.SessionHandle open() {
            JapeSession.SessionHandle hnd;
            boolean hasCurrentSession = JapeSession.hasCurrentSession();
            boolean hasTransaction = false;
            if (hasCurrentSession) hasTransaction = JapeSession.getCurrentSession().hasTransaction();
            if (hasCurrentSession && hasTransaction) {
                this.internalTransaction = false;
                hnd = JapeSession.getCurrentSession().getTopMostHandle();
            } else hnd = JapeSession.open();
            this.hnd = hnd;
            return hnd;
        }

        public void canTimeout(boolean canTimeout) {
            this.hnd.setCanTimeout(canTimeout);
            this.hnd.setSessionTimeout(Long.MAX_VALUE);
        }

        public void close() {
            if (!this.internalTransaction) // N?o fecha a conex?o se ela n?o pertence a instancia desta, pois esta aproveita a conex?o externa
                return;

            JapeHelper.closeHnd(hnd);
        }

        public void close(JapeSession.SessionHandle hnd) {
            if (!this.internalTransaction) // N?o fecha a conex?o se ela n?o pertence a instancia desta, pois esta aproveita a conex?o externa
                return;

            JapeHelper.closeHnd(hnd);
        }
    }

    public static void closeHnd(JapeSession.SessionHandle hnd) {
        boolean hasCurrentSession = JapeSession.hasCurrentSession();
        boolean hasTransaction = false;
        if (hasCurrentSession) hasTransaction = JapeSession.getCurrentSession().hasTransaction();

        if (hnd != null && hasTransaction) JapeSession.close(hnd);
    }

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


    public static ResolveSqlTypes.Database getDialect() {
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


    public static DynamicVO update(Map<String, Object> values, String instance, String where, Object... params) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        StringBuilder listValues = new StringBuilder();
        JapeHelper.setSessionProperties();
        ResolveSqlTypes resolveSqlTypes = new ResolveSqlTypes(getDialect());
        where = resolveSqlTypes.replaceParameters(where, params);
        try {
            hnd = JapeSession.open();
            JapeWrapper instanciaDAO = JapeFactory.dao(instance);
            DynamicVO fluidGetVO = instanciaDAO.findOne(where);

            JapeWrapper updateDAO = JapeFactory.dao(instance);

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                fluidGetVO.setProperty(name, value);
                listValues.append(name).append("= ").append(value).append("\n");
            }
            return JapeHelper.updateVO(instance, fluidGetVO);
        } catch (Exception e) {
            throw new MGEModelException("createNewLine Error:" + e.getMessage() + "\n Values:\n" + listValues);
        } finally {
            JapeSession.close(hnd);
        }
    }


    public static DynamicVO updateVO(String instance, DynamicVO vo) throws Exception {
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        PersistentLocalEntity entity = dwfFacade.saveEntity(instance, (EntityVO) vo);

        return (DynamicVO) entity.getValueObject();
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

    public static DynamicVO persist(String instance, DynamicVO vo) throws Exception {
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        try {
            PersistentLocalEntity entity = dwfFacade.saveEntity(instance, (EntityVO) vo);
            return (DynamicVO) entity.getValueObject();
        } catch (PKNullElementError e) {
            return createNewLine(instance, vo);
        }

    }

    private static DynamicVO persist(HashMap<String, Object> values, String instance) throws MGEModelException {
        JapeSession.SessionHandle hnd = null;
        StringBuilder listValues = new StringBuilder();
        JapeHelper.setSessionProperties();
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
            throw new MGEModelException("persist Error(" + instance + "):" + e.getMessage() + "\n Values:\n" + listValues);
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


    public static class PersistLine {
        private String instance;
        private HashMap<String, Object> values = new HashMap<>();
        private DynamicVO vo;
        private final Gson gson = new Gson();

        public PersistLine(String instance) {
            this.instance = instance;
        }


        public DynamicVO persist() throws Exception {
            if (this.vo != null && vo.getPrimaryKey() != null) return JapeHelper.persist(instance, this.vo);
            return JapeHelper.persist(values, instance);
        }

        public void setVO(DynamicVO vo) {
            this.vo = vo;
        }

        public DynamicVO updateVO() throws Exception {
            return JapeHelper.updateVO(instance, this.vo);
        }

        public DynamicVO update(String where, Object... params) throws MGEModelException {
            return JapeHelper.update(values, instance, where, params);
        }

        public PersistLine findOne(String where, Object... params) throws MGEModelException {
            PersistLine dao = new PersistLine(instance);
            DynamicVO vo1 = JapeHelper.getVO(instance, where, params);
            dao.setVO(vo1);
            return dao;
        }

        public DynamicVO getVo() {
            return vo;
        }

        public String getJson() {
            if (this.vo != null) return toJson(vo);
            else return gson.toJson(values);
        }

        public void set(String label, Object value) {
            if (this.vo != null) this.vo.setProperty(label, value);
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

        private String toJson(DynamicVO vo) {
            if (vo == null) return null;
            JsonObject root = new JsonObject();
            Iterator iterator = vo.iterator();
            while (iterator.hasNext()) {
                VOProperty entry = (VOProperty) iterator.next();
                String name = entry.getName();

                if (isUpperCase(name)) root.addProperty(name, entry.getValue() + "");
            }
            return gson.toJson(root);
        }

        private boolean isUpperCase(String str) {
            return str.equals(str.toUpperCase());
        }
    }


}
