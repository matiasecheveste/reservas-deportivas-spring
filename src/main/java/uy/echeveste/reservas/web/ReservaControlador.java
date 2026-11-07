/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.web;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import uy.echeveste.reservas.dominio.Deporte;
import uy.echeveste.reservas.repositorio.CanchaRepositorio;
import uy.echeveste.reservas.servicio.ReservaServicio;
import uy.echeveste.reservas.servicio.ReservaServicio.Franja;
import uy.echeveste.reservas.web.Dtos.CanchaDto;
import uy.echeveste.reservas.web.Dtos.NuevaReserva;
import uy.echeveste.reservas.web.Dtos.ReservaDto;

@RestController
@RequestMapping("/api")
public class ReservaControlador {

    private final ReservaServicio servicio;
    private final CanchaRepositorio canchas;

    public ReservaControlador(ReservaServicio servicio, CanchaRepositorio canchas) {
        this.servicio = servicio;
        this.canchas = canchas;
    }

    @GetMapping("/canchas")
    public List<CanchaDto> canchas(@RequestParam(required = false) Deporte deporte) {
        var lista = deporte == null ? canchas.findAll() : canchas.findByDeporteOrderByNombre(deporte);
        return lista.stream().map(CanchaDto::de).toList();
    }

    @GetMapping("/canchas/{id}/disponibilidad")
    public List<Franja> disponibilidad(@PathVariable long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return servicio.disponibilidad(id, fecha);
    }

    @PostMapping("/reservas")
    @Transactional
    public ResponseEntity<ReservaDto> reservar(@Valid @RequestBody NuevaReserva pedido) {
        var reserva = servicio.reservar(pedido.canchaId(), pedido.titular(), pedido.telefono(), pedido.inicio(),
                Duration.ofMinutes(pedido.minutos()));
        return ResponseEntity.created(URI.create("/api/reservas/" + reserva.getId())).body(ReservaDto.de(reserva));
    }

    @GetMapping("/reservas/{id}")
    @Transactional(readOnly = true)
    public ReservaDto reserva(@PathVariable long id) {
        return ReservaDto.de(servicio.buscar(id));
    }

    @DeleteMapping("/reservas/{id}")
    @Transactional
    public ReservaDto cancelar(@PathVariable long id) {
        return ReservaDto.de(servicio.cancelar(id));
    }
}
