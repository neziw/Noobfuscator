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

import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * A ClassWriter that safely handles obfuscated class names and external classes
 * during frame computation. It overrides getCommonSuperClass to:
 * 1. Map obfuscated class names back to original names when needed
 * 2. Handle external classes (like Minecraft/Bukkit) that aren't in the JAR
 */
public class SafeClassWriter extends ClassWriter {

    private final Map<String, String> classNameMap; // original -> obfuscated
    private final Map<String, String> reverseClassNameMap; // obfuscated -> original

    public SafeClassWriter(final ClassReader classReader, final int flags,
                           final Map<String, String> classNameMap) {
        super(classReader, flags);
        this.classNameMap = classNameMap != null ? classNameMap : new java.util.HashMap<>();
        this.reverseClassNameMap = new java.util.HashMap<>();
        if (classNameMap != null) {
            for (final Map.Entry<String, String> entry : classNameMap.entrySet()) {
                this.reverseClassNameMap.put(entry.getValue(), entry.getKey());
            }
        }
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        final String class1 = type1.replace('/', '.');
        final String class2 = type2.replace('/', '.');
        if (this.isExternalClass(class1) || this.isExternalClass(class2)) {
            return "java/lang/Object";
        }
        final boolean isType1Obfuscated = this.reverseClassNameMap.containsKey(type1);
        final boolean isType2Obfuscated = this.reverseClassNameMap.containsKey(type2);
        final String originalClass1 = this.getOriginalClassName(type1);
        final String originalClass2 = this.getOriginalClassName(type2);
        final boolean isJavaStdlib1 = class1.startsWith("java.") || class1.startsWith("javax.");
        final boolean isJavaStdlib2 = class2.startsWith("java.") || class2.startsWith("javax.");
        if (isType1Obfuscated || isType2Obfuscated || !isJavaStdlib1 || !isJavaStdlib2) {
            return "java/lang/Object";
        }
        try {
            Class<?> c1 = this.getClass(originalClass1);
            final Class<?> c2 = this.getClass(originalClass2);
            if (c1.isAssignableFrom(c2)) {
                return type1;
            } else if (c2.isAssignableFrom(c1)) {
                return type2;
            } else if (c1.isInterface() || c2.isInterface()) {
                return "java/lang/Object";
            } else {
                do {
                    c1 = c1.getSuperclass();
                    if (c1 == null) {
                        return "java/lang/Object";
                    }
                } while (!c1.isAssignableFrom(c2));
                return c1.getName().replace('.', '/');
            }
        } catch (final Throwable throwable) {
            return "java/lang/Object";
        }
    }

    /**
     * Gets the original class name if the given name is obfuscated
     * className is in internal format (with /)
     * Returns binary format (with .)
     */
    private String getOriginalClassName(final String className) {
        // Check if this is an obfuscated name (look in reverse map)
        if (this.reverseClassNameMap != null && this.reverseClassNameMap.containsKey(className)) {
            final String original = this.reverseClassNameMap.get(className);
            return original.replace('/', '.');
        }
        // Not obfuscated, just convert internal to binary format
        return className.replace('/', '.');
    }

    /**
     * Checks if a class is an external class (not in the JAR being obfuscated)
     */
    private boolean isExternalClass(final String className) {
        return false; // TODO
    }

    /**
     * Safely loads a class, catching ClassNotFoundException
     * Returns the class if found, throws ClassNotFoundException if not
     */
    private Class<?> getClass(final String className) throws ClassNotFoundException {
        try {
            // Try system class loader first
            return Class.forName(className, false, ClassLoader.getSystemClassLoader());
        } catch (final ClassNotFoundException exception) {
            // If not found, try with current thread's context class loader
            try {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl != null) {
                    return cl.loadClass(className);
                }
            } catch (final ClassNotFoundException ignored) {
                // Ignore and rethrow original
            }
            // If still not found, try with this class's class loader
            try {
                final ClassLoader cl = this.getClass().getClassLoader();
                if (cl != null) {
                    return cl.loadClass(className);
                }
            } catch (final ClassNotFoundException ignored) {
                // Ignore and rethrow original
            }
            throw exception;
        }
    }
}
