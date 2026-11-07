/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import uy.echeveste.reservas.dominio.Reserva;
import uy.echeveste.reservas.repositorio.ReservaRepositorio;
import uy.echeveste.reservas.servicio.ReglaIncumplida;
import uy.echeveste.reservas.servicio.ReglaIncumplida.Tipo;
import uy.echeveste.reservas.servicio.ReservaServicio;

@SpringBootTest
@Import(RelojDePrueba.class)
class ReservaServicioTest {

    private static final long FUTBOL_1 = 1;
    private static final Duration HORA = Duration.ofHours(1);

    @Autowired
    ReservaServicio servicio;

    @Autowired
    ReservaRepositorio reservas;

    @BeforeEach
    void limpiar() {
        reservas.deleteAll();
    }

    private Reserva reservar(LocalDateTime inicio, Duration duracion) {
        return servicio.reservar(FUTBOL_1, "Equipo de prueba", "099123456", inicio, duracion);
    }

    private Tipo fallo(Runnable accion) {
        return assertThrows(ReglaIncumplida.class, accion::run).getTipo();
    }

    @Test
    void reservaYCalculaElImporteConRecargoPico() {
        Reserva r = reservar(LocalDateTime.of(2026, 12, 3, 17, 30), Duration.ofMinutes(90));
        assertEquals(new BigDecimal("4080.00"), r.getImporte());
        assertEquals(LocalDateTime.of(2026, 12, 3, 19, 0), r.getFin());
    }

    @Test
    void impideSolapamientosPeroPermiteTurnosContiguos() {
        reservar(LocalDateTime.of(2026, 12, 3, 10, 0), HORA);
        assertEquals(Tipo.CONFLICTO, fallo(() -> reservar(LocalDateTime.of(2026, 12, 3, 10, 30), Duration.ofMinutes(90))));
        assertEquals(Tipo.CONFLICTO, fallo(() -> reservar(LocalDateTime.of(2026, 12, 3, 9, 30), HORA)));
        reservar(LocalDateTime.of(2026, 12, 3, 11, 0), HORA);
        reservar(LocalDateTime.of(2026, 12, 3, 9, 0), HORA);
        // Otra cancha en el mismo horario no tiene conflicto.
        servicio.reservar(2, "Otro equipo", "098765432", LocalDateTime.of(2026, 12, 3, 10, 0), HORA);
    }

    @Test
    void laCancelacionLiberaElHorarioYRespetaLas24Horas() {
        Reserva lejana = reservar(LocalDateTime.of(2026, 12, 4, 20, 0), HORA);
        assertEquals(Reserva.Estado.CANCELADA, servicio.cancelar(lejana.getId()).getEstado());
        reservar(LocalDateTime.of(2026, 12, 4, 20, 0), HORA);
        assertEquals(Tipo.CONFLICTO, fallo(() -> servicio.cancelar(lejana.getId())));

        Reserva cercana = reservar(LocalDateTime.of(2026, 12, 2, 9, 0), HORA); // dentro de 23 horas
        assertEquals(Tipo.CONFLICTO, fallo(() -> servicio.cancelar(cercana.getId())));
        assertEquals(Tipo.NO_ENCONTRADO, fallo(() -> servicio.cancelar(9_999)));
    }

    @Test
    void validaHorariosYDuraciones() {
        assertEquals(Tipo.INVALIDA, fallo(() -> reservar(LocalDateTime.of(2026, 12, 1, 9, 0), HORA)));    // pasado
        assertEquals(Tipo.INVALIDA, fallo(() -> reservar(LocalDateTime.of(2026, 12, 3, 10, 15), HORA)));  // no es punto ni media
        assertEquals(Tipo.INVALIDA, fallo(() -> reservar(LocalDateTime.of(2026, 12, 3, 10, 0), Duration.ofHours(3))));
        assertEquals(Tipo.INVALIDA, fallo(() -> reservar(LocalDateTime.of(2026, 12, 3, 22, 30), HORA))); // pasa el cierre
        assertEquals(Tipo.INVALIDA, fallo(() -> reservar(LocalDateTime.of(2026, 12, 3, 7, 30), HORA)));  // antes de abrir
        assertEquals(Tipo.INVALIDA, fallo(() -> reservar(LocalDateTime.of(2027, 1, 1, 10, 0), HORA)));   // más de 30 días
        assertEquals(Tipo.NO_ENCONTRADO, fallo(() -> servicio.reservar(99, "X", "099123456", LocalDateTime.of(2026, 12, 3, 10, 0), HORA)));
    }

    @Test
    void calculaLaDisponibilidadDelDia() {
        reservar(LocalDateTime.of(2026, 12, 3, 10, 0), Duration.ofMinutes(90));
        List<ReservaServicio.Franja> libres = servicio.disponibilidad(FUTBOL_1, LocalDate.of(2026, 12, 3));
        // 29 franjas de una hora entre 08:00 y 22:00; las que empiezan 09:30, 10:00, 10:30 y 11:00 chocan.
        assertEquals(25, libres.size());
        assertEquals(LocalDateTime.of(2026, 12, 3, 9, 0), libres.get(2).inicio());
        assertEquals(LocalDateTime.of(2026, 12, 3, 11, 30), libres.get(3).inicio());
        // Hoy sólo se ofrecen las franjas que todavía no empezaron.
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 30), servicio.disponibilidad(FUTBOL_1, LocalDate.of(2026, 12, 1)).getFirst().inicio());
    }

    @Test
    void dosPedidosSimultaneosNoGeneranDobleReserva() throws Exception {
        int hilos = 6;
        var largada = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        try (var ejecutor = Executors.newFixedThreadPool(hilos)) {
            for (int i = 0; i < hilos; i++) {
                Callable<Boolean> intento = () -> {
                    largada.await();
                    try {
                        reservar(LocalDateTime.of(2026, 12, 10, 21, 0), HORA);
                        return true;
                    } catch (ReglaIncumplida e) {
                        return false;
                    }
                };
                resultados.add(ejecutor.submit(intento));
            }
            largada.countDown();
            long exitosas = 0;
            for (Future<Boolean> r : resultados) {
                if (r.get()) {
                    exitosas++;
                }
            }
            assertEquals(1, exitosas);
        }
        assertTrue(reservas.findAll().stream().filter(r -> r.getEstado() == Reserva.Estado.CONFIRMADA).count() == 1);
    }
}
