/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReservasApplication {

    public static final ZoneId ZONA = ZoneId.of("America/Montevideo");

    public static void main(String[] args) {
        SpringApplication.run(ReservasApplication.class, args);
    }

    /** Reloj inyectable: las pruebas lo reemplazan por uno fijo. */
    @Bean
    Clock reloj() {
        return Clock.system(ZONA);
    }
}
