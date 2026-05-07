package mc.md5g22.redbug.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvHelper {

    public static HashMap<String, List<String>> extractColumns(Path csvPath) throws IOException {
        HashMap<String, List<String>> map = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath)) {

            // Read header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV is empty: " + csvPath);
            }

            String[] headers = headerLine.split(",", -1);
            int expectedColumns = headers.length;

            // Initialize map
            for (String h : headers) {
                map.put(h, new ArrayList<>());
            }

            // Read rows
            String line;
            int rowNumber = 1; // header is row 0

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                String[] parts = line.split(",", -1);

                // Arity check
                if (parts.length != expectedColumns) {
                    throw new IOException(
                            "CSV row " + rowNumber + " has incorrect number of columns: " +
                                    parts.length + " (expected " + expectedColumns + ")"
                    );
                }

                // Insert values
                for (int i = 0; i < expectedColumns; i++) {
                    map.get(headers[i]).add(parts[i]);
                }
            }
        }

        return map;
    }

}
