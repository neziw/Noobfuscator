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
package ovh.neziw.obfuscator.obfuscator.chain;

import org.objectweb.asm.ClassVisitor;
import ovh.neziw.obfuscator.config.JsonConfig;
import ovh.neziw.obfuscator.obfuscator.ClassNameObfuscator;
import ovh.neziw.obfuscator.obfuscator.FlowObfuscator;
import ovh.neziw.obfuscator.obfuscator.MethodNameObfuscator;
import ovh.neziw.obfuscator.obfuscator.MethodOrderObfuscator;
import ovh.neziw.obfuscator.obfuscator.NameGenerator;
import ovh.neziw.obfuscator.obfuscator.PatternMatcher;
import ovh.neziw.obfuscator.obfuscator.StringObfuscator;
import ovh.neziw.obfuscator.obfuscator.VariableObfuscator;
import ovh.neziw.obfuscator.obfuscator.mappings.ObfuscationMappings;
import ovh.neziw.obfuscator.obfuscator.stats.ObfuscationStatsCollector;

/**
 * Builds the chain of obfuscators based on configuration
 * Single Responsibility: Construct the obfuscator chain in the correct order
 */
public class ObfuscatorChainBuilder {

    private final JsonConfig config;
    private final PatternMatcher patternMatcher;
    private final NameGenerator nameGenerator;
    private final ObfuscationStatsCollector stats;
    private final ObfuscationMappings mappings;

    public ObfuscatorChainBuilder(final JsonConfig config, final PatternMatcher patternMatcher,
                                  final NameGenerator nameGenerator,
                                  final ObfuscationStatsCollector stats,
                                  final ObfuscationMappings mappings) {
        this.config = config;
        this.patternMatcher = patternMatcher;
        this.nameGenerator = nameGenerator;
        this.stats = stats;
        this.mappings = mappings;
    }

    /**
     * Builds the obfuscator chain starting from the given ClassVisitor
     * Order matters: class name obfuscation should be first (outermost)
     */
    public ClassVisitor buildChain(final ClassVisitor baseVisitor, final String className) {
        ClassVisitor cv = baseVisitor;
        // Class name and package obfuscation (must be first to handle all class references)
        if (this.config.isObfuscateClassNames() || this.config.isObfuscatePackages()) {
            cv = new ClassNameObfuscator(cv, this.nameGenerator, this.patternMatcher,
                this.stats.getClassNameStats(), this.mappings.getClassNameMap(),
                this.config.isObfuscatePackages(), this.config.isObfuscateClassNames());
        }
        // Variable obfuscation
        if (this.config.isObfuscateVariables()) {
            cv = new VariableObfuscator(cv, this.nameGenerator, this.patternMatcher, this.stats.getVariableStats(),
                this.mappings.getFieldMappings(), this.mappings.getLocalVariableMappings(), className);
        }
        // String obfuscation (before method order, as it adds methods)
        if (this.config.isObfuscateStrings()) {
            cv = new StringObfuscator(cv, this.patternMatcher, this.stats.getStringStats());
        }
        // Method name obfuscation (before method order, as it changes method names)
        if (this.config.isObfuscateMethodNames()) {
            cv = new MethodNameObfuscator(cv, this.nameGenerator, this.patternMatcher, this.stats.getMethodNameStats(),
                this.mappings.getMethodNameMap(), this.mappings.getClassNameMap());
        }
        // Flow obfuscation (before method order, as it modifies method code)
        final String flowObfuscation = this.config.getFlowObfuscation();
        if (flowObfuscation != null && !flowObfuscation.equals("NONE")) {
            cv = new FlowObfuscator(cv, this.patternMatcher, this.stats.getFlowStats(), flowObfuscation);
        }
        // Method order obfuscation (should be last as it reorders methods)
        if (this.config.isChangeMethodsOrders()) {
            cv = new MethodOrderObfuscator(cv, this.patternMatcher, this.stats.getMethodOrderStats());
        }
        return cv;
    }
}

