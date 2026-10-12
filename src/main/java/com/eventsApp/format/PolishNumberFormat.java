package com.eventsApp.format;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Liczby i kwoty tak, jak pisze się je po polsku: tysiące rozdzielone spacją, przecinek dziesiętny.
 * Jedno miejsce dla PDF-a, pól informacji i maili, żeby kwota wyglądała wszędzie tak samo.
 */
public final class PolishNumberFormat {

    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    private PolishNumberFormat() {
    }

    /** 31231 → "31 231". */
    public static String grouped(long value) {
        return format("#,##0", value);
    }

    /** 31231 → "31 231 zł". */
    public static String wholeMoney(long amount) {
        return grouped(amount) + " zł";
    }

    /** 2200 → "2 200,00 zł"; brak wartości liczy się jak zero. */
    public static String money(BigDecimal value) {
        return format("#,##0.00", value != null ? value : BigDecimal.ZERO) + " zł";
    }

    /** DecimalFormat nie jest bezpieczny wątkowo, więc każde wywołanie dostaje własny. */
    private static String format(String pattern, Object number) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(POLISH);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat(pattern, symbols).format(number);
    }
}
