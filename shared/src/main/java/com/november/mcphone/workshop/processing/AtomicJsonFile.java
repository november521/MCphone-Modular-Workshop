// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AtomicJsonFile {
    private AtomicJsonFile() {}
    public static void write(Path destination, String contents) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), ".workshop-", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            // If the filesystem cannot replace atomically, keep the old save and report the error.
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
