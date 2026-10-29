package cv.perola;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class FlywayMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Deve conter o histórico de migrações V1, V2, V3 e V4 com sucesso no Flyway")
    void flywayMigrationHistoryIsComplete() {
        List<String> versions = jdbcTemplate.query(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank",
                (rs, rowNum) -> rs.getString("version")
        );

        assertThat(versions).contains("1", "2", "3", "4");
    }

    @Test
    @DisplayName("Deve conter todas as 16 tabelas de domínio criadas pelas migrações")
    void allRequiredTablesExist() {
        List<String> tables = jdbcTemplate.query(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                (rs, rowNum) -> rs.getString("table_name")
        );

        assertThat(tables).contains(
                "users",
                "user_roles",
                "refresh_tokens",
                "businesses",
                "services",
                "employees",
                "employee_services",
                "employee_monthly_schedules",
                "employee_schedules",
                "employee_schedule_days",
                "employee_schedule_breaks",
                "employee_time_off",
                "holidays",
                "appointments",
                "appointment_histories",
                "reviews"
        );
    }

    @Test
    @DisplayName("Deve conter os 12 feriados nacionais oficiais de Cabo Verde semeados pela migração V2")
    void capeVerdeHolidaysAreSeeded() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM holidays WHERE is_national = true",
                Integer.class
        );

        assertThat(count).isEqualTo(12);

        String anoNovo = jdbcTemplate.queryForObject(
                "SELECT name FROM holidays WHERE holiday_date = '2026-01-01'",
                String.class
        );
        assertThat(anoNovo).isEqualTo("Dia de Ano Novo");

        String independencia = jdbcTemplate.queryForObject(
                "SELECT name FROM holidays WHERE holiday_date = '2026-07-05'",
                String.class
        );
        assertThat(independencia).isEqualTo("Dia da Independência Nacional");
    }

    @Test
    @DisplayName("Deve validar a restrição GiST ex_appointments_no_overlap impedindo sobreposição e permitindo adjacentes")
    void antiOverbookingGistConstraintPreventsOverlappingAppointments() {
        UUID ownerId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID businessId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        try {
            // Setup dados base
            jdbcTemplate.update(
                    "INSERT INTO users (id, name, email, phone) VALUES (?, ?, ?, ?)",
                    ownerId, "Dono Teste", "owner-" + ownerId + "@test.cv", "+2389911111"
            );
            jdbcTemplate.update(
                    "INSERT INTO users (id, name, email, phone) VALUES (?, ?, ?, ?)",
                    customerId, "Cliente Teste", "customer-" + customerId + "@test.cv", "+2389922222"
            );
            jdbcTemplate.update(
                    "INSERT INTO businesses (id, owner_id, name, island, city, location, contact_phone) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    businessId, ownerId, "Salão Beleza", "Santiago", "Praia", "Plateau", "+2382611111"
            );
            jdbcTemplate.update(
                    "INSERT INTO services (id, business_id, name, duration_minutes, price) VALUES (?, ?, ?, ?, ?)",
                    serviceId, businessId, "Corte Cabelo", 60, 1000.00
            );
            jdbcTemplate.update(
                    "INSERT INTO employees (id, business_id, display_name) VALUES (?, ?, ?)",
                    employeeId, businessId, "Barbeiro João"
            );

            // 1. Inserir primeira marcação das 10:00 às 11:00 UTC (CONFIRMED)
            Instant t1000 = Instant.parse("2026-11-03T10:00:00Z");
            Instant t1100 = Instant.parse("2026-11-03T11:00:00Z");
            UUID appt1Id = UUID.randomUUID();

            jdbcTemplate.update(
                    "INSERT INTO appointments (id, business_id, customer_id, employee_id, service_id, created_by_id, start_time, end_time, service_name, service_price, service_duration, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED'::appointment_status)",
                    appt1Id, businessId, customerId, employeeId, serviceId, customerId,
                    Timestamp.from(t1000), Timestamp.from(t1100), "Corte Cabelo", 1000.00, 60
            );

            // 2. Tentar inserir marcação concorrente sobreposta das 10:30 às 11:30 UTC para o MESMO funcionário (deve falhar por GiST 23P01)
            Instant t1030 = Instant.parse("2026-11-03T10:30:00Z");
            Instant t1130 = Instant.parse("2026-11-03T11:30:00Z");
            UUID appt2Id = UUID.randomUUID();

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "INSERT INTO appointments (id, business_id, customer_id, employee_id, service_id, created_by_id, start_time, end_time, service_name, service_price, service_duration, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING'::appointment_status)",
                    appt2Id, businessId, customerId, employeeId, serviceId, customerId,
                    Timestamp.from(t1030), Timestamp.from(t1130), "Corte Cabelo", 1000.00, 60
            )).isInstanceOf(DataIntegrityViolationException.class)
              .hasMessageContaining("ex_appointments_no_overlap");

            // 3. Inserir marcação adjacente contígua das 11:00 às 12:00 UTC (intervalo semi-aberto [) permite adjacência exata)
            Instant t1200 = Instant.parse("2026-11-03T12:00:00Z");
            UUID appt3Id = UUID.randomUUID();

            int adjacentRows = jdbcTemplate.update(
                    "INSERT INTO appointments (id, business_id, customer_id, employee_id, service_id, created_by_id, start_time, end_time, service_name, service_price, service_duration, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED'::appointment_status)",
                    appt3Id, businessId, customerId, employeeId, serviceId, customerId,
                    Timestamp.from(t1100), Timestamp.from(t1200), "Corte Cabelo", 1000.00, 60
            );
            assertThat(adjacentRows).isEqualTo(1);

            // 4. Inserir marcação com status CANCELLED no mesmo horário (10:00 às 11:00 UTC) -> permitida pelo predicado da constraint
            UUID apptCancelledId = UUID.randomUUID();
            int cancelledRows = jdbcTemplate.update(
                    "INSERT INTO appointments (id, business_id, customer_id, employee_id, service_id, created_by_id, start_time, end_time, service_name, service_price, service_duration, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CANCELLED'::appointment_status)",
                    apptCancelledId, businessId, customerId, employeeId, serviceId, customerId,
                    Timestamp.from(t1000), Timestamp.from(t1100), "Corte Cabelo", 1000.00, 60
            );
            assertThat(cancelledRows).isEqualTo(1);

        } finally {
            // Limpeza
            jdbcTemplate.update("DELETE FROM appointments WHERE business_id = ?", businessId);
            jdbcTemplate.update("DELETE FROM employees WHERE id = ?", employeeId);
            jdbcTemplate.update("DELETE FROM services WHERE id = ?", serviceId);
            jdbcTemplate.update("DELETE FROM businesses WHERE id = ?", businessId);
            jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", ownerId, customerId);
        }
    }
}
