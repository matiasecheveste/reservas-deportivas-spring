/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.repositorio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import uy.echeveste.reservas.dominio.Reserva;

public interface ReservaRepositorio extends JpaRepository<Reserva, Long> {

    /** Dos intervalos [a, b) y [c, d) se solapan si a < d y c < b. */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.cancha.id = :canchaId
              and r.estado = uy.echeveste.reservas.dominio.Reserva.Estado.CONFIRMADA
              and r.inicio < :fin and :inicio < r.fin
            """)
    boolean existeSolapada(Long canchaId, LocalDateTime inicio, LocalDateTime fin);

    @Query("""
            select r from Reserva r
            where r.cancha.id = :canchaId
              and r.estado = uy.echeveste.reservas.dominio.Reserva.Estado.CONFIRMADA
              and r.inicio < :hasta and r.fin > :desde
            order by r.inicio
            """)
    List<Reserva> confirmadasEntre(Long canchaId, LocalDateTime desde, LocalDateTime hasta);
}
