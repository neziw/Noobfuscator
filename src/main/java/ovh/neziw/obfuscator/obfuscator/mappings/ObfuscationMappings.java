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
package ovh.neziw.obfuscator.obfuscator.mappings;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Manages all obfuscation mappings (class names, method names, fields, local variables)
 * Single Responsibility: Store and provide access to all obfuscation mappings
 */
@Getter
public class ObfuscationMappings {

    // Shared map for class name obfuscation across all classes
    private final Map<String, String> classNameMap = new HashMap<>();

    // Shared map for method name obfuscation across all classes
    // Map: className -> (methodKey -> obfuscatedName)
    private final Map<String, Map<String, String>> methodNameMap = new HashMap<>();

    // Shared map for field name obfuscation across all classes
    // Map: className -> (fieldName -> obfuscatedName)
    private final Map<String, Map<String, String>> fieldMappings = new HashMap<>();

    // Shared map for local variable name obfuscation across all classes
    // Map: className -> (methodKey -> (varName -> obfuscatedName))
    private final Map<String, Map<String, Map<String, String>>> localVariableMappings = new HashMap<>();

}

