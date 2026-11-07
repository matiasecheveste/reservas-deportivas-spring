/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.servicio;

import static uy.echeveste.reservas.servicio.ReglaIncumplida.Tipo.CONFLICTO;
import static uy.echeveste.reservas.servicio.ReglaIncumplida.Tipo.INVALIDA;
import static uy.echeveste.reservas.servicio.ReglaIncumplida.Tipo.NO_ENCONTRADO;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import uy.echeveste.reservas.dominio.Cancha;
import uy.echeveste.reservas.dominio.Reserva;
import uy.echeveste.reservas.repositorio.CanchaRepositorio;
import uy.echeveste.reservas.repositorio.ReservaRepositorio;

@Service
public class ReservaServicio {

    public static final LocalTime APERTURA = LocalTime.of(8, 0);
    public static final LocalTime CIERRE = LocalTime.of(23, 0);
    public static final Duration MINIMO = Duration.ofHours(1);
    public static final Duration MAXIMO = Duration.ofHours(2);
    public static final Duration ANTICIPACION_CANCELACION = Duration.ofHours(24);
    public static final int DIAS_MAXIMOS_ADELANTE = 30;

    public record Franja(LocalDateTime inicio, LocalDateTime fin) {
    }

    private final CanchaRepositorio canchas;
    private final ReservaRepositorio reservas;
    private final Clock reloj;

    public ReservaServicio(CanchaRepositorio canchas, ReservaRepositorio reservas, Clock reloj) {
        this.canchas = canchas;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    @Transactional
    public Reserva reservar(long canchaId, String titular, String telefono, LocalDateTime inicio, Duration duracion) {
        LocalDateTime fin = inicio.plus(duracion);
        validarHorario(inicio, fin, duracion);
        Cancha cancha = canchas.bloquear(canchaId)
                .orElseThrow(() -> new ReglaIncumplida(NO_ENCONTRADO, "La cancha " + canchaId + " no existe"));
        if (reservas.existeSolapada(canchaId, inicio, fin)) {
            throw new ReglaIncumplida(CONFLICTO, "La cancha ya está reservada en ese horario");
        }
        var reserva = new Reserva(cancha, titular.strip(), telefono, inicio, fin, Tarifa.importe(cancha.getPrecioHora(), inicio, fin));
        return reservas.save(reserva);
    }

    @Transactional
    public Reserva cancelar(long reservaId) {
        Reserva reserva = buscar(reservaId);
        if (reserva.getEstado() == Reserva.Estado.CANCELADA) {
            throw new ReglaIncumplida(CONFLICTO, "La reserva ya estaba cancelada");
        }
        if (Duration.between(ahora(), reserva.getInicio()).compareTo(ANTICIPACION_CANCELACION) < 0) {
            throw new ReglaIncumplida(CONFLICTO, "Sólo se puede cancelar con 24 horas de anticipación");
        }
        reserva.cancelar();
        return reserva;
    }

    @Transactional(readOnly = true)
    public Reserva buscar(long reservaId) {
        return reservas.findById(reservaId)
                .orElseThrow(() -> new ReglaIncumplida(NO_ENCONTRADO, "La reserva " + reservaId + " no existe"));
    }

    /** Franjas libres de una hora, cada media hora, entre la apertura y el cierre. */
    @Transactional(readOnly = true)
    public List<Franja> disponibilidad(long canchaId, LocalDate fecha) {
        if (!canchas.existsById(canchaId)) {
            throw new ReglaIncumplida(NO_ENCONTRADO, "La cancha " + canchaId + " no existe");
        }
        List<Reserva> ocupadas = reservas.confirmadasEntre(canchaId, fecha.atTime(APERTURA), fecha.atTime(CIERRE));
        List<Franja> libres = new ArrayList<>();
        LocalDateTime limite = ahora();
        for (LocalDateTime inicio = fecha.atTime(APERTURA); !inicio.plus(MINIMO).isAfter(fecha.atTime(CIERRE)); inicio = inicio.plusMinutes(30)) {
            LocalDateTime fin = inicio.plus(MINIMO);
            LocalDateTime i = inicio;
            boolean ocupada = ocupadas.stream().anyMatch(r -> r.getInicio().isBefore(fin) && i.isBefore(r.getFin()));
            if (!ocupada && inicio.isAfter(limite)) {
                libres.add(new Franja(inicio, fin));
            }
        }
        return libres;
    }

    private void validarHorario(LocalDateTime inicio, LocalDateTime fin, Duration duracion) {
        if (inicio.getMinute() % 30 != 0 || inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new ReglaIncumplida(INVALIDA, "Las reservas empiezan en punto o y media");
        }
        if (duracion.compareTo(MINIMO) < 0 || duracion.compareTo(MAXIMO) > 0 || duracion.toMinutes() % 30 != 0) {
            throw new ReglaIncumplida(INVALIDA, "La duración debe ser de 60, 90 o 120 minutos");
        }
        if (inicio.toLocalTime().isBefore(APERTURA) || fin.toLocalTime().isAfter(CIERRE) || !fin.toLocalDate().equals(inicio.toLocalDate())) {
            throw new ReglaIncumplida(INVALIDA, "El complejo abre de 08:00 a 23:00");
        }
        LocalDateTime ahora = ahora();
        if (!inicio.isAfter(ahora)) {
            throw new ReglaIncumplida(INVALIDA, "No se puede reservar en el pasado");
        }
        if (inicio.toLocalDate().isAfter(ahora.toLocalDate().plusDays(DIAS_MAXIMOS_ADELANTE))) {
            throw new ReglaIncumplida(INVALIDA, "Se puede reservar con hasta 30 días de anticipación");
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(reloj);
    }
}
