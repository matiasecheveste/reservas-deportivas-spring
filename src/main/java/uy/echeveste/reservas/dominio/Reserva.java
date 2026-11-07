/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.dominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reservas")
public class Reserva {

    public enum Estado {
        CONFIRMADA,
        CANCELADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cancha_id")
    private Cancha cancha;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.CONFIRMADA;

    @Version
    private long version;

    protected Reserva() {
    }

    public Reserva(Cancha cancha, String titular, String telefono, LocalDateTime inicio, LocalDateTime fin, BigDecimal importe) {
        this.cancha = cancha;
        this.titular = titular;
        this.telefono = telefono;
        this.inicio = inicio;
        this.fin = fin;
        this.importe = importe;
    }

    public void cancelar() {
        this.estado = Estado.CANCELADA;
    }

    public Long getId() {
        return id;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public String getTitular() {
        return titular;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public Estado getEstado() {
        return estado;
    }
}
