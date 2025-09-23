package com.example.motorreporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuoteDataLoaderCsvEncodingTest {

    @TempDir
    Path tempDir;

    @Test
    void readsUtf16LeEncodedFiles() throws IOException {
        Path csvFile = tempDir.resolve("utf16.csv");
        String content = String.join("\n",
                "Name,Status",
                "Alice,Success",
                "Bob,Failed"
        ) + "\n";

        byte[] bom = new byte[]{(byte) 0xFF, (byte) 0xFE};
        byte[] data = content.getBytes(StandardCharsets.UTF_16LE);
        byte[] combined = new byte[bom.length + data.length];
        System.arraycopy(bom, 0, combined, 0, bom.length);
        System.arraycopy(data, 0, combined, bom.length, data.length);
        Files.write(csvFile, combined);

        List<QuoteRecord> records = QuoteDataLoader.load(csvFile);

        assertEquals(2, records.size());
        assertEquals("Alice", records.get(0).getRawValues().get("Name"));
        assertEquals("Success", records.get(0).getRawValues().get("Status"));
        assertEquals("Bob", records.get(1).getRawValues().get("Name"));
        assertEquals("Failed", records.get(1).getRawValues().get("Status"));
    }
}
