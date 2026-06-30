package org.example.padelback;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PadelBackApplication {

    public static void main(String[] args) {
        // JVM pineado a UTC para que los temporales sin zona (LocalTime/LocalDateTime de horarios y
        // reservas) y los Instant de auditoría hagan round-trip determinístico, independiente de la
        // zona del server. Combinado con hibernate.jdbc.time_zone=UTC el offset es cero.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(PadelBackApplication.class, args);
    }

}
