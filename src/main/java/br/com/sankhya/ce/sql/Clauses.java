package br.com.sankhya.ce.sql;

import java.util.Iterator;

public class Clauses {
    public static String toSqlInClause(Iterable<Object> array) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        Iterator<Object> iterator = array.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next().toString());
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }

        sb.append(")");
        return sb.toString();
    }

}
