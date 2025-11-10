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
import lombok.Getter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import ovh.neziw.obfuscator.obfuscator.method.MethodCallRemapper;
import ovh.neziw.obfuscator.obfuscator.method.MethodNameMapper;
import ovh.neziw.obfuscator.obfuscator.method.MethodObfuscationChecker;

/**
 * Obfuscates method names in class files, skipping methods that implement/override interface methods
 * or have @Override annotation
 * Single Responsibility: Coordinate method name obfuscation for a class
 */
public class MethodNameObfuscator extends ClassVisitor {

    private final NameGenerator nameGenerator;
    private final PatternMatcher patternMatcher;
    private final ObfuscationStats stats;
    private final Map<String, Map<String, String>> globalMethodNameMap;
    private final Map<String, String> classNameMap;
    private boolean shouldObfuscate;
    private ClassNode classNode;
    private MethodNameMapper methodNameMapper;
    private MethodCallRemapper methodCallRemapper;
    private MethodObfuscationChecker methodChecker;

    public MethodNameObfuscator(final ClassVisitor cv, final NameGenerator nameGenerator, final PatternMatcher patternMatcher,
                                final ObfuscationStats stats, final Map<String, Map<String, String>> globalMethodNameMap,
                                final Map<String, String> classNameMap) {
        super(Opcodes.ASM9, cv);
        this.nameGenerator = nameGenerator;
        this.patternMatcher = patternMatcher;
        this.stats = stats;
        this.globalMethodNameMap = globalMethodNameMap;
        this.classNameMap = classNameMap;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        String originalClassName;
        if (this.classNameMap != null) {
            // Try to find original class name by reverse lookup
            originalClassName = this.findOriginalClassName(name);
            if (originalClassName == null) {
                // Not found in map, so name is original (not obfuscated yet)
                originalClassName = name;
            }
        } else {
            originalClassName = name;
        }
        // Check if class should be obfuscated using original name (pattern matching uses original names)
        this.shouldObfuscate = this.patternMatcher.matches(originalClassName);
        final boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        if (this.shouldObfuscate) {
            // Initialize helper classes
            this.methodNameMapper = new MethodNameMapper(this.nameGenerator, originalClassName, this.globalMethodNameMap);
            this.methodCallRemapper = new MethodCallRemapper(this.globalMethodNameMap, this.classNameMap);
            this.methodChecker = new MethodObfuscationChecker(this.nameGenerator, isInterface);
            // Use ClassNode to collect all methods and check annotations
            this.classNode = new ClassNode();
            this.classNode.visit(version, access, name, signature, superName, interfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    @Override
    public void visitSource(final String source, final String debug) {
        if (this.shouldObfuscate && this.classNode != null) {
            this.classNode.visitSource(source, debug);
        } else {
            super.visitSource(source, debug);
        }
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
        if (this.shouldObfuscate && this.classNode != null) {
            this.classNode.visitOuterClass(owner, name, descriptor);
        } else {
            super.visitOuterClass(owner, name, descriptor);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (this.shouldObfuscate && this.classNode != null) {
            return this.classNode.visitAnnotation(descriptor, visible);
        } else {
            return super.visitAnnotation(descriptor, visible);
        }
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (this.shouldObfuscate && this.classNode != null) {
            return this.classNode.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        } else {
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
        }
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        if (this.shouldObfuscate && this.classNode != null) {
            this.classNode.visitAttribute(attribute);
        } else {
            super.visitAttribute(attribute);
        }
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        if (this.shouldObfuscate && this.classNode != null) {
            this.classNode.visitInnerClass(name, outerName, innerName, access);
        } else {
            super.visitInnerClass(name, outerName, innerName, access);
        }
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        if (this.shouldObfuscate && this.classNode != null) {
            return this.classNode.visitField(access, name, descriptor, signature, value);
        } else {
            return super.visitField(access, name, descriptor, signature, value);
        }
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        if (this.shouldObfuscate && this.classNode != null) {
            return this.classNode.visitMethod(access, name, descriptor, signature, exceptions);
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {
        if (this.shouldObfuscate && this.classNode != null) {
            this.processMethods();
            this.classNode.accept(new MethodRemappingClassVisitor(this.cv));
        } else {
            super.visitEnd();
        }
    }

    /**
     * Processes methods and determines which should be obfuscated
     */
    private void processMethods() {
        if (this.classNode.methods == null) {
            return;
        }
        for (final MethodNode method : this.classNode.methods) {
            final String methodKey = method.name + method.desc;
            if (this.methodNameMapper.isMapped(methodKey)) {
                continue;
            }
            if (this.methodChecker.shouldObfuscateMethod(method)) {
                final String obfuscatedName = this.methodNameMapper.generateObfuscatedMethodName(method.name, method.desc);
                this.methodNameMapper.mapMethod(method, obfuscatedName);
                this.stats.incrementMethodsObfuscated();
            }
        }
        for (final MethodNode method : this.classNode.methods) {
            this.methodCallRemapper.remapMethodCalls(method);
        }
        this.methodNameMapper.applyMappingsToMethods(this.classNode.methods);
    }

    /**
     * Finds the original class name from an obfuscated class name
     * Returns null if not found or if classNameMap is null
     */
    private String findOriginalClassName(final String obfuscatedClassName) {
        if (this.classNameMap == null) {
            return null;
        }
        // Reverse lookup: find original class name that maps to obfuscatedClassName
        for (final Map.Entry<String, String> entry : this.classNameMap.entrySet()) {
            if (entry.getValue().equals(obfuscatedClassName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Statistics for method name obfuscation
     */
    @Getter
    public static class ObfuscationStats {

        private int methodsObfuscated = 0;

        public void incrementMethodsObfuscated() {
            this.methodsObfuscated++;
        }

    }

    /**
     * Class visitor that remaps method calls to obfuscated names
     */
    private static class MethodRemappingClassVisitor extends ClassVisitor {

        public MethodRemappingClassVisitor(final ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }
    }
}
