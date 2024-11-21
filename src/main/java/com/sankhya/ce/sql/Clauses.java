package com.sankhya.ce.sql;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

public class Clauses {

    public static String toSqlInClause(Iterable<String> array) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");

        Iterator<String> iterator = array.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }

        sb.append(")");
        return sb.toString();
    }
}
