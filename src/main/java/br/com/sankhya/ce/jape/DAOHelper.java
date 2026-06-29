package br.com.sankhya.ce.jape;

import br.com.sankhya.ce.sql.ResolveSqlTypes;
import br.com.sankhya.ce.sql.RunQuery;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.EntityDAO;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DAOHelper {

    private static final Logger log = Logger.getLogger(DAOHelper.class.getName());

    private static final Map<String, JapeWrapper> DAO_CACHE = new ConcurrentHashMap<>();

    private DAOHelper() {
    }

    // =========================================================
    // CALLBACK
    // =========================================================

    @FunctionalInterface
    public interface DaoCallback<T> {
        T execute(JapeWrapper dao) throws Exception;
    }

    // =========================================================
    // SESSION
    // =========================================================

    private static <T> T withDao(String instance, DaoCallback<T> callback) throws MGEModelException {

        Result<T> result = Jape.withSession(() -> {

            JapeWrapper dao = getDao(instance);

            return callback.execute(dao);
        });

        if (result.isError()) {
            Exception cause = result.getExceptionOrNull();

            log.log(
                Level.SEVERE,
                "Erro na entidade: " + instance,
                cause
            );

            throw new MGEModelException(
                "Erro na entidade '" + instance + "'",
                cause
            );
        }

        return result.getOrNull();
    }

    private static <T> T withTransaction(String instance, DaoCallback<T> callback)
        throws MGEModelException {

        Result<T> result = Jape.secureTransaction(() -> {

            JapeWrapper dao = getDao(instance);

            return callback.execute(dao);
        });

        if (result.isError()) {

            Exception cause = result.getExceptionOrNull();

            log.log(
                Level.SEVERE,
                "Erro na entidade: " + instance,
                cause
            );

            throw new MGEModelException(
                "Erro na entidade '" + instance + "'",
                cause
            );
        }

        return result.getOrNull();
    }

    // =========================================================
    // DAO
    // =========================================================

    private static JapeWrapper getDao(String instance) {

        return DAO_CACHE.computeIfAbsent(instance, JapeFactory::dao);
    }

    // =========================================================
    // FIND
    // =========================================================

    public static DynamicVO findOne(String instance, String where, Object... params) throws MGEModelException {

        String resolvedWhere = resolveWhere(where, params);

        return withDao(instance, dao -> dao.findOne(resolvedWhere));
    }

    public static Collection<DynamicVO> findAll(String instance, String where, Object... params) throws MGEModelException {

        String resolvedWhere = resolveWhere(where, params);

        return withDao(instance, dao -> dao.find(resolvedWhere));
    }

    public static Optional<DynamicVO> findOptional(String instance, String where, Object... params) {

        try {

            return Optional.ofNullable(findOne(instance, where, params));

        } catch (Exception e) {

            return Optional.empty();
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    public static DynamicVO create(String instance, Map<String, ?> values) throws MGEModelException {

        return withTransaction(instance, dao -> {

            FluidCreateVO create = dao.create();

            values.forEach(create::set);

            return create.save();
        });
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public static DynamicVO update(String instance, DynamicVO vo, Map<String, ?> values) throws MGEModelException {

        return withTransaction(instance, dao -> {

            FluidUpdateVO update = dao.prepareToUpdate(vo);

            values.forEach(update::set);

            update.update();

            return dao.findByPK(vo.getPrimaryKey());
        });
    }

    public static DynamicVO update(String instance, Map<String, ?> values, String where, Object... params) throws MGEModelException {

        String resolvedWhere = resolveWhere(where, params);

        return withTransaction(instance, dao -> {

            DynamicVO vo = dao.findOne(resolvedWhere);

            if (vo == null) {

                throw new IllegalArgumentException("Registro não encontrado.");
            }

            FluidUpdateVO update = dao.prepareToUpdate(vo);

            values.forEach(update::set);

            update.update();

            return dao.findByPK(vo.getPrimaryKey());
        });
    }

    public static int updateMany(String instance, String where, Map<String, ?> values, Object... params) throws MGEModelException {

        String resolvedWhere = resolveWhere(where, params);

        return withTransaction(instance, dao -> {

            Collection<DynamicVO> records = dao.find(resolvedWhere);

            int updated = 0;

            for (DynamicVO vo : records) {

                FluidUpdateVO update = dao.prepareToUpdate(vo);

                values.forEach(update::set);

                update.update();

                updated++;
            }

            return updated;
        });
    }

    // =========================================================
    // DELETE
    // =========================================================

    public static boolean delete(String instance, DynamicVO vo) throws MGEModelException {
        return withTransaction(instance, dao -> dao.delete(vo.getPrimaryKey()));
    }

    public static boolean delete(String instance, String where, Object... params) throws MGEModelException {

        String resolvedWhere = resolveWhere(where, params);

        return withTransaction(instance, dao -> {

            DynamicVO vo = dao.findOne(resolvedWhere);

            if (vo == null) {
                return false;
            }

            return dao.delete(vo.getPrimaryKey());
        });
    }

    public static int deleteMany(String instance, String where, Object... params) throws MGEModelException {

        String resolvedWhere = resolveWhere(where, params);

        return withTransaction(instance, dao -> dao.deleteByCriteria(where, params));
    }

    // =========================================================
    // SAVE
    // =========================================================

    public static DynamicVO save(String instance, DynamicVO vo) throws Exception {
        return withTransaction(instance, dao -> {
            EntityFacade facade = EntityFacadeFactory.getDWFFacade();

            PersistentLocalEntity entity = facade.saveEntity(instance, (EntityVO) vo);

            return (DynamicVO) entity.getValueObject();
        });
    }

    public static DynamicVO save(DynamicVO vo) throws Exception {
        return save(getEntity(vo), vo);
    }

    // =========================================================
    // FIELD
    // =========================================================

    @SuppressWarnings("unchecked")
    public static <T> T get(DynamicVO vo, String field) {
        return (T) vo.getProperty(field);
    }

    public static void set(DynamicVO vo, String field, Object value) throws MGEModelException {
        DAOHelper.update(getEntity(vo), vo, Collections.singletonMap(field, value));
    }

    // =========================================================
    // ENTITY
    // =========================================================

    public static String getEntity(@NotNull DynamicVO vo) {
        String id = vo.getValueObjectID();

        if (id == null) {
            throw new IllegalStateException("ValueObjectID null");
        }

        int index = id.indexOf('.');

        return index >= 0 ? id.substring(0, index) : id;
    }

    public static String getTable(String instance) throws Exception {
        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
        EntityDAO dao = dwfFacade.getDAOInstance(instance);

        return dao.getEntity().getEntityObject(dao.getEntity().getMainObject()).getTable();
    }
    // =========================================================
    // DIALECT
    // =========================================================

    public static ResolveSqlTypes.Database getDialect() {

        JdbcWrapper jdbc = EntityFacadeFactory.getDWFFacade().getJdbcWrapper();

        try {

            if (jdbc.isOracle()) {
                return ResolveSqlTypes.Database.ORACLE;
            }

            return ResolveSqlTypes.Database.MSSQL;

        } catch (Exception e) {

            return ResolveSqlTypes.Database.UNKNOW;

        } finally {

            jdbc.closeSession();
        }
    }

    // =========================================================
    // WHERE
    // =========================================================

    private static String resolveWhere(String where, Object... params) {

        ResolveSqlTypes resolve = new ResolveSqlTypes(getDialect());

        return resolve.replaceParameters(where, params);
    }

    // =========================================================
    // BUILDER
    // =========================================================

    public static Builder builder(String instance) {

        return new Builder(instance);
    }

    public static class Builder {

        private final String instance;

        private final Map<String, Object> values = new LinkedHashMap<>();

        private DynamicVO vo;

        public Builder(String instance) {

            this.instance = instance;
        }

        public Builder set(String field, Object value) {

            values.put(field, value);

            return this;
        }

        public Builder vo(DynamicVO vo) {

            this.vo = vo;

            return this;
        }

        public Builder clear() {

            values.clear();

            return this;
        }

        public DynamicVO create() throws MGEModelException {

            return DAOHelper.create(instance, values);
        }

        public DynamicVO update() throws MGEModelException {

            if (vo == null) {

                throw new IllegalStateException("VO não informado.");
            }

            return DAOHelper.update(instance, vo, values);
        }

        public DynamicVO update(String where, Object... params) throws MGEModelException {
            return DAOHelper.update(instance, values, where, params);
        }
    }

    // =========================================================
    // EXISTS
    // =========================================================

    public static boolean exists(String instance, String where, Object... params) {
        return findOptional(instance, where, params).isPresent();
    }

    // =========================================================
    // COUNT
    // =========================================================

    public static int count(String instance, String where, Object... params) throws Exception {
        String table = getTable(instance);
        String resolvedWhere = resolveWhere(where, params);
        String query = String.format("Select COUNT(*) AS TOTAL from %s", table);
        if (resolvedWhere != null && !resolvedWhere.trim().isEmpty()) {
            query += " where " + resolvedWhere;
        }

        Optional<Integer> total = RunQuery.getFirst(query, rs -> rs.getInt("TOTAL"));

        if (!total.isPresent())
            throw new IllegalArgumentException(String.format("Não foi possivel contar as linhas da tabela %s", table));

        return total.get();
    }

    public static int count(String instance) throws Exception {
        return count(instance, null);
    }

    // =========================================================
    // REFRESH CACHE
    // =========================================================

    public static void clearDaoCache() {
        DAO_CACHE.clear();
    }
}
