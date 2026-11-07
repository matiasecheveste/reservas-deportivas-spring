/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.dominio;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "canchas")
public class Cancha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Deporte deporte;

    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    @Column(nullable = false)
    private boolean techada;

    protected Cancha() {
    }

    public Cancha(String nombre, Deporte deporte, BigDecimal precioHora, boolean techada) {
        this.nombre = nombre;
        this.deporte = deporte;
        this.precioHora = precioHora;
        this.techada = techada;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Deporte getDeporte() {
        return deporte;
    }

    public BigDecimal getPrecioHora() {
        return precioHora;
    }

    public boolean isTechada() {
        return techada;
    }
}
