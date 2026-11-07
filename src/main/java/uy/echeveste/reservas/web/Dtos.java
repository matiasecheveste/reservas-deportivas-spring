/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.echeveste.reservas.dominio.Cancha;
import uy.echeveste.reservas.dominio.Deporte;
import uy.echeveste.reservas.dominio.Reserva;

/** Objetos de entrada y salida de la API: las entidades JPA nunca se exponen directamente. */
public final class Dtos {

    private Dtos() {
    }

    public record NuevaReserva(
            @NotNull Long canchaId,
            @NotBlank @Size(max = 80) String titular,
            // Celular uruguayo: 09 seguido de 7 dígitos.
            @NotBlank @Pattern(regexp = "09\\d{7}", message = "debe ser un celular uruguayo (09XXXXXXX)") String telefono,
            @NotNull LocalDateTime inicio,
            @NotNull @Min(60) @Max(120) Integer minutos) {
    }

    public record CanchaDto(long id, String nombre, Deporte deporte, BigDecimal precioHora, boolean techada) {
        static CanchaDto de(Cancha c) {
            return new CanchaDto(c.getId(), c.getNombre(), c.getDeporte(), c.getPrecioHora(), c.isTechada());
        }
    }

    public record ReservaDto(long id, long canchaId, String cancha, String titular, LocalDateTime inicio,
            LocalDateTime fin, BigDecimal importe, Reserva.Estado estado) {
        static ReservaDto de(Reserva r) {
            return new ReservaDto(r.getId(), r.getCancha().getId(), r.getCancha().getNombre(), r.getTitular(),
                    r.getInicio(), r.getFin(), r.getImporte(), r.getEstado());
        }
    }
}
