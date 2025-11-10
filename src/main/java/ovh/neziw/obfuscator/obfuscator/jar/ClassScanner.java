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
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import ovh.neziw.obfuscator.config.JsonConfig;
import ovh.neziw.obfuscator.obfuscator.ObfuscatorEngine;
import ovh.neziw.obfuscator.obfuscator.PatternMatcher;

/**
 * Scans classes in JAR files for name mapping generation
 * Single Responsibility: Scan classes and generate name mappings
 */
public class ClassScanner {

    private static final Logger LOGGER = Logger.getLogger(ClassScanner.class.getName());

    private final ObfuscatorEngine obfuscatorEngine;
    private final JsonConfig config;

    public ClassScanner(final ObfuscatorEngine obfuscatorEngine, final JsonConfig config) {
        this.obfuscatorEngine = obfuscatorEngine;
        this.config = config;
    }

    /**
     * Phase 1: Scans all classes in the JAR to generate class name mappings
     */
    public void scanClassesForNameMapping(final Path inputPath) throws IOException {
        try (final JarFile inputJar = new JarFile(inputPath.toFile())) {
            final Enumeration<JarEntry> entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String entryName = entry.getName();
                if (entryName.endsWith(".class")) {
                    final String internalClassName = entryName.substring(0, entryName.length() - 6);
                    if (this.obfuscatorEngine.shouldObfuscate(internalClassName)) {
                        final byte[] classBytes = JarReader.readEntry(inputJar, entry);
                        try {
                            final ClassReader reader = new ClassReader(classBytes);
                            final NameMappingVisitor visitor = new NameMappingVisitor();
                            reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        } catch (final Exception exception) {
                            LOGGER.warning("Failed to scan class " + entryName + " for name mapping: " + exception.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Phase 1: Scans all classes in the JAR to generate method name mappings
     */
    public void scanClassesForMethodNameMapping(final Path inputPath) throws IOException {
        try (final JarFile inputJar = new JarFile(inputPath.toFile())) {
            final Enumeration<JarEntry> entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                final String entryName = entry.getName();

                if (entryName.endsWith(".class")) {
                    final String internalClassName = entryName.substring(0, entryName.length() - 6);

                    if (this.obfuscatorEngine.shouldObfuscate(internalClassName)) {
                        final byte[] classBytes = JarReader.readEntry(inputJar, entry);
                        try {
                            final ClassReader reader = new ClassReader(classBytes);
                            final MethodNameMappingVisitor visitor = new MethodNameMappingVisitor(internalClassName);
                            reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        } catch (final Exception exception) {
                            LOGGER.warning("Failed to scan class " + entryName + " for method name mapping: " + exception.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Visitor that generates class name mappings without modifying the class
     */
    private class NameMappingVisitor extends ClassVisitor {
        private final PatternMatcher patternMatcher;
        private final ovh.neziw.obfuscator.obfuscator.NameGenerator nameGenerator;
        private final Map<String, String> classNameMap;
        private final boolean obfuscatePackages;
        private final boolean obfuscateClassNames;

        public NameMappingVisitor() {
            super(Opcodes.ASM9);
            this.patternMatcher = new PatternMatcher(ClassScanner.this.config.getInclude());
            this.nameGenerator = ClassScanner.this.obfuscatorEngine.getNameGenerator();
            this.classNameMap = ClassScanner.this.obfuscatorEngine.getClassNameMap();
            this.obfuscatePackages = ClassScanner.this.config.isObfuscatePackages();
            this.obfuscateClassNames = ClassScanner.this.config.isObfuscateClassNames();
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            if (this.patternMatcher.matches(name)) {
                if (!this.classNameMap.containsKey(name)) {
                    final String obfuscatedName = this.generateObfuscatedClassName(name);
                    this.classNameMap.put(name, obfuscatedName);
                }
                if (superName != null && this.patternMatcher.matches(superName)) {
                    if (!this.classNameMap.containsKey(superName)) {
                        this.classNameMap.put(superName, this.generateObfuscatedClassName(superName));
                    }
                }
                if (interfaces != null) {
                    for (final String iface : interfaces) {
                        if (this.patternMatcher.matches(iface) && !this.classNameMap.containsKey(iface)) {
                            this.classNameMap.put(iface, this.generateObfuscatedClassName(iface));
                        }
                    }
                }
            }
        }

        private String generateObfuscatedClassName(final String className) {
            if (this.obfuscatePackages && this.obfuscateClassNames) {
                final String packageName = PatternMatcher.getPackageName(className);
                final String simpleClassName = PatternMatcher.getSimpleClassName(className);

                if (!packageName.isEmpty()) {
                    final String[] packageParts = packageName.split("/");
                    final StringBuilder obfuscatedPackage = new StringBuilder();
                    for (final String part : packageParts) {
                        if (!obfuscatedPackage.isEmpty()) {
                            obfuscatedPackage.append("/");
                        }
                        obfuscatedPackage.append(this.nameGenerator.getObfuscatedName("pkg." + part));
                    }
                    final String obfuscatedSimpleName = this.nameGenerator.getObfuscatedName("cls." + simpleClassName);
                    return obfuscatedPackage + "/" + obfuscatedSimpleName;
                } else {
                    return this.nameGenerator.getObfuscatedName("cls." + simpleClassName);
                }
            } else if (this.obfuscatePackages) {
                final String packageName = PatternMatcher.getPackageName(className);
                final String simpleClassName = PatternMatcher.getSimpleClassName(className);

                if (!packageName.isEmpty()) {
                    final String[] packageParts = packageName.split("/");
                    final StringBuilder obfuscatedPackage = new StringBuilder();
                    for (final String part : packageParts) {
                        if (!obfuscatedPackage.isEmpty()) {
                            obfuscatedPackage.append("/");
                        }
                        obfuscatedPackage.append(this.nameGenerator.getObfuscatedName("pkg." + part));
                    }
                    return obfuscatedPackage + "/" + simpleClassName;
                } else {
                    return className;
                }
            } else if (this.obfuscateClassNames) {
                final String packageName = PatternMatcher.getPackageName(className);
                final String simpleClassName = PatternMatcher.getSimpleClassName(className);

                final String obfuscatedSimpleName = this.nameGenerator.getObfuscatedName("cls." + simpleClassName);
                if (!packageName.isEmpty()) {
                    return packageName + "/" + obfuscatedSimpleName;
                } else {
                    return obfuscatedSimpleName;
                }
            }
            return className;
        }
    }

    /**
     * Visitor that generates method name mappings without modifying the class
     */
    private class MethodNameMappingVisitor extends ClassVisitor {
        private final String className;
        private final ovh.neziw.obfuscator.obfuscator.NameGenerator nameGenerator;
        private final Map<String, Map<String, String>> globalMethodNameMap;
        private boolean isInterface;

        public MethodNameMappingVisitor(final String className) {
            super(Opcodes.ASM9);
            this.className = className;
            this.nameGenerator = ClassScanner.this.obfuscatorEngine.getNameGenerator();
            this.globalMethodNameMap = ClassScanner.this.obfuscatorEngine.getMethodNameMap();
        }

        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
            if (name.equals("<init>") || name.equals("<clinit>")) {
                return null;
            }
            if (this.nameGenerator.isObfuscated(name)) {
                return null;
            }
            if (this.isInterface) {
                return null;
            }
            return new MethodVisitor(Opcodes.ASM9) {
                private boolean hasOverride = false;

                @Override
                public org.objectweb.asm.AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
                    if ("Ljava/lang/Override;".equals(desc)) {
                        this.hasOverride = true;
                    }
                    return null;
                }

                @Override
                public void visitEnd() {
                    if (!this.hasOverride) {
                        final boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
                        final boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;

                        if (!isSynthetic && !isBridge) {
                            final String methodKey = name + descriptor;
                            final String key = MethodNameMappingVisitor.this.className + "." + name + descriptor;
                            final String obfuscatedName = MethodNameMappingVisitor.this.nameGenerator.getObfuscatedName(key);

                            MethodNameMappingVisitor.this.globalMethodNameMap.computeIfAbsent(MethodNameMappingVisitor.this.className, k -> new HashMap<>()).put(methodKey, obfuscatedName);
                        }
                    }
                }
            };
        }
    }
}
