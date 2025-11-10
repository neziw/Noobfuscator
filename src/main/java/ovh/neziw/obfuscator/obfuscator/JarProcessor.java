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

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;
import ovh.neziw.obfuscator.config.JsonConfig;
import ovh.neziw.obfuscator.obfuscator.jar.ClassScanner;
import ovh.neziw.obfuscator.obfuscator.jar.JarReader;
import ovh.neziw.obfuscator.obfuscator.jar.JarWriter;
import ovh.neziw.obfuscator.obfuscator.jar.ManifestHandler;
import ovh.neziw.obfuscator.obfuscator.stats.ObfuscationStatsCollector;

/**
 * Processes JAR files for obfuscation
 */
public class JarProcessor {

    private static final Logger LOGGER = Logger.getLogger(JarProcessor.class.getName());

    private final ObfuscatorEngine obfuscatorEngine;
    private final JsonConfig config;
    private final ClassScanner classScanner;

    public JarProcessor(final JsonConfig config) {
        this.config = config;
        this.obfuscatorEngine = new ObfuscatorEngine(config);
        this.classScanner = new ClassScanner(this.obfuscatorEngine, config);
    }

    /**
     * Processes the input JAR file and creates an obfuscated output JAR
     *
     * @param inputJarPath  Path to input JAR file
     * @param outputJarPath Path to output JAR file
     * @throws IOException If an I/O error occurs
     */
    public void processJar(final String inputJarPath, final String outputJarPath) throws IOException {
        this.obfuscatorEngine.getNameGenerator().clear();
        final Path inputPath = Paths.get(inputJarPath);
        final Path outputPath = Paths.get(outputJarPath);
        final Path outputDir = outputPath.getParent();
        if (outputDir != null && !Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        if (this.config.isObfuscateClassNames() || this.config.isObfuscatePackages()) {
            this.classScanner.scanClassesForNameMapping(inputPath);
        }
        if (this.config.isObfuscateMethodNames()) {
            this.classScanner.scanClassesForMethodNameMapping(inputPath);
        }

        try (final JarFile inputJar = new JarFile(inputPath.toFile());
             final FileOutputStream fos = new FileOutputStream(outputPath.toFile());
             final JarOutputStream jos = new JarOutputStream(fos)) {
            ManifestHandler.copyManifest(inputJar, jos);
            final Enumeration<JarEntry> entries = inputJar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.equals("META-INF/MANIFEST.MF")) {
                    continue;
                }
                byte[] entryData = JarReader.readEntry(inputJar, entry);
                if (entryName.endsWith(".class")) {
                    final String internalClassName = entryName.substring(0, entryName.length() - 6);
                    final ovh.neziw.obfuscator.obfuscator.stats.ObfuscationStatsCollector engineStats = this.obfuscatorEngine.getStats();
                    engineStats.incrementClassesProcessed();
                    try {
                        if (this.obfuscatorEngine.shouldObfuscate(internalClassName)) {
                            entryData = this.obfuscatorEngine.obfuscateClass(entryData, internalClassName);
                            engineStats.incrementClassesObfuscated();
                            if (this.config.isObfuscateClassNames() || this.config.isObfuscatePackages()) {
                                final String obfuscatedClassName = this.obfuscatorEngine.getObfuscatedClassName(internalClassName);
                                if (!obfuscatedClassName.equals(internalClassName)) {
                                    entryName = obfuscatedClassName + ".class";
                                }
                            }
                        }
                    } catch (final Exception exception) {
                        final String errorMsg = exception.getMessage();
                        if (errorMsg != null && errorMsg.contains("Flow obfuscation skipped")) {
                            LOGGER.warning(errorMsg + " for class " + entryName +
                                ". Attempting obfuscation without flow obfuscation.");
                            try {
                                final String originalFlow = this.config.getFlowObfuscation();
                                this.config.setFlowObfuscation("NONE");
                                final byte[] obfuscatedData = this.obfuscatorEngine.obfuscateClass(entryData, internalClassName);
                                this.config.setFlowObfuscation(originalFlow);
                                entryData = obfuscatedData;
                                engineStats.incrementClassesObfuscated();
                                if (this.config.isObfuscateClassNames() || this.config.isObfuscatePackages()) {
                                    final String obfuscatedClassName = this.obfuscatorEngine.getObfuscatedClassName(internalClassName);
                                    if (!obfuscatedClassName.equals(internalClassName)) {
                                        entryName = obfuscatedClassName + ".class";
                                    }
                                }
                            } catch (final Exception exception2) {
                                LOGGER.warning("Failed to obfuscate class " + entryName +
                                    " even without flow obfuscation: " + exception2.getMessage());
                            }
                        } else {
                            LOGGER.warning("Failed to obfuscate class " + entryName + ": " + exception.getMessage());
                            LOGGER.throwing(JarProcessor.class.getName(), "processJar", exception);
                        }
                    }
                }
                final JarEntry outputEntry = JarWriter.createEntry(entryName, entry.getTime());
                JarWriter.writeEntry(jos, outputEntry, entryData);
            }
            if (this.config.isCrashClass()) {
                final byte[] crashClassBytes = CrashClassGenerator.generateCrashClass();
                final JarEntry crashClassEntry = JarWriter.createEntry(CrashClassGenerator.getCrashClassName(), System.currentTimeMillis());
                JarWriter.writeEntry(jos, crashClassEntry, crashClassBytes);
            }
        }
        if (this.config.isGenerateMappings()) {
            try {
                final MappingsGenerator mappingsGenerator = new MappingsGenerator();
                mappingsGenerator.generateMappings(
                    this.obfuscatorEngine.getClassNameMap(),
                    this.obfuscatorEngine.getMethodNameMap(),
                    this.obfuscatorEngine.getFieldMappings(),
                    this.obfuscatorEngine.getLocalVariableMappings(),
                    outputJarPath
                );
                LOGGER.info("Mappings file generated: " + outputJarPath.replace(".jar", "_mappings.json"));
            } catch (final IOException exception) {
                LOGGER.warning("Failed to generate mappings file: " + exception.getMessage());
                LOGGER.throwing(JarProcessor.class.getName(), "processJar", exception);
            }
        }
    }

    /**
     * Gets the obfuscation statistics
     */
    public ObfuscationStatsCollector getStats() {
        return this.obfuscatorEngine.getStats();
    }
}
