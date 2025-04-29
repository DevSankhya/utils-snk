package br.com.sankhya.ce.jape;

import br.com.sankhya.ce.sql.ResolveSqlTypes;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {
    private final Where where = new Where();
    private final List<Object> values = new ArrayList<>();

    public Where where(String where) {
        this.where.setWhere(where);
        return this.where;
    }

    public Where where() {
        return this.where;
    }


    public class Where {
        private String where = "";

        public void setWhere(String where) {
            this.where = where;
        }

        public Where(String where) {
            this.where = where;
        }

        public Where() {
            this.where = "";
        }

        public void and(String where) {
            if (this.where.isEmpty()) {
                this.where = where;
                return;
            }
            this.where += " AND " + where;
        }

        public void or(String where) {
            this.where += " OR " + where;
        }

        public String get() {
            return this.where;
        }

        public void setParam(String param, Object value) {
            ResolveSqlTypes resolveSqlTypes = new ResolveSqlTypes(getDialect());
            where = resolveSqlTypes.replaceParameters(where, values);
            where = where.replace(param, value.toString());
        }

        private ResolveSqlTypes.Database getDialect() {
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
    }
}
