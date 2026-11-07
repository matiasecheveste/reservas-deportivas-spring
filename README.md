# Reservas deportivas — Spring Boot

API REST para reservar canchas de fútbol 5, pádel y tenis de un complejo deportivo. Hecha con **Java 21 y Spring Boot 4**, usa JPA/Hibernate, migraciones Flyway, Bean Validation, errores RFC 9457 y documentación OpenAPI, y la cubren pruebas JUnit 5 que también corren contra PostgreSQL.

## Reglas de negocio

- El complejo abre de 08:00 a 23:00.
- Las reservas empiezan en punto o y media, y duran 60, 90 o 120 minutos.
- Se puede reservar con hasta 30 días de anticipación. No se aceptan reservas en el pasado.
- Una cancha no admite reservas solapadas; los turnos contiguos (10:00–11:00 y 11:00–12:00) sí se permiten.
- El importe se calcula por bloques de 30 minutos, con un **recargo del 20 %** en horario pico: de lunes a viernes desde las 18:00, y sábados y domingos todo el día (`Tarifa.java`).
- La cancelación sólo se acepta con 24 horas de anticipación, y libera el horario.

## Decisiones técnicas

- **Concurrencia**: al reservar se bloquea la fila de la cancha (`PESSIMISTIC_WRITE`). Así dos pedidos simultáneos se serializan y el segundo ve el primero al validar el solapamiento. Una prueba lanza 6 hilos sobre el mismo horario y verifica que se confirma una sola reserva.
- **Reloj inyectable**: el servicio recibe un `Clock`. Las pruebas fijan "ahora" en una fecha conocida y no dependen del día en que se ejecutan.
- **DTO con `record`**: las entidades JPA no se exponen en la API, y `open-in-view` está desactivado.
- **Flyway + `ddl-auto: validate`**: el esquema lo definen las migraciones, y Hibernate sólo verifica que coincida con las entidades.
- **H2 en modo PostgreSQL** para desarrollo y pruebas rápidas. En CI, las mismas pruebas se repiten contra PostgreSQL 17.

## API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/canchas?deporte=PADEL` | canchas, opcionalmente filtradas por deporte |
| GET | `/api/canchas/{id}/disponibilidad?fecha=2026-12-03` | franjas libres de una hora |
| POST | `/api/reservas` | crea una reserva (`201` + `Location`) |
| GET | `/api/reservas/{id}` | consulta una reserva |
| DELETE | `/api/reservas/{id}` | cancela una reserva |
| GET | `/docs` | Swagger UI (OpenAPI en `/v3/api-docs`) |
| GET | `/actuator/health` | estado del servicio |

```json
POST /api/reservas
{ "canchaId": 3, "titular": "Lucía Pereira", "telefono": "099123456", "inicio": "2026-12-03T19:00:00", "minutos": 90 }
```

Los errores devuelven `application/problem+json`:

| Código | Caso |
|---|---|
| 400 | datos inválidos, con el detalle de cada campo |
| 404 | la cancha o la reserva no existe |
| 409 | horario ocupado, o cancelación tardía |
| 422 | horario fuera de las reglas |

## Uso

```bash
./mvnw spring-boot:run          # H2 en memoria, http://localhost:8080/docs
./mvnw verify                   # 14 pruebas
docker compose up --build       # API + PostgreSQL 17
```

## Pruebas

- `TarifaTest`: pruebas parametrizadas del cálculo de importes.
- `ReservaServicioTest`: reglas de negocio, solapamientos, cancelación, disponibilidad y concurrencia con hilos.
- `ReservaControladorTest`: HTTP con MockMvc. Cubre los códigos de estado, ProblemDetail, las migraciones aplicadas, Actuator y OpenAPI.

## Autor

Matías Nicolás Echeveste Cardozo — licencia MIT.
