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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Obfuscates variable names in class files using ASM
 */
public class VariableObfuscator extends ClassVisitor {

    private final NameGenerator nameGenerator;
    private final PatternMatcher patternMatcher;
    private final ObfuscationStats stats;
    private final Map<String, String> fieldNameMap = new HashMap<>();
    private final Map<String, Map<String, String>> globalFieldMappings;
    private final Map<String, Map<String, Map<String, String>>> globalLocalVariableMappings;
    private String className;
    private boolean shouldObfuscate;
    private String currentClassName;

    public VariableObfuscator(final ClassVisitor cv, final NameGenerator nameGenerator, final PatternMatcher patternMatcher,
                              final ObfuscationStats stats,
                              final Map<String, Map<String, String>> globalFieldMappings,
                              final Map<String, Map<String, Map<String, String>>> globalLocalVariableMappings,
                              final String className) {
        super(Opcodes.ASM9, cv);
        this.nameGenerator = nameGenerator;
        this.patternMatcher = patternMatcher;
        this.stats = stats;
        this.globalFieldMappings = globalFieldMappings;
        this.globalLocalVariableMappings = globalLocalVariableMappings;
        this.currentClassName = className;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.className = name;
        this.currentClassName = name;
        this.shouldObfuscate = this.patternMatcher.matches(name);
        this.fieldNameMap.clear();
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        if (!this.shouldObfuscate) {
            return this.cv.visitField(access, name, descriptor, signature, value);
        }
        if ((access & Opcodes.ACC_SYNTHETIC) != 0 || (access & Opcodes.ACC_ENUM) != 0) {
            return this.cv.visitField(access, name, descriptor, signature, value);
        }
        if (this.nameGenerator.isObfuscated(name)) {
            return this.cv.visitField(access, name, descriptor, signature, value);
        }
        if (name.equals("serialVersionUID")) {
            return this.cv.visitField(access, name, descriptor, signature, value);
        }
        final String obfuscatedName = this.nameGenerator.getObfuscatedName(this.className + "." + name);
        this.fieldNameMap.put(name, obfuscatedName);
        if (this.globalFieldMappings != null && this.currentClassName != null) {
            this.globalFieldMappings.computeIfAbsent(this.currentClassName, k -> new HashMap<>()).put(name, obfuscatedName);
        }
        this.stats.incrementVariablesObfuscated();
        return this.cv.visitField(access, obfuscatedName, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final MethodVisitor mv = this.cv.visitMethod(access, name, descriptor, signature, exceptions);
        if (!this.shouldObfuscate) {
            return mv;
        }
        return new MethodVariableObfuscator(mv, this.nameGenerator, this.fieldNameMap, this.className, access, name, descriptor, this.stats,
            this.globalLocalVariableMappings, this.currentClassName);
    }

    /**
     * Statistics for variable obfuscation
     */
    @Getter
    public static class ObfuscationStats {

        private int variablesObfuscated = 0;

        public void incrementVariablesObfuscated() {
            this.variablesObfuscated++;
        }

    }

    /**
     * Method visitor that obfuscates local variables and updates field references
     */
    private static class MethodVariableObfuscator extends MethodVisitor {

        private final NameGenerator nameGenerator;
        private final Map<String, String> nameMapping;
        private final Map<String, String> fieldNameMap;
        private final String className;
        private final Set<String> usedNames;
        private final ObfuscationStats stats;
        private final boolean obfuscateLocals;
        private final Map<String, Map<String, Map<String, String>>> globalLocalVariableMappings;
        private final String currentClassName;
        private final String methodName;
        private final String methodDescriptor;

        public MethodVariableObfuscator(final MethodVisitor mv, final NameGenerator nameGenerator, final Map<String, String> fieldNameMap,
                                        final String className, final int access, final String name, final String descriptor, final ObfuscationStats stats,
                                        final Map<String, Map<String, Map<String, String>>> globalLocalVariableMappings,
                                        final String currentClassName) {
            super(Opcodes.ASM9, mv);
            this.nameGenerator = nameGenerator;
            this.nameMapping = new HashMap<>();
            this.fieldNameMap = fieldNameMap;
            this.className = className;
            this.usedNames = new HashSet<>();
            this.stats = stats;
            this.globalLocalVariableMappings = globalLocalVariableMappings;
            this.currentClassName = currentClassName;
            this.methodName = name;
            this.methodDescriptor = descriptor;
            this.obfuscateLocals = !name.equals("<clinit>");
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            // Update field references: GETFIELD, PUTFIELD, GETSTATIC, PUTSTATIC
            // Only update if the field belongs to the current class
            if (owner.equals(this.className) && this.fieldNameMap.containsKey(name)) {
                final String obfuscatedFieldName = this.fieldNameMap.get(name);
                super.visitFieldInsn(opcode, owner, obfuscatedFieldName, descriptor);
            } else {
                // Field from another class or not obfuscated, use original name
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }
        }

        @Override
        public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
            if (!this.obfuscateLocals) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
                return;
            }
            if (name == null || name.isEmpty()) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
                return;
            }
            if (name.equals("this")) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
                return;
            }
            if (this.nameGenerator.isObfuscated(name)) {
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
                return;
            }
            String obfuscatedName = this.nameMapping.get(name);
            if (obfuscatedName == null) {
                do {
                    obfuscatedName = this.nameGenerator.generateName();
                } while (this.usedNames.contains(obfuscatedName));

                this.usedNames.add(obfuscatedName);
                this.nameMapping.put(name, obfuscatedName);
                if (this.globalLocalVariableMappings != null && this.currentClassName != null && this.methodName != null) {
                    final String methodKey = this.methodName + this.methodDescriptor;
                    this.globalLocalVariableMappings
                        .computeIfAbsent(this.currentClassName, k -> new HashMap<>())
                        .computeIfAbsent(methodKey, k -> new HashMap<>())
                        .put(name, obfuscatedName);
                }
                this.stats.incrementVariablesObfuscated();
            }
            super.visitLocalVariable(obfuscatedName, descriptor, signature, start, end, index);
        }
    }
}
