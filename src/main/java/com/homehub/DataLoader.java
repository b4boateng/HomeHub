package com.homehub;

import java.nio.file.Path;
import java.util.List;

// Reads a file and turns each row into a T.
public interface DataLoader<T> {
    List<T> load(Path path);
}
