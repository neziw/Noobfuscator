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
package ovh.neziw.obfuscator.obfuscator.method;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.tree.MethodNode;
import ovh.neziw.obfuscator.obfuscator.NameGenerator;

/**
 * Maps original method names to obfuscated names
 * Single Responsibility: Generate and store method name mappings
 */
public class MethodNameMapper {

    private final NameGenerator nameGenerator;
    private final String originalClassName;
    private final Map<String, String> localMethodNameMap = new HashMap<>();
    private final Map<String, Map<String, String>> globalMethodNameMap;

    public MethodNameMapper(final NameGenerator nameGenerator, final String originalClassName,
                            final Map<String, Map<String, String>> globalMethodNameMap) {
        this.nameGenerator = nameGenerator;
        this.originalClassName = originalClassName;
        this.globalMethodNameMap = globalMethodNameMap;
    }

    /**
     * Generates an obfuscated method name for a method
     */
    public String generateObfuscatedMethodName(final String originalName, final String descriptor) {
        final String key = this.originalClassName + "." + originalName + descriptor;
        return this.nameGenerator.getObfuscatedName(key);
    }

    /**
     * Maps a method and stores the mapping
     */
    public void mapMethod(final MethodNode method, final String obfuscatedName) {
        final String methodKey = method.name + method.desc;
        this.localMethodNameMap.put(methodKey, obfuscatedName);
        this.globalMethodNameMap.computeIfAbsent(this.originalClassName, k -> new HashMap<>())
            .put(methodKey, obfuscatedName);
    }

    /**
     * Checks if a method key is mapped
     */
    public boolean isMapped(final String methodKey) {
        return this.localMethodNameMap.containsKey(methodKey);
    }

    /**
     * Applies obfuscated names to methods in the class node
     */
    public void applyMappingsToMethods(final java.util.List<MethodNode> methods) {
        if (methods == null) {
            return;
        }
        for (final MethodNode method : methods) {
            final String originalName = method.name;
            final String methodKey = originalName + method.desc;
            if (this.localMethodNameMap.containsKey(methodKey)) {
                method.name = this.localMethodNameMap.get(methodKey);
            }
        }
    }
}

