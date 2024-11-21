package com.sankhya.ce.data;

import com.sankhya.ce.tuples.Pair;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class ConvertHelper {

    /**
     * Converte um valor em BRL(com ",") para [BigDecimal]
     *
     * @param str Texto a ser convertido
     * @return [BigDecimal]
     * @author Luis Ricardo Alves Santos
     */
    public static BigDecimal convertBrlToBigDecimal(String str) {
        String string = str.replace("\"", "");
        Locale inId = new Locale("pt", "BR");
        DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance(inId);
        nf.setParseBigDecimal(true);
        return (BigDecimal) nf.parse(string, new ParsePosition(0));
    }

    /**
     * Converte uma data do formato especificado em timestamp
     *
     * @param strDate Texto a ser convertido
     * @return [Timestamp]
     * @author Luis Ricardo Alves Santos
     */
    public static Timestamp stringToTimeStamp(String strDate, String format) {
        try {
            DateFormat formatter = new SimpleDateFormat(format);
            Date date = formatter.parse(strDate);
            return new Timestamp(date.getTime());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converte uma data dd/mm/yyyy ou dd-mm-yyyy em timestamp
     *
     * @param strDate Texto a ser convertido
     * @return [Timestamp]
     * @author Luis Ricardo Alves Santos
     */
    public static Timestamp stringToTimeStamp(String strDate) {
        try {
            DateFormat formatter = new SimpleDateFormat(strDate.contains("-") ? "dd-MM-yyyy" : "dd/MM/yyyy");
            Date date = formatter.parse(strDate);
            return new Timestamp(date.getTime());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Converte uma data timestamp em um formato informado
     *
     * @param date Data a ser convertida
     * @return [Timestamp]
     * @author Luis Ricardo Alves Santos
     */
    public static String timeStampToString(Timestamp date, String format) {
        try {
            DateFormat formatter = new SimpleDateFormat(format);
            return formatter.format(date);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Converte uma data timestamp em um formato informado
     *
     * @param date Data a ser convertida
     * @return [Timestamp]
     * @author Luis Ricardo Alves Santos
     */
    public static String timeStampToString(Timestamp date) {
        try {
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            return formatter.format(date);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Retorna a data atual no formato dd/mm/yyyy
     *
     * @return [Pair<Timestamp, String>]
     * @author Luis Ricardo Alves Santos
     */
    public static Pair<Timestamp, String> dateNow() {
        LocalDate dateObj = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return Pair.of(new Timestamp(System.currentTimeMillis()), dateObj.format(formatter));
    }

}
