package br.com.sankhya.ce.sql.enums;

public enum SqlCommandCategory {

    DQL,        // SELECT

    DML,        // INSERT UPDATE DELETE MERGE

    DDL,        // CREATE DROP ALTER TRUNCATE

    METADATA,   // SHOW DESCRIBE EXPLAIN

    UNKNOWN
}
