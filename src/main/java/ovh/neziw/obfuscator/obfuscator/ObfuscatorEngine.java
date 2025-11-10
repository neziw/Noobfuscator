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

import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import ovh.neziw.obfuscator.config.JsonConfig;
import ovh.neziw.obfuscator.obfuscator.chain.ObfuscatorChainBuilder;
import ovh.neziw.obfuscator.obfuscator.mappings.ObfuscationMappings;
import ovh.neziw.obfuscator.obfuscator.stats.ObfuscationStatsCollector;

/**
 * Main obfuscator engine that orchestrates the obfuscation process
 * Single Responsibility: Coordinate obfuscation of a single class
 */
public class ObfuscatorEngine {

    private final PatternMatcher patternMatcher;
    @Getter
    private final NameGenerator nameGenerator;
    @Getter
    private final ObfuscationStatsCollector stats;
    private final ObfuscationMappings mappings;
    private final ObfuscatorChainBuilder chainBuilder;

    public ObfuscatorEngine(final JsonConfig config) {
        this.patternMatcher = new PatternMatcher(config.getInclude());
        final String watermark = config.getWatermark();
        this.nameGenerator = new NameGenerator(watermark != null ? watermark : "");
        this.stats = new ObfuscationStatsCollector();
        this.mappings = new ObfuscationMappings();
        this.chainBuilder = new ObfuscatorChainBuilder(config, this.patternMatcher, this.nameGenerator, this.stats, this.mappings);
    }

    /**
     * Obfuscates a class file bytecode
     *
     * @param classBytes Original class file bytes
     * @param className  Internal class name (e.g., "com/example/Test")
     * @return Obfuscated class file bytes
     */
    public byte[] obfuscateClass(final byte[] classBytes, final String className) throws IOException {
        final ClassReader classReader = new ClassReader(classBytes);
        final ClassWriter classWriter = new SafeClassWriter(classReader,
            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, this.mappings.getClassNameMap());
        final ClassVisitor cv = this.chainBuilder.buildChain(classWriter, className);
        classReader.accept(cv, ClassReader.EXPAND_FRAMES);

        return classWriter.toByteArray();
    }

    /**
     * Gets the obfuscated class name for a given class
     * Returns the original name if not obfuscated
     */
    public String getObfuscatedClassName(final String className) {
        return this.mappings.getClassNameMap().getOrDefault(className, className);
    }

    /**
     * Gets the class name map (for pre-scanning phase)
     */
    public Map<String, String> getClassNameMap() {
        return this.mappings.getClassNameMap();
    }

    /**
     * Gets the method name map (for pre-scanning phase)
     */
    public Map<String, Map<String, String>> getMethodNameMap() {
        return this.mappings.getMethodNameMap();
    }

    /**
     * Gets the field mappings
     */
    public Map<String, Map<String, String>> getFieldMappings() {
        return this.mappings.getFieldMappings();
    }

    /**
     * Gets the local variable mappings
     */
    public Map<String, Map<String, Map<String, String>>> getLocalVariableMappings() {
        return this.mappings.getLocalVariableMappings();
    }

    /**
     * Checks if a class should be obfuscated based on include patterns
     */
    public boolean shouldObfuscate(final String className) {
        return this.patternMatcher.matches(className);
    }

}
