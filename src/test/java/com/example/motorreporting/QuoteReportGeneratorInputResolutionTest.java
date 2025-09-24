package com.example.motorreporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuoteReportGeneratorInputResolutionTest {

    @TempDir
    Path tempDir;

    @Test
    void includesCandidateFileNamesWhenMultipleOptionsExist() throws IOException {
        Path sourceDataDirectory = Files.createDirectories(tempDir.resolve("source data"));
        Files.createFile(sourceDataDirectory.resolve("alpha.csv"));
        Files.createFile(sourceDataDirectory.resolve("beta.xlsx"));

        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> QuoteReportGenerator.resolveInputPath(null));
            String message = exception.getMessage();
            assertTrue(message.contains("alpha.csv"));
            assertTrue(message.contains("beta.xlsx"));
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}

