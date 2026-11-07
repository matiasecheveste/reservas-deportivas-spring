/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.servicio;

/** Violación de una regla de negocio. El código permite distinguir el caso en la API. */
public class ReglaIncumplida extends RuntimeException {

    public enum Tipo {
        NO_ENCONTRADO,
        CONFLICTO,
        INVALIDA
    }

    private final Tipo tipo;

    public ReglaIncumplida(Tipo tipo, String mensaje) {
        super(mensaje);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
