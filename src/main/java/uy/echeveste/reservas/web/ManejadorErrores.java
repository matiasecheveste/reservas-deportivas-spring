/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas.web;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import uy.echeveste.reservas.servicio.ReglaIncumplida;

/**
 * Traduce los errores a respuestas RFC 9457 (application/problem+json). Al extender
 * ResponseEntityExceptionHandler, también los errores del framework (JSON mal formado,
 * parámetros de tipo incorrecto, método no admitido) salen con el mismo formato.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ReglaIncumplida.class)
    ProblemDetail regla(ReglaIncumplida e) {
        HttpStatus estado = switch (e.getTipo()) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CONFLICTO -> HttpStatus.CONFLICT;
            case INVALIDA -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        return ProblemDetail.forStatusAndDetail(estado, e.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders cabeceras, HttpStatusCode estado, WebRequest pedido) {
        Map<String, String> campos = new TreeMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }
}
