package br.com.iforce.praxis.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationVersionUniquenessTest {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^V([^_]+)__.+\\.sql$");

    @Test
    void shouldNotContainDuplicateMigrationVersions() throws IOException {
        Path migrationDirectory = Paths.get("src", "main", "resources", "db", "migration");

        Map<String, List<String>> filesByVersion;
        try (Stream<Path> paths = Files.list(migrationDirectory)) {
            filesByVersion = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .map(FlywayMigrationVersionUniquenessTest::versionedFile)
                    .filter(file -> file != null)
                    .collect(Collectors.groupingBy(
                            VersionedFile::version,
                            Collectors.mapping(VersionedFile::fileName, Collectors.toList())
                    ));
        }

        Map<String, List<String>> duplicates = filesByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(duplicates)
                .as("Cada versão Flyway deve existir em apenas um arquivo")
                .isEmpty();
    }

    private static VersionedFile versionedFile(String fileName) {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }
        return new VersionedFile(matcher.group(1), fileName);
    }

    private record VersionedFile(String version, String fileName) {
    }
}
