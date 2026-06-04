package br.com.sankhya.ce.sql.interfaces;

import java.sql.ResultSet;

@FunctionalInterface
public interface ResultSetPredicate {
    boolean test(ResultSet rs) throws Exception;
}
