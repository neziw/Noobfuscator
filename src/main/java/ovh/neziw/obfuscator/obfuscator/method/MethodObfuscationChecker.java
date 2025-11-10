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
package ovh.neziw.obfuscator.obfuscator.method;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import ovh.neziw.obfuscator.obfuscator.NameGenerator;

/**
 * Checks if a method should be obfuscated
 * Single Responsibility: Determine if a method should be obfuscated based on various criteria
 */
public class MethodObfuscationChecker {

    private final NameGenerator nameGenerator;
    private final boolean isInterface;

    public MethodObfuscationChecker(final NameGenerator nameGenerator, final boolean isInterface) {
        this.nameGenerator = nameGenerator;
        this.isInterface = isInterface;
    }

    /**
     * Determines if a method should be obfuscated
     */
    public boolean shouldObfuscateMethod(final MethodNode method) {
        final String name = method.name;
        final int access = method.access;
        if (name.equals("<init>") || name.equals("<clinit>")) {
            return false;
        }
        if (this.nameGenerator.isObfuscated(name)) {
            return false;
        }
        if (this.isInterface) {
            return false;
        }
        if (this.hasOverrideAnnotation(method)) {
            return false;
        }
        final boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
        final boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;
        return !isSynthetic && !isBridge;
    }

    /**
     * Checks if a method has @Override annotation
     */
    private boolean hasOverrideAnnotation(final MethodNode method) {
        if (method.visibleAnnotations != null || method.invisibleAnnotations != null) {
            final List<AnnotationNode> allAnnotations = new ArrayList<>();
            if (method.visibleAnnotations != null) {
                allAnnotations.addAll(method.visibleAnnotations);
            }
            if (method.invisibleAnnotations != null) {
                allAnnotations.addAll(method.invisibleAnnotations);
            }
            for (final AnnotationNode annotation : allAnnotations) {
                if ("Ljava/lang/Override;".equals(annotation.desc)) {
                    return true;
                }
            }
        }
        return false;
    }
}

