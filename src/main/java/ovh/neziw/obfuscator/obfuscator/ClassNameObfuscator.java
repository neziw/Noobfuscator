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
import java.util.logging.Logger;
import lombok.Getter;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import ovh.neziw.obfuscator.obfuscator.classname.ClassNameMapper;
import ovh.neziw.obfuscator.obfuscator.classname.SignatureObfuscator;
import ovh.neziw.obfuscator.obfuscator.classname.TypeRemapper;

/**
 * Obfuscates class names and package names in class files using ASM
 * Single Responsibility: Coordinate class name obfuscation for a class
 */
public class ClassNameObfuscator extends ClassVisitor {

    private static final Logger LOGGER = Logger.getLogger(ClassNameObfuscator.class.getName());

    private final PatternMatcher patternMatcher;
    private final ObfuscationStats stats;
    private final ClassNameMapper classNameMapper;
    private final SignatureObfuscator signatureObfuscator;
    private final TypeRemapper typeRemapper;
    private boolean shouldObfuscate;

    public ClassNameObfuscator(final ClassVisitor cv, final NameGenerator nameGenerator, final PatternMatcher patternMatcher,
                               final ObfuscationStats stats, final Map<String, String> classNameMap,
                               final boolean obfuscatePackages, final boolean obfuscateClassNames) {
        super(Opcodes.ASM9, cv);
        this.patternMatcher = patternMatcher;
        this.stats = stats;
        this.classNameMapper = new ClassNameMapper(nameGenerator, patternMatcher, classNameMap,
            obfuscatePackages, obfuscateClassNames);
        this.signatureObfuscator = new SignatureObfuscator(this.classNameMapper);
        this.typeRemapper = new TypeRemapper(this.classNameMapper, this.signatureObfuscator);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.shouldObfuscate = this.patternMatcher.matches(name);
        if (this.shouldObfuscate) {
            final String obfuscatedName = this.classNameMapper.getOrGenerateObfuscatedName(name);
            this.stats.incrementClassesObfuscated();
            final String obfuscatedSuperName = this.classNameMapper.obfuscateClassName(superName);
            String[] obfuscatedInterfaces = null;
            if (interfaces != null && interfaces.length > 0) {
                obfuscatedInterfaces = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    obfuscatedInterfaces[i] = this.classNameMapper.obfuscateClassName(interfaces[i]);
                }
            }
            // Obfuscate signature if present
            final String obfuscatedSignature = this.signatureObfuscator.obfuscateSignature(signature);
            super.visit(version, access, obfuscatedName, obfuscatedSignature, obfuscatedSuperName, obfuscatedInterfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        if (this.shouldObfuscate) {
            final String obfuscatedName = this.classNameMapper.obfuscateClassName(name);
            final String obfuscatedOuterName = outerName != null ? this.classNameMapper.obfuscateClassName(outerName) : null;
            // innerName is simple name, might need to extract from obfuscated name
            final String obfuscatedInnerName = innerName != null ? PatternMatcher.getSimpleClassName(obfuscatedName) : null;
            super.visitInnerClass(obfuscatedName, obfuscatedOuterName, obfuscatedInnerName, access);
        } else {
            super.visitInnerClass(name, outerName, innerName, access);
        }
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        if (this.shouldObfuscate) {
            final String obfuscatedDescriptor = this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            final String obfuscatedSignature = this.signatureObfuscator.obfuscateSignature(signature);
            return super.visitField(access, name, obfuscatedDescriptor, obfuscatedSignature, value);
        } else {
            return super.visitField(access, name, descriptor, signature, value);
        }
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final MethodVisitor mv;
        if (this.shouldObfuscate) {
            final String obfuscatedDescriptor = this.signatureObfuscator.obfuscateMethodDescriptor(descriptor);
            final String obfuscatedSignature = this.signatureObfuscator.obfuscateSignature(signature);
            String[] obfuscatedExceptions = null;
            if (exceptions != null && exceptions.length > 0) {
                obfuscatedExceptions = new String[exceptions.length];
                for (int i = 0; i < exceptions.length; i++) {
                    obfuscatedExceptions[i] = this.classNameMapper.obfuscateClassName(exceptions[i]);
                }
            }
            mv = super.visitMethod(access, name, obfuscatedDescriptor, obfuscatedSignature, obfuscatedExceptions);
        } else {
            mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        // We need to return a MethodVisitor that remaps class references in instructions
        if (this.shouldObfuscate) {
            return new MethodRemapper(mv);
        } else {
            return mv;
        }
    }

    /**
     * Statistics for class name obfuscation
     */
    @Getter
    public static class ObfuscationStats {

        private int classesObfuscated = 0;
        private final int packagesObfuscated = 0; // TODO

        public void incrementClassesObfuscated() {
            this.classesObfuscated++;
        }
    }

    /**
     * Method visitor that remaps class references in bytecode instructions
     */
    private class MethodRemapper extends MethodVisitor {
        public MethodRemapper(final MethodVisitor mv) {
            super(Opcodes.ASM9, mv);
        }

        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            final String obfuscatedType = ClassNameObfuscator.this.classNameMapper.obfuscateClassName(type);
            super.visitTypeInsn(opcode, obfuscatedType);
        }

        @Override
        public void visitFieldInsn(final int opcode, final String owner, final String name, final String descriptor) {
            try {
                final String obfuscatedOwner = ClassNameObfuscator.this.classNameMapper.obfuscateClassName(owner);
                final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
                super.visitFieldInsn(opcode, obfuscatedOwner, name, obfuscatedDescriptor);
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap field instruction: " + owner + "." + name + " " + descriptor + ", error: " + exception.getMessage());
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }
        }

        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String descriptor, final boolean isInterface) {
            try {
                final String obfuscatedOwner = ClassNameObfuscator.this.classNameMapper.obfuscateClassName(owner);
                final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateMethodDescriptor(descriptor);
                super.visitMethodInsn(opcode, obfuscatedOwner, name, obfuscatedDescriptor, isInterface);
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap method instruction: " + owner + "." + name + descriptor + ", error: " + exception.getMessage());
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }

        @Override
        public void visitInvokeDynamicInsn(final String name, final String descriptor, final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
            try {
                final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateMethodDescriptor(descriptor);
                final Handle obfuscatedHandle = ClassNameObfuscator.this.typeRemapper.remapHandle(bootstrapMethodHandle);
                final Object[] obfuscatedArgs = new Object[bootstrapMethodArguments.length];
                for (int i = 0; i < bootstrapMethodArguments.length; i++) {
                    try {
                        obfuscatedArgs[i] = ClassNameObfuscator.this.typeRemapper.remapBootstrapArgument(bootstrapMethodArguments[i]);
                    } catch (final Exception exception) {
                        LOGGER.warning("Failed to remap bootstrap argument " + i + ": " + exception.getMessage());
                        obfuscatedArgs[i] = bootstrapMethodArguments[i];
                    }
                }
                super.visitInvokeDynamicInsn(name, obfuscatedDescriptor, obfuscatedHandle, obfuscatedArgs);
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap InvokeDynamic instruction: " + exception.getMessage());
                super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            }
        }

        @Override
        public void visitLdcInsn(final Object value) {
            try {
                if (value instanceof final Type type) {
                    if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                        final String obfuscatedInternalName = ClassNameObfuscator.this.classNameMapper.obfuscateClassName(type.getInternalName());
                        super.visitLdcInsn(Type.getObjectType(obfuscatedInternalName));
                    } else {
                        super.visitLdcInsn(value);
                    }
                } else if (value instanceof Handle) {
                    final Handle remappedHandle = ClassNameObfuscator.this.typeRemapper.remapHandle((Handle) value);
                    super.visitLdcInsn(remappedHandle);
                } else {
                    super.visitLdcInsn(value);
                }
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap LDC instruction: " + exception.getMessage());
                super.visitLdcInsn(value);
            }
        }

        @Override
        public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
            try {
                final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
                super.visitMultiANewArrayInsn(obfuscatedDescriptor, numDimensions);
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap MultiANewArray instruction: " + exception.getMessage());
                super.visitMultiANewArrayInsn(descriptor, numDimensions);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
            final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            final AnnotationVisitor av = super.visitAnnotation(obfuscatedDescriptor, visible);
            return av != null ? ClassNameObfuscator.this.typeRemapper.createAnnotationRemapper(av) : null;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(final int parameter, final String descriptor, final boolean visible) {
            final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            final AnnotationVisitor av = super.visitParameterAnnotation(parameter, obfuscatedDescriptor, visible);
            return av != null ? ClassNameObfuscator.this.typeRemapper.createAnnotationRemapper(av) : null;
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
            final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            final AnnotationVisitor av = super.visitTypeAnnotation(typeRef, typePath, obfuscatedDescriptor, visible);
            return av != null ? ClassNameObfuscator.this.typeRemapper.createAnnotationRemapper(av) : null;
        }

        @Override
        public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
            try {
                final String obfuscatedType = type != null ? ClassNameObfuscator.this.classNameMapper.obfuscateClassName(type) : null;
                super.visitTryCatchBlock(start, end, handler, obfuscatedType);
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap try-catch block type: " + exception.getMessage());
                super.visitTryCatchBlock(start, end, handler, type);
            }
        }

        @Override
        public void visitLocalVariable(final String name, final String descriptor, final String signature, final Label start, final Label end, final int index) {
            try {
                final String obfuscatedDescriptor = ClassNameObfuscator.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
                final String obfuscatedSignature = ClassNameObfuscator.this.signatureObfuscator.obfuscateSignature(signature);
                super.visitLocalVariable(name, obfuscatedDescriptor, obfuscatedSignature, start, end, index);
            } catch (final Exception exception) {
                LOGGER.warning("Failed to remap local variable: " + exception.getMessage());
                super.visitLocalVariable(name, descriptor, signature, start, end, index);
            }
        }
    }
}
