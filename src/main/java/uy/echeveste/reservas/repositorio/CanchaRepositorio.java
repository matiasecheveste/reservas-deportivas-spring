/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import uy.echeveste.reservas.dominio.Cancha;
import uy.echeveste.reservas.dominio.Deporte;

public interface CanchaRepositorio extends JpaRepository<Cancha, Long> {

    List<Cancha> findByDeporteOrderByNombre(Deporte deporte);

    /**
     * Bloquea la fila de la cancha durante la transacción: dos reservas simultáneas
     * sobre la misma cancha se serializan y la segunda ve la primera al validar solapamientos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cancha c where c.id = :id")
    Optional<Cancha> bloquear(Long id);
}
