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
package ovh.neziw.obfuscator.obfuscator.flow;

import java.util.logging.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Calculates safe maxLocals values for methods
 * Single Responsibility: Calculate and validate maxLocals
 */
public final class MaxLocalsCalculator {

    private static final Logger LOGGER = Logger.getLogger(MaxLocalsCalculator.class.getName());

    /**
     * Calculates a safe maxLocals value for a method based on its signature
     */
    public static int calculateSafeMaxLocals(final int access, final String descriptor) {
        try {
            final Type[] argTypes = Type.getArgumentTypes(descriptor);
            int minLocals = 0;
            for (final Type argType : argTypes) {
                minLocals += argType.getSize();
            }
            if ((access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0) {
                minLocals += 1; // 'this'
            }
            return Math.max(1, minLocals + 5); // Add buffer
        } catch (final Exception exception) {
            return (access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0 ? 10 : 5;
        }
    }

    /**
     * Calculates a safe maxLocals value for a MethodNode
     * This method analyzes the actual method to determine the correct maxLocals
     */
    public static int calculateSafeMaxLocalsForMethod(final MethodNode method) {
        int calculated;
        try {
            final Type[] argTypes = Type.getArgumentTypes(method.desc);
            int paramSlots = 0;
            for (final Type argType : argTypes) {
                paramSlots += argType.getSize();
            }
            if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0) {
                paramSlots += 1; // 'this'
            }
            calculated = paramSlots;
            if (method.instructions != null && method.instructions.size() > 0) {
                int maxLocalIndex = -1;
                for (final AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof final VarInsnNode varInsn) {
                        maxLocalIndex = Math.max(maxLocalIndex, varInsn.var);
                    } else if (insn instanceof final IincInsnNode iincInsn) {
                        maxLocalIndex = Math.max(maxLocalIndex, iincInsn.var);
                    }
                }

                if (maxLocalIndex >= 0) {
                    calculated = Math.max(calculated, maxLocalIndex + 1);
                }
            }
            calculated = calculated + 10;
            if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0) {
                calculated = Math.max(1, calculated);
            } else {
                calculated = Math.max(0, calculated);
            }
        } catch (final Exception exception) {
            calculated = (method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0 ? 20 : 10;
        }
        return calculated;
    }

    /**
     * Fixes maxLocals for a single method
     * This method is VERY defensive - it always sets a safe, valid maxLocals value
     */
    public static void fixMaxLocals(final MethodNode method) {
        int calculatedMaxLocals = -1;
        try {
            final Type[] argTypes = Type.getArgumentTypes(method.desc);
            int paramSlots = 0;
            for (final Type argType : argTypes) {
                paramSlots += argType.getSize();
            }
            if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0) {
                paramSlots += 1;
            }

            calculatedMaxLocals = paramSlots;
            if (method.instructions != null && method.instructions.size() > 0) {
                int maxLocalIndex = -1;
                for (final AbstractInsnNode insn : method.instructions) {
                    if (insn instanceof final VarInsnNode varInsn) {
                        maxLocalIndex = Math.max(maxLocalIndex, varInsn.var);
                    } else if (insn instanceof final IincInsnNode iincInsn) {
                        maxLocalIndex = Math.max(maxLocalIndex, iincInsn.var);
                    }
                }
                // Use the maximum of parameter slots and actual usage
                if (maxLocalIndex >= 0) {
                    calculatedMaxLocals = Math.max(calculatedMaxLocals, maxLocalIndex + 1);
                }
            }
            calculatedMaxLocals = Math.max(calculatedMaxLocals, paramSlots) + 5;
            if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0) {
                // Non-static: at least 1 (for 'this'), but use calculated if larger
                calculatedMaxLocals = Math.max(1, calculatedMaxLocals);
            } else {
                // Static: at least 0, but use calculated if larger
                calculatedMaxLocals = Math.max(0, calculatedMaxLocals);
            }

        } catch (final Exception exception) {
            // If anything fails, use very safe defaults
            if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0) {
                calculatedMaxLocals = 20;
            } else {
                calculatedMaxLocals = 10;
            }
        }
        method.maxLocals = calculatedMaxLocals;
    }

    /**
     * Checks if all methods in the class have valid maxLocals
     */
    public static boolean allMethodsNotHaveValidMaxLocals(final org.objectweb.asm.tree.ClassNode classNode) {
        if (classNode.methods == null) {
            return false;
        }
        for (final MethodNode method : classNode.methods) {
            // Skip abstract and native methods (they don't have code)
            if ((method.access & (org.objectweb.asm.Opcodes.ACC_ABSTRACT | org.objectweb.asm.Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            // Check if maxLocals is valid
            if (method.maxLocals < 0) {
                return true;
            }
            // For non-static methods, maxLocals should be at least 1 (for 'this')
            if ((method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0 && method.maxLocals == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates and fixes maxLocals for all methods in the class
     * This prevents NegativeArraySizeException during frame calculation
     */
    public static void validateAndFixMaxLocals(final org.objectweb.asm.tree.ClassNode classNode) {
        if (classNode.methods == null) {
            return;
        }
        for (final MethodNode method : classNode.methods) {
            if ((method.access & (org.objectweb.asm.Opcodes.ACC_ABSTRACT | org.objectweb.asm.Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }
            // AGGRESSIVE FIX: Always fix maxLocals for methods with code
            // This ensures we never have invalid maxLocals values
            final int oldMaxLocals = method.maxLocals;
            fixMaxLocals(method);
            if (oldMaxLocals != method.maxLocals) {
                LOGGER.fine("Fixed maxLocals for method " + method.name +
                    " in class " + classNode.name +
                    " from " + oldMaxLocals + " to " + method.maxLocals);
            }
            if (method.maxLocals < 0) {
                LOGGER.severe("Failed to fix maxLocals for method " + method.name +
                    " in class " + classNode.name + ", maxLocals is still " + method.maxLocals);
                method.maxLocals = (method.access & org.objectweb.asm.Opcodes.ACC_STATIC) == 0 ? 10 : 5;
            }
        }
    }
}

