package com.homehub.loaders;

import com.homehub.exceptions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Does the boring part once: read the file, skip the header, split each line into cells,
// and turn ANY problem into a DataLoadException that names the file and line.
// Subclasses only say how to turn one row (its cells) into an object.
public abstract class CsvLoader<T> implements DataLoader<T> {

    private final int columns;

    protected CsvLoader(int columns) {
        this.columns = columns;
    }

    protected abstract T parseRow(String[] cells);

    @Override
    public List<T> load(Path path) {
        String file = path.getFileName().toString();

        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException e) {
            throw new DataLoadException(file, 0, "cannot read file");
        }

        List<T> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {      // index 0 is the header row
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            int lineNumber = i + 1;                    // humans count from 1, and the header is line 1

            String[] cells = line.split(",", -1);      // -1 keeps empty cells at the end
            if (cells.length != columns) {
                throw new DataLoadException(file, lineNumber,
                        "expected " + columns + " columns but found " + cells.length);
            }
            for (int c = 0; c < cells.length; c++) {
                cells[c] = cells[c].trim();
            }

            try {
                result.add(parseRow(cells));
            } catch (RuntimeException e) {             // bad number, unknown enum, bad time...
                throw new DataLoadException(file, lineNumber, e.getMessage());
            }
        }
        return result;
    }
}
