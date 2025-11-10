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
package ovh.neziw.obfuscator.obfuscator.stats;

import lombok.Getter;
import ovh.neziw.obfuscator.obfuscator.ClassNameObfuscator;
import ovh.neziw.obfuscator.obfuscator.FlowObfuscator;
import ovh.neziw.obfuscator.obfuscator.MethodNameObfuscator;
import ovh.neziw.obfuscator.obfuscator.MethodOrderObfuscator;
import ovh.neziw.obfuscator.obfuscator.StringObfuscator;
import ovh.neziw.obfuscator.obfuscator.VariableObfuscator;

/**
 * Collects and aggregates obfuscation statistics from all obfuscators
 * Single Responsibility: Manage and aggregate statistics
 */
@Getter
public class ObfuscationStatsCollector {

    private final VariableObfuscator.ObfuscationStats variableStats = new VariableObfuscator.ObfuscationStats();
    private final ClassNameObfuscator.ObfuscationStats classNameStats = new ClassNameObfuscator.ObfuscationStats();
    private final StringObfuscator.ObfuscationStats stringStats = new StringObfuscator.ObfuscationStats();
    private final MethodNameObfuscator.ObfuscationStats methodNameStats = new MethodNameObfuscator.ObfuscationStats();
    private final MethodOrderObfuscator.ObfuscationStats methodOrderStats = new MethodOrderObfuscator.ObfuscationStats();
    private final FlowObfuscator.ObfuscationStats flowStats = new FlowObfuscator.ObfuscationStats();
    private int classesProcessed = 0;
    private int classesObfuscated = 0;

    public void incrementClassesProcessed() {
        this.classesProcessed++;
    }

    public void incrementClassesObfuscated() {
        this.classesObfuscated++;
    }

    public int getVariablesObfuscated() {
        return this.variableStats.getVariablesObfuscated();
    }

    @Override
    public String toString() {
        return String.format("Classes processed: %d, Obfuscated: %d, Variables obfuscated: %d, Class names obfuscated: %d, Strings obfuscated: %d, Method names obfuscated: %d, Methods reordered: %d, Flow obfuscated: %d",
            this.classesProcessed, this.classesObfuscated, this.getVariablesObfuscated(), this.classNameStats.getClassesObfuscated(),
            this.stringStats.getStringsObfuscated(), this.methodNameStats.getMethodsObfuscated(),
            this.methodOrderStats.getMethodsReordered(), this.flowStats.getMethodsObfuscated());
    }
}
