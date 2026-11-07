/*
 * Reservas deportivas — Matías Nicolás Echeveste Cardozo
 */
package uy.echeveste.reservas;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import uy.echeveste.reservas.repositorio.ReservaRepositorio;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojDePrueba.class)
class ReservaControladorTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ReservaRepositorio reservas;

    @BeforeEach
    void limpiar() {
        reservas.deleteAll();
    }

    private static String pedido(String telefono, String inicio, int minutos) {
        return """
                {"canchaId": 3, "titular": "Lucía Pereira", "telefono": "%s", "inicio": "%s", "minutos": %d}
                """.formatted(telefono, inicio, minutos);
    }

    @Test
    void listaLasCanchasCargadasPorFlywayYFiltraPorDeporte() throws Exception {
        mvc.perform(get("/api/canchas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)));
        mvc.perform(get("/api/canchas").param("deporte", "PADEL"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nombre").value("Pádel — Cancha 1"));
    }

    @Test
    void creaConsultaYCancelaUnaReserva() throws Exception {
        String ubicacion = mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(pedido("099123456", "2026-12-03T10:00:00", 60)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/reservas/")))
                .andExpect(jsonPath("$.importe").value(1600.00))
                .andExpect(jsonPath("$.fin").value("2026-12-03T11:00:00"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(ubicacion)).andExpect(jsonPath("$.estado").value("CONFIRMADA"));
        mvc.perform(delete(ubicacion)).andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void devuelveProblemDetailAnteErrores() throws Exception {
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(pedido("12345", "2026-12-03T10:00:00", 45)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.campos.telefono").value("debe ser un celular uruguayo (09XXXXXXX)"))
                .andExpect(jsonPath("$.campos.minutos").exists());

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(pedido("099123456", "2026-12-03T10:00:00", 60)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(pedido("099123456", "2026-12-03T10:30:00", 60)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La cancha ya está reservada en ese horario"));

        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(pedido("099123456", "2026-11-30T10:00:00", 60)))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(get("/api/reservas/123456")).andExpect(status().isNotFound());
    }

    @Test
    void publicaDisponibilidadSaludYDocumentacion() throws Exception {
        mvc.perform(get("/api/canchas/1/disponibilidad").param("fecha", "2026-12-03"))
                .andExpect(jsonPath("$", hasSize(29)))
                .andExpect(jsonPath("$[0].inicio").value("2026-12-03T08:00:00"));
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/reservas']").exists());
    }
}
