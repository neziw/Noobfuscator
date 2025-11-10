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
package ovh.neziw.obfuscator.obfuscator.classname;

import java.util.logging.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

/**
 * Remaps types, handles, and annotations in bytecode instructions
 * Single Responsibility: Remap class references in bytecode instructions
 */
public class TypeRemapper {

    private static final Logger LOGGER = Logger.getLogger(TypeRemapper.class.getName());

    private final ClassNameMapper classNameMapper;
    private final SignatureObfuscator signatureObfuscator;

    public TypeRemapper(final ClassNameMapper classNameMapper, final SignatureObfuscator signatureObfuscator) {
        this.classNameMapper = classNameMapper;
        this.signatureObfuscator = signatureObfuscator;
    }

    /**
     * Remaps a Handle (method/field reference)
     */
    public Handle remapHandle(final Handle handle) {
        if (handle == null) {
            return null;
        }
        try {
            final String obfuscatedOwner = this.classNameMapper.obfuscateClassName(handle.getOwner());
            final String descriptor = handle.getDesc();

            if (descriptor == null || descriptor.isEmpty()) {
                return new Handle(handle.getTag(), obfuscatedOwner, handle.getName(), descriptor, handle.isInterface());
            }

            final String obfuscatedDescriptor;
            // Handle can represent either a method or a field
            final int tag = handle.getTag();
            if (tag >= org.objectweb.asm.Opcodes.H_GETFIELD && tag <= org.objectweb.asm.Opcodes.H_PUTSTATIC) {
                // Field handle - use type descriptor
                obfuscatedDescriptor = this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            } else {
                // Method handle - use method descriptor
                obfuscatedDescriptor = this.signatureObfuscator.obfuscateMethodDescriptor(descriptor);
            }
            return new Handle(handle.getTag(), obfuscatedOwner, handle.getName(), obfuscatedDescriptor, handle.isInterface());
        } catch (final Exception exception) {
            LOGGER.warning("Failed to remap Handle: " + handle + ", error: " + exception.getMessage());
            return handle;
        }
    }

    /**
     * Remaps a bootstrap argument
     */
    public Object remapBootstrapArgument(final Object arg) {
        try {
            if (arg instanceof Handle) {
                return this.remapHandle((Handle) arg);
            } else if (arg instanceof final Type type) {
                if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                    final String obfuscatedInternalName = this.classNameMapper.obfuscateClassName(type.getInternalName());
                    return Type.getObjectType(obfuscatedInternalName);
                }
            }
            return arg;
        } catch (final Exception exception) {
            LOGGER.warning("Failed to remap bootstrap argument: " + arg + ", error: " + exception.getMessage());
            return arg;
        }
    }

    /**
     * Creates an annotation remapper visitor
     */
    public AnnotationVisitor createAnnotationRemapper(final AnnotationVisitor av) {
        return new AnnotationRemapper(av);
    }

    /**
     * Annotation visitor that remaps class names in annotations
     */
    private class AnnotationRemapper extends AnnotationVisitor {

        public AnnotationRemapper(final AnnotationVisitor av) {
            super(org.objectweb.asm.Opcodes.ASM9, av);
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
            final String obfuscatedDescriptor = TypeRemapper.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            final AnnotationVisitor av = super.visitAnnotation(name, obfuscatedDescriptor);
            return av != null ? new AnnotationRemapper(av) : null;
        }

        @Override
        public AnnotationVisitor visitArray(final String name) {
            final AnnotationVisitor av = super.visitArray(name);
            return av != null ? new AnnotationRemapper(av) : null;
        }

        @Override
        public void visitEnum(final String name, final String descriptor, final String value) {
            final String obfuscatedDescriptor = TypeRemapper.this.signatureObfuscator.obfuscateTypeDescriptor(descriptor);
            super.visitEnum(name, obfuscatedDescriptor, value);
        }
    }
}

