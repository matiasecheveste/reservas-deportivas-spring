/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fija "ahora" en el martes 1 de diciembre de 2026 a las 10:00 de Montevideo. */
@TestConfiguration
public class RelojDePrueba {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 10, 0);

    @Bean
    @Primary
    Clock relojFijo() {
        return Clock.fixed(AHORA.atZone(ReservasApplication.ZONA).toInstant(), ReservasApplication.ZONA);
    }
}
