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
package ovh.neziw.obfuscator.obfuscator;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates obfuscated names (a0, a1, b0, b1, z2, etc.)
 * Optionally supports a watermark prefix (e.g., "LPX_a0", "LPX_b1")
 */
// TODO: enhance to support more complex naming schemes if needed
public class NameGenerator {

    // Characters to use for obfuscated names (lowercase letters + digits)
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private final Map<String, String> nameMap;
    private final String watermark;
    private int letterIndex;
    private int digitIndex;

    public NameGenerator(final String watermark) {
        this.nameMap = new HashMap<>();
        this.letterIndex = 0;
        this.digitIndex = 0;
        this.watermark = watermark != null ? watermark : "";
    }

    /**
     * Generates a new obfuscated name
     * Format: watermark + letter + digit (e.g., LPX_a0, LPX_a1, LPX_b0, LPX_b5, LPX_z2)
     * Uses sequential generation: a0, a1, ..., a9, b0, b1, ...
     * If watermark is empty, returns just letter + digit (e.g., a0, a1)
     */
    public String generateName() {
        final char letter = CHARS.charAt(this.letterIndex % CHARS.length());
        final char digit = DIGITS.charAt(this.digitIndex % DIGITS.length());
        final String name = String.valueOf(letter) + digit;
        this.digitIndex++;
        if (this.digitIndex >= DIGITS.length()) {
            this.digitIndex = 0;
            this.letterIndex++;
        }
        return this.watermark + name;
    }

    /**
     * Gets or generates an obfuscated name for a given original name
     */
    public String getObfuscatedName(final String originalName) {
        return this.nameMap.computeIfAbsent(originalName, k -> this.generateName());
    }

    /**
     * Clears the name map and resets counters
     */
    public void clear() {
        this.nameMap.clear();
        this.letterIndex = 0;
        this.digitIndex = 0;
    }

    /**
     * Checks if a name is already obfuscated
     */
    public boolean isObfuscated(String name) {
        if (name == null) {
            return false;
        }
        if (!this.watermark.isEmpty()) {
            if (!name.startsWith(this.watermark)) {
                return false;
            }
            name = name.substring(this.watermark.length());
        }
        if (name.length() != 2) {
            return false;
        }
        final char first = name.charAt(0);
        final char second = name.charAt(1);
        return CHARS.indexOf(first) >= 0 && DIGITS.indexOf(second) >= 0;
    }
}
