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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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

/**
 * Obfuscates method order in class files by randomizing their sequence
 */
public class MethodOrderObfuscator extends ClassVisitor {

    private final PatternMatcher patternMatcher;
    private final ObfuscationStats stats;
    private boolean shouldObfuscate;
    private ClassNode classNode;

    public MethodOrderObfuscator(final ClassVisitor cv, final PatternMatcher patternMatcher, final ObfuscationStats stats) {
        super(Opcodes.ASM9, cv);
        this.patternMatcher = patternMatcher;
        this.stats = stats;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.shouldObfuscate = this.patternMatcher.matches(name);
        if (this.shouldObfuscate) {
            // Use ClassNode to collect all methods, then reorder them
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
            this.reorderMethods();
            this.classNode.accept(this.cv);
        } else {
            super.visitEnd();
        }
    }

    /**
     * Reorders methods in the class by randomizing their sequence
     */
    private void reorderMethods() {
        if (this.classNode.methods == null || this.classNode.methods.size() <= 1) {
            return;
        }
        final List<MethodNode> specialMethods = new ArrayList<>();
        final List<MethodNode> regularMethods = new ArrayList<>();
        for (final MethodNode method : this.classNode.methods) {
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                specialMethods.add(method);
            } else {
                regularMethods.add(method);
            }
        }
        if (regularMethods.size() <= 1) {
            return;
        }
        Collections.shuffle(regularMethods, new Random());
        this.classNode.methods.clear();
        this.classNode.methods.addAll(specialMethods);
        this.classNode.methods.addAll(regularMethods);
        this.stats.incrementMethodsReordered();
    }

    /**
     * Statistics for method order obfuscation
     */
    @Getter
    public static class ObfuscationStats {

        private int methodsReordered = 0;

        public void incrementMethodsReordered() {
            this.methodsReordered++;
        }

    }
}
