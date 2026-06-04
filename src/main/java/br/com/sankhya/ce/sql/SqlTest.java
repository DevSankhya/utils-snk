package br.com.sankhya.ce.sql;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlTest {
    private JdbcWrapper jdbc;

    SqlTest() {
        EntityFacade entity = EntityFacadeFactory.getDWFFacade();
        jdbc = entity.getJdbcWrapper();
    }


    public int executarUpdate(String sql, Object... params) {
        try (Connection conn = jdbc.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setParams(ps, params);
            int linhas = ps.executeUpdate();
            System.out.println("Linhas afetadas: " + linhas);
            return linhas;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
