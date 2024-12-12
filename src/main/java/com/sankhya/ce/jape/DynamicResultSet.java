package com.sankhya.ce.jape;

import br.com.sankhya.jape.vo.DynamicVO;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class DynamicResultSet implements DynamicVO {
    private final HashMap<String, Object> row = new HashMap<>();
    private Object pk = null;

    private Set<Object> getPrimaryKeyColumnsForTable(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet rs = metaData.getExportedKeys("", "", tableName);
        Set<Object> pkColumns = new HashSet<>();
        while (rs.next()) {
            Object pkey = rs.getObject("PKCOLUMN_NAME");
            pkColumns.add(pkey);
        }
        return pkColumns;
    }

    private DynamicResultSet(HashMap<String, Object> row) {
        this.row.putAll(row);
    }

    public DynamicResultSet(ResultSet resultSet) {
        ResultSetMetaData rsmd;
        int numColumns;
        try {
            rsmd = resultSet.getMetaData();
            Connection conn = resultSet.getStatement().getConnection();
            String tableName = rsmd.getTableName(1);
            Set<Object> pkColumns = getPrimaryKeyColumnsForTable(conn, tableName);
            Object[] pk = pkColumns.toArray();
            if (pk.length > 1) {
                this.pk = pk[0];
            } else {
                this.pk = pk;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            numColumns = rsmd.getColumnCount();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for (int i = 1; i <= numColumns; i++) {
            String columnName;
            try {
                columnName = rsmd.getColumnName(i);
                this.row.put(columnName, resultSet.getObject(columnName));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static DynamicResultSet of(ResultSet resultSet) {
        return new DynamicResultSet(resultSet);
    }

    @Override
    public BigDecimal asBigDecimal(String s) {
        return (BigDecimal) this.row.get(s);
    }

    @Override
    public BigDecimal asBigDecimalOrZero(String s) {
        BigDecimal value = (BigDecimal) this.row.get(s);
        if (value == null) return BigDecimal.ZERO;
        return value;
    }

    @Override
    public byte[] asBlob(String s) {
        try {
            Blob blob = (Blob) this.row.get(s);
            int blobLength = (int) blob.length();
            byte[] blobAsBytes = blob.getBytes(1, blobLength);
            blob.free();
            return blobAsBytes;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean asBoolean(String s) {
        Object value = this.row.get(s);
        if (value == null) return false;
        return (boolean) this.row.get(s);
    }

    @Override
    public char[] asClob(String s) {
        Clob clob;
        clob = (Clob) this.row.get(s);
        String clobAsString;
        try (Reader reader = clob.getCharacterStream(); StringWriter w = new StringWriter()) {
            char[] buffer = new char[4096];
            int charsRead;
            while ((charsRead = reader.read(buffer)) != -1) {
                w.write(buffer, 0, charsRead);
            }
            clobAsString = w.toString();
            return clobAsString.toCharArray();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection asCollection(String s) {
        Object object = this.row.get(s);
        if (object instanceof Collection) return (Collection) object;
        throw new RuntimeException("Not possible");
    }

    @Override
    public double asDouble(String s) {
        Object o = this.row.get(s);
        if (o == null) return 0;
        return (double) o;
    }

    @Override
    public DynamicVO asDymamicVO(String s) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public int asInt(String s) {
        Object o = this.row.get(s);
        if (o == null) return 0;
        return (int) o;
    }

    @Override
    public long asLong(String s) {
        Object o = this.row.get(s);
        if (o == null) return 0;
        return (long) o;
    }

    @Override
    public String asString(String s) {
        Object o = this.row.get(s);
        return o.toString();
    }

    @Override
    public Timestamp asTimestamp(String s) {
        return (Timestamp) this.row.get(s);
    }

    @Override
    public DynamicVO buildClone() {
        return new DynamicResultSet(this.row);
    }

    @Override
    public void clean() {
        this.row.clear();
    }

    @Override
    public void clearReferences() {
        this.row.clear();
    }

    @Override
    public boolean containsProperty(String s) {
        return this.row.containsKey(s);
    }

    @Override
    public void copyPersistentPropertiesTo(DynamicVO dynamicVO) {
        for (Map.Entry<String, Object> entry : this.row.entrySet()) {
            dynamicVO.setProperty(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean dataEquals(Object o) {
        if (o instanceof DynamicResultSet) {
            DynamicResultSet other = (DynamicResultSet) o;
            return this.row.equals(other.row);
        }
        return false;
    }

    @Override
    public boolean equals(DynamicVO dynamicVO) {
        if (dynamicVO instanceof DynamicResultSet) {
            DynamicResultSet other = (DynamicResultSet) dynamicVO;
            return this.row.equals(other.row);
        }
        return false;
    }

    @Override
    public String getElementName() {
        return null;
    }

    @Override
    public Object getPrimaryKey() {
        return this.pk;
    }

    @Override
    public Object getProperty(String s) {
        return this.row.get(s);
    }

    @Override
    public Object getUserObject() {
        return null;
    }

    @Override
    public String getValueObjectID() {
        return "";
    }

    @Override
    public boolean isDeleted() {
        return false;
    }

    @Override
    public boolean isProxyProperty(String s) {
        return false;
    }

    @Override
    public Iterator iterator() {
        return null;
    }

    @Override
    public void lock(String s) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void resolveProxies() {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void setAceptTransientProperties(boolean b) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void setDeleted(boolean b) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void setElementName(String s) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void setPrimaryKey(Object o) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void setProperty(String s, Object o) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public void setUserObject(Object o) {
        throw new RuntimeException("Not possible");
    }

    @Override
    public DynamicVO wrapInterface(Class aClass) {
        return null;
    }
}
