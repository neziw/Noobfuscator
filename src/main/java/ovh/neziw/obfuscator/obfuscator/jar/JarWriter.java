/*
 * This file is part of "Noobfuscator", licensed under MIT License.
 *
 *  Copyright (c) 2025 neziw
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package ovh.neziw.obfuscator.obfuscator.jar;

import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Writes entries to JAR files
 * Single Responsibility: Write JAR entries with proper metadata
 */
public final class JarWriter {

    /**
     * Writes an entry to the JAR output stream
     */
    public static void writeEntry(final JarOutputStream jos, final JarEntry entry, final byte[] data) throws IOException {
        jos.putNextEntry(entry);
        jos.write(data);
        jos.closeEntry();
    }

    /**
     * Creates a new JAR entry with the given name and timestamp
     */
    public static JarEntry createEntry(final String entryName, final long timestamp) {
        final JarEntry entry = new JarEntry(entryName);
        entry.setTime(timestamp);
        return entry;
    }
}

