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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Generates a class file designed to crash decompilers
 * This class contains various problematic constructs that many decompilers cannot handle
 */
public final class CrashClassGenerator {

    private static final String CRASH_CLASS_NAME = "ClassFileFormatViolation"; // TODO: make configurable

    /**
     * Generates a problematic class file that can crash decompilers
     *
     * @return byte array representing the class file
     */
    public static byte[] generateCrashClass() {
        final ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, CRASH_CLASS_NAME, null,
            "java/lang/Object", null);
        final AnnotationVisitor av = cw.visitAnnotation("L" + generateLongName(500) + ";", true);
        av.visit("value", generateLongName(1000));
        av.visitEnd();
        for (int i = 0; i < 5; i++) {
            final FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "field" + generateLongName(100 + i * 50), "Ljava/lang/Object;",
                generateProblematicSignature(), null);
            final AnnotationVisitor fav = fv.visitAnnotation("L" + generateLongName(300) + ";", false);
            if (fav != null) {
                fav.visitEnd();
            }
            fv.visitEnd();
        }
        for (int i = 0; i < 10; i++) {
            addProblematicMethod(cw, "method" + i + generateLongName(50 + i * 10));
        }
        addMethodWithInvalidStackMap(cw, "complexMethod" + generateLongName(100));
        addMethodWithComplexControlFlow(cw, "nestedMethod" + generateLongName(100));
        addMethodWithProblematicExceptions(cw, "exceptionMethod" + generateLongName(100));
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generates a long name that can cause issues
     */
    private static String generateLongName(final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        return sb.toString();
    }

    /**
     * Generates a problematic generic signature
     */
    private static String generateProblematicSignature() {
        return "<T::Ljava/lang/Object;U::Ljava/lang/Object;V::Ljava/lang/Object;>Ljava/lang/Object;";
    }

    /**
     * Adds a method with problematic exception declarations
     */
    private static void addMethodWithProblematicExceptions(final ClassWriter cw, final String methodName) {
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            methodName, "()V", null,
            new String[] {"java/lang/Exception", "java/lang/RuntimeException", "java/lang/Error"});
        mv.visitCode();
        final Label l0 = new Label();
        final Label l1 = new Label();
        final Label l2 = new Label();
        mv.visitLabel(l0);
        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(l1);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(l2);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Exception"});
        mv.visitVarInsn(Opcodes.ASTORE, 0);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Adds a method with problematic bytecode
     */
    private static void addProblematicMethod(final ClassWriter cw, final String methodName) {
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            methodName, "()V", null, null);
        mv.visitCode();
        final Label start = new Label();
        final Label end = new Label();
        final Label middle = new Label();
        mv.visitLabel(start);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 0);
        mv.visitLabel(middle);
        mv.visitIincInsn(0, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitIntInsn(Opcodes.BIPUSH, 10);
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, middle);
        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLocalVariable("i", "I", null, start, end, 0);
        mv.visitLineNumber(1, start);
        mv.visitLineNumber(2, middle);
        mv.visitLineNumber(3, end);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /**
     * Adds a method with invalid stack map frames (can crash some decompilers)
     */
    private static void addMethodWithInvalidStackMap(final ClassWriter cw, final String methodName) {
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            methodName, "()V", null, null);
        mv.visitCode();
        final Label l0 = new Label();
        final Label l1 = new Label();
        final Label l2 = new Label();
        mv.visitLabel(l0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 1);
        mv.visitLabel(l1);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitIntInsn(Opcodes.BIPUSH, 5);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, l2);
        mv.visitIincInsn(1, 1);
        mv.visitJumpInsn(Opcodes.GOTO, l1);
        mv.visitLabel(l2);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * Adds a method with complex control flow
     */
    private static void addMethodWithComplexControlFlow(final ClassWriter cw, final String methodName) {
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            methodName, "()V", null, null);
        mv.visitCode();
        final Label[] labels = new Label[10];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }
        mv.visitLabel(labels[0]);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 0);
        for (int i = 1; i < labels.length - 1; i++) {
            mv.visitLabel(labels[i]);
            mv.visitVarInsn(Opcodes.ILOAD, 0);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, labels[i + 1]);
            mv.visitIincInsn(0, 1);
            mv.visitJumpInsn(Opcodes.GOTO, labels[i]);
        }
        mv.visitLabel(labels[labels.length - 1]);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /**
     * Gets the class name of the crash class
     */
    public static String getCrashClassName() {
        return CRASH_CLASS_NAME + ".class";
    }
}
