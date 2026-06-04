package br.com.sankhya.ce.sql.interfaces;

import java.sql.ResultSet;

@FunctionalInterface
public interface ResultSetConsumer {
    void accept(ResultSet rs) throws Exception;
}
