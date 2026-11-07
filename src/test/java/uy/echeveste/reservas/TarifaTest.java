/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import uy.echeveste.reservas.servicio.Tarifa;

class TarifaTest {

    @ParameterizedTest(name = "{0} a {1} a ${2}/h = ${3}")
    @CsvSource({
            // martes por la mañana: sin recargo
            "2026-12-01T10:00, 2026-12-01T11:00, 2400, 2400.00",
            // martes 17:30 a 19:00: un bloque normal y dos en horario pico
            "2026-12-01T17:30, 2026-12-01T19:00, 2400, 4080.00",
            // sábado: todo el día es pico
            "2026-12-05T10:00, 2026-12-05T11:30, 1600, 2880.00",
            // media hora con precio impar: se redondea al centésimo
            "2026-12-01T09:00, 2026-12-01T10:00, 1255, 1255.00",
    })
    void calculaElImportePorBloquesDeMediaHora(LocalDateTime inicio, LocalDateTime fin, BigDecimal precioHora, BigDecimal esperado) {
        assertEquals(esperado, Tarifa.importe(precioHora, inicio, fin));
    }
}
