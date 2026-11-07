/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Cálculo del importe por bloques de 30 minutos. Los bloques que empiezan en horario
 * pico (de lunes a viernes de 18 a 23, y sábados y domingos todo el día) llevan un
 * recargo del 20 %.
 */
public final class Tarifa {

    public static final BigDecimal RECARGO_PICO = new BigDecimal("1.20");
    private static final LocalTime INICIO_PICO = LocalTime.of(18, 0);

    private Tarifa() {
    }

    public static boolean esPico(LocalDateTime momento) {
        DayOfWeek dia = momento.getDayOfWeek();
        boolean finDeSemana = dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
        return finDeSemana || !momento.toLocalTime().isBefore(INICIO_PICO);
    }

    public static BigDecimal importe(BigDecimal precioHora, LocalDateTime inicio, LocalDateTime fin) {
        BigDecimal mediaHora = precioHora.divide(BigDecimal.TWO);
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDateTime bloque = inicio; bloque.isBefore(fin); bloque = bloque.plusMinutes(30)) {
            total = total.add(esPico(bloque) ? mediaHora.multiply(RECARGO_PICO) : mediaHora);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
