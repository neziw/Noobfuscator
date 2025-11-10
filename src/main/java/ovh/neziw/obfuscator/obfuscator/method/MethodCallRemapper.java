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

import java.util.Map;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Remaps method calls in bytecode to use obfuscated names
 * Single Responsibility: Remap method call instructions to obfuscated names
 */
public class MethodCallRemapper {

    private final Map<String, Map<String, String>> globalMethodNameMap;
    private final Map<String, String> classNameMap;

    public MethodCallRemapper(final Map<String, Map<String, String>> globalMethodNameMap,
                              final Map<String, String> classNameMap) {
        this.globalMethodNameMap = globalMethodNameMap;
        this.classNameMap = classNameMap;
    }

    /**
     * Remaps all method calls in a method's bytecode
     */
    public void remapMethodCalls(final MethodNode method) {
        if (method.instructions == null) {
            return;
        }
        for (final AbstractInsnNode insn : method.instructions) {
            if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                this.remapMethodInsn((MethodInsnNode) insn);
            } else if (insn.getType() == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
                this.remapInvokeDynamic((InvokeDynamicInsnNode) insn);
            }
        }
    }

    /**
     * Remaps a method instruction
     */
    private void remapMethodInsn(final MethodInsnNode methodInsn) {
        final String ownerClass = methodInsn.owner;
        final String methodKey = methodInsn.name + methodInsn.desc;

        Map<String, String> ownerMethodMap = this.globalMethodNameMap.get(ownerClass);
        if (ownerMethodMap != null && ownerMethodMap.containsKey(methodKey)) {
            methodInsn.name = ownerMethodMap.get(methodKey);
        } else if (this.classNameMap != null) {
            final String originalOwnerClass = this.findOriginalClassName(ownerClass);
            if (originalOwnerClass != null) {
                ownerMethodMap = this.globalMethodNameMap.get(originalOwnerClass);
                if (ownerMethodMap != null && ownerMethodMap.containsKey(methodKey)) {
                    methodInsn.name = ownerMethodMap.get(methodKey);
                }
            }
        }
    }

    /**
     * Remaps an InvokeDynamic instruction
     */
    private void remapInvokeDynamic(final InvokeDynamicInsnNode invokeDynamic) {
        // Remap bootstrap method handle if it's a method in an obfuscated class
        if (invokeDynamic.bsm != null) {
            this.remapHandle(invokeDynamic.bsm);
        }
        // Remap bootstrap arguments (Handles)
        if (invokeDynamic.bsmArgs != null) {
            for (int i = 0; i < invokeDynamic.bsmArgs.length; i++) {
                final Object arg = invokeDynamic.bsmArgs[i];
                if (arg instanceof Handle) {
                    final Handle remappedHandle = this.remapHandle((Handle) arg);
                    if (remappedHandle != arg) {
                        invokeDynamic.bsmArgs[i] = remappedHandle;
                    }
                }
            }
        }
    }

    /**
     * Remaps a Handle (method reference)
     */
    private Handle remapHandle(final Handle handle) {
        final String ownerClass = handle.getOwner();
        final String methodKey = handle.getName() + handle.getDesc();
        Map<String, String> ownerMethodMap = this.globalMethodNameMap.get(ownerClass);
        if (ownerMethodMap == null && this.classNameMap != null) {
            final String originalOwnerClass = this.findOriginalClassName(ownerClass);
            if (originalOwnerClass != null) {
                ownerMethodMap = this.globalMethodNameMap.get(originalOwnerClass);
            }
        }
        if (ownerMethodMap != null && ownerMethodMap.containsKey(methodKey)) {
            return new Handle(
                handle.getTag(),
                handle.getOwner(), // Keep original owner (will be remapped by ClassNameObfuscator if needed)
                ownerMethodMap.get(methodKey),
                handle.getDesc(),
                handle.isInterface()
            );
        }
        return handle;
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
}

