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

import java.nio.charset.StandardCharsets;
import java.util.Random;
import lombok.Getter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Obfuscates string literals in class files by encoding them and adding a decoder method
 */
public class StringObfuscator extends ClassVisitor {

    private final PatternMatcher patternMatcher;
    private final ObfuscationStats stats;
    private final Random random;
    private boolean shouldObfuscate;
    private String className;
    private boolean decoderMethodAdded;
    private int decoderMethodKey;

    public StringObfuscator(final ClassVisitor cv, final PatternMatcher patternMatcher, final ObfuscationStats stats) {
        super(Opcodes.ASM9, cv);
        this.patternMatcher = patternMatcher;
        this.stats = stats;
        this.random = new Random();
        this.decoderMethodKey = this.random.nextInt(256);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.className = name;
        this.shouldObfuscate = this.patternMatcher.matches(name);
        this.decoderMethodAdded = false;
        this.decoderMethodKey = this.random.nextInt(256);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (this.shouldObfuscate) {
            return new StringObfuscatingMethodVisitor(mv, this);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (this.shouldObfuscate && this.decoderMethodAdded) {
            this.addDecoderMethod();
            this.stats.incrementClassesWithObfuscatedStrings();
        }
        super.visitEnd();
    }

    /**
     * Encodes a string using XOR with a key
     */
    private byte[] encodeString(final String str, final int key) {
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        final byte[] encoded = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            encoded[i] = (byte) (bytes[i] ^ key);
        }
        return encoded;
    }

    /**
     * Adds a static decoder method to the class
     */
    private void addDecoderMethod() {
        final MethodVisitor mv = super.visitMethod(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
            "decode",
            "([BI)Ljava/lang/String;",
            null,
            null
        );
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        final Label loopStart = new Label();
        final Label loopEnd = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.BALOAD);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitInsn(Opcodes.I2B);
        mv.visitInsn(Opcodes.BASTORE);
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);
        mv.visitLabel(loopEnd);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitLdcInsn("UTF-8");
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/String",
            "<init>",
            "([BLjava/lang/String;)V",
            false
        );
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(5, 4);
        mv.visitEnd();
    }

    /**
     * Statistics for string obfuscation
     */
    @Getter
    public static class ObfuscationStats {

        private int stringsObfuscated = 0;
        private int classesWithObfuscatedStrings = 0;

        public void incrementStringsObfuscated() {
            this.stringsObfuscated++;
        }

        public void incrementClassesWithObfuscatedStrings() {
            this.classesWithObfuscatedStrings++;
        }

    }

    /**
     * Method visitor that obfuscates string literals
     */
    private class StringObfuscatingMethodVisitor extends MethodVisitor {

        private final StringObfuscator obfuscator;

        public StringObfuscatingMethodVisitor(final MethodVisitor mv, final StringObfuscator obfuscator) {
            super(Opcodes.ASM9, mv);
            this.obfuscator = obfuscator;
        }

        @Override
        public void visitLdcInsn(final Object value) {
            if (value instanceof final String str) {
                if (str.length() < 2) {
                    super.visitLdcInsn(value);
                    return;
                }
                final byte[] encoded = this.obfuscator.encodeString(str, this.obfuscator.decoderMethodKey);
                this.pushByteArray(encoded);
                this.mv.visitIntInsn(Opcodes.BIPUSH, this.obfuscator.decoderMethodKey);
                this.mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    this.obfuscator.className,
                    "decode",
                    "([BI)Ljava/lang/String;",
                    false
                );
                this.obfuscator.decoderMethodAdded = true;
                StringObfuscator.this.stats.incrementStringsObfuscated();
            } else {
                super.visitLdcInsn(value);
            }
        }

        /**
         * Pushes a byte array onto the stack
         */
        private void pushByteArray(final byte[] bytes) {
            final int length = bytes.length;
            if (length == 0) {
                this.mv.visitInsn(Opcodes.ICONST_0);
            } else if (length == 1) {
                this.mv.visitInsn(Opcodes.ICONST_1);
            } else if (length == 2) {
                this.mv.visitInsn(Opcodes.ICONST_2);
            } else if (length == 3) {
                this.mv.visitInsn(Opcodes.ICONST_3);
            } else if (length == 4) {
                this.mv.visitInsn(Opcodes.ICONST_4);
            } else if (length == 5) {
                this.mv.visitInsn(Opcodes.ICONST_5);
            } else if (length <= 127) {
                this.mv.visitIntInsn(Opcodes.BIPUSH, length);
            } else if (length <= 32767) {
                this.mv.visitIntInsn(Opcodes.SIPUSH, length);
            } else {
                this.mv.visitLdcInsn(length);
            }
            this.mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
            for (int i = 0; i < bytes.length; i++) {
                this.mv.visitInsn(Opcodes.DUP);
                if (i == 0) {
                    this.mv.visitInsn(Opcodes.ICONST_0);
                } else if (i == 1) {
                    this.mv.visitInsn(Opcodes.ICONST_1);
                } else if (i == 2) {
                    this.mv.visitInsn(Opcodes.ICONST_2);
                } else if (i == 3) {
                    this.mv.visitInsn(Opcodes.ICONST_3);
                } else if (i == 4) {
                    this.mv.visitInsn(Opcodes.ICONST_4);
                } else if (i == 5) {
                    this.mv.visitInsn(Opcodes.ICONST_5);
                } else if (i <= 127) {
                    this.mv.visitIntInsn(Opcodes.BIPUSH, i);
                } else if (i <= 32767) {
                    this.mv.visitIntInsn(Opcodes.SIPUSH, i);
                } else {
                    this.mv.visitLdcInsn(i);
                }
                final byte b = bytes[i];
                if (b == 0) {
                    this.mv.visitInsn(Opcodes.ICONST_0);
                } else if (b == 1) {
                    this.mv.visitInsn(Opcodes.ICONST_1);
                } else if (b == 2) {
                    this.mv.visitInsn(Opcodes.ICONST_2);
                } else if (b == 3) {
                    this.mv.visitInsn(Opcodes.ICONST_3);
                } else if (b == 4) {
                    this.mv.visitInsn(Opcodes.ICONST_4);
                } else if (b == 5) {
                    this.mv.visitInsn(Opcodes.ICONST_5);
                } else if (b == -1) {
                    this.mv.visitInsn(Opcodes.ICONST_M1);
                } else {
                    this.mv.visitIntInsn(Opcodes.BIPUSH, b);
                }
                this.mv.visitInsn(Opcodes.I2B);
                this.mv.visitInsn(Opcodes.BASTORE);
            }
        }
    }
}
