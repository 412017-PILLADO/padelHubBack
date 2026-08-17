package org.example.padelback.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * El club de demostración no puede llegar a una base productiva.
 *
 * <p>La {@code V2} siembra un complejo de fixture ("Padel Hub Demo", Av. Siempre Viva 742, tres
 * canchas) y registra {@code localhost} como uno de sus dominios. En prod eso es un club falso
 * alcanzable desde afuera por el header {@code X-Tenant} — y además el {@code OwnerSeeder} le
 * engancharía una cuenta de dueño, porque busca el tenant cuyo dominio es {@code localhost}, que es
 * exactamente el que crea esa migración.
 *
 * <p>La separación es por ubicación de Flyway: {@code db/migration} es el esquema y va a todos lados;
 * {@code db/seed} son los datos de demo y sólo lo incluye el default de {@code application.yml}
 * (dev y tests, que reservan contra ese club). {@code application-prod.yml} deja únicamente el
 * esquema.
 *
 * <p>Estas dos puertas cubren las dos formas de romperlo, que son distintas: mover el seed de vuelta
 * al esquema, y borrar el override del perfil de prod.
 */
class SeedFueraDeProduccionTest {

    private static final Path MIGRATION = Path.of("src/main/resources/db/migration");
    private static final Path SEED = Path.of("src/main/resources/db/seed");
    private static final Path PROD_YML = Path.of("src/main/resources/application-prod.yml");

    /**
     * El invariante de fondo, y por eso mira el CONTENIDO y no el nombre de los archivos: la carpeta
     * del esquema no puede insertar clubes. Que hoy la V2 esté mudada es una forma de cumplirlo;
     * agregar mañana un {@code INSERT INTO tenants} en una migración nueva lo rompería igual, y esta
     * puerta lo agarra.
     */
    @Test
    void elEsquemaNoInsertaNingunClub() throws IOException {
        try (Stream<Path> archivos = Files.list(MIGRATION)) {
            List<String> conInsert = archivos
                    .filter(p -> p.toString().endsWith(".sql"))
                    .filter(SeedFueraDeProduccionTest::insertaTenantOComplejo)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();

            assertThat(conInsert)
                    .as("db/migration es el esquema: los datos de club van en db/seed, que prod no carga")
                    .isEmpty();
        }
    }

    /** Y el seed tiene que seguir existiendo: si desaparece, dev y los e2e se quedan sin club. */
    @Test
    void elSeedDeDemoVivioEnSuPropiaCarpeta() throws IOException {
        try (Stream<Path> archivos = Files.list(SEED)) {
            assertThat(archivos.map(p -> p.getFileName().toString()))
                    .as("el seed de demo desapareció: dev y los tests reservan contra ese club")
                    .anyMatch(n -> n.endsWith(".sql") && insertaTenantOComplejo(SEED.resolve(n)));
        }
    }

    @Test
    void elPerfilDeProdNoCargaElSeed() throws IOException {
        String prod = Files.readString(PROD_YML, StandardCharsets.UTF_8);
        String sinComentarios = prod.replaceAll("(?m)^\\s*#.*$", "");

        assertThat(sinComentarios)
                .as("application-prod.yml dejó de fijar las locations de Flyway: heredaría el default, "
                        + "que incluye el seed de demo")
                .contains("locations:")
                .contains("classpath:db/migration");
        assertThat(sinComentarios)
                .as("el perfil de prod volvió a cargar db/seed: el club de demostración se instalaría "
                        + "en la base del cliente")
                .doesNotContain("db/seed");
    }

    /** Lee el SQL sin comentarios: la prosa de estas migraciones nombra tablas todo el tiempo. */
    private static boolean insertaTenantOComplejo(Path sql) {
        try {
            String cuerpo = Files.readString(sql, StandardCharsets.UTF_8)
                    .replaceAll("(?m)^\\s*--.*$", "")
                    .toLowerCase();
            return cuerpo.matches("(?s).*insert\\s+into\\s+(tenants|complejos|canchas|tenant_dominios).*");
        } catch (IOException e) {
            throw new IllegalStateException("no se pudo leer " + sql, e);
        }
    }
}
