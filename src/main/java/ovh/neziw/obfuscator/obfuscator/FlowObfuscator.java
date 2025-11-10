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

import java.util.Random;
import java.util.logging.Logger;
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
import ovh.neziw.obfuscator.obfuscator.flow.EasyFlowStrategy;
import ovh.neziw.obfuscator.obfuscator.flow.FlowObfuscationStrategy;
import ovh.neziw.obfuscator.obfuscator.flow.HeavyFlowStrategy;
import ovh.neziw.obfuscator.obfuscator.flow.MaxLocalsCalculator;

/**
 * Obfuscates control flow in class files to make code less readable
 * EASY: Adds simple dead code, unnecessary jumps, and fake conditions
 * HEAVY: Adds complex control flow structures, fake try-catch blocks, and overlapping ranges
 * Single Responsibility: Coordinate flow obfuscation for a class
 */
public class FlowObfuscator extends ClassVisitor {

    private static final Logger LOGGER = Logger.getLogger(FlowObfuscator.class.getName());

    private final PatternMatcher patternMatcher;
    private final ObfuscationStats stats;
    private final String mode; // "EASY" or "HEAVY"
    private final FlowObfuscationStrategy strategy;
    private boolean shouldObfuscate;
    private ClassNode classNode;

    public FlowObfuscator(final ClassVisitor cv, final PatternMatcher patternMatcher, final ObfuscationStats stats, final String mode) {
        super(Opcodes.ASM9, cv);
        this.patternMatcher = patternMatcher;
        this.stats = stats;
        this.mode = mode;
        final Random random = new Random();
        if ("EASY".equals(mode)) {
            this.strategy = new EasyFlowStrategy(random);
        } else if ("HEAVY".equals(mode)) {
            this.strategy = new HeavyFlowStrategy(random);
        } else {
            this.strategy = null;
        }
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        this.shouldObfuscate = this.patternMatcher.matches(name);
        if (this.shouldObfuscate && !"NONE".equals(this.mode)) {
            // Use ClassNode to collect all methods and modify their control flow
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
            final MethodVisitor mv = this.classNode.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMaxs(final int maxStack, final int maxLocals) {
                    if (maxLocals < 0) {
                        final int safeMaxLocals = MaxLocalsCalculator.calculateSafeMaxLocals(access, descriptor);
                        super.visitMaxs(maxStack, safeMaxLocals);
                    } else if (maxLocals == 0 && (access & Opcodes.ACC_STATIC) == 0) {
                        super.visitMaxs(maxStack, 1);
                    } else {
                        super.visitMaxs(maxStack, maxLocals);
                    }
                }
            };
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    @Override
    public void visitEnd() {
        if (this.shouldObfuscate && this.classNode != null && !"NONE".equals(this.mode)) {
            try {
                // FIRST: Validate and fix maxLocals for ALL methods in the class
                // This must be done before any modifications to prevent frame calculation errors
                MaxLocalsCalculator.validateAndFixMaxLocals(this.classNode);
                // Verify all methods have valid maxLocals before proceeding
                if (MaxLocalsCalculator.allMethodsNotHaveValidMaxLocals(this.classNode)) {
                    LOGGER.warning("Class " + this.classNode.name +
                        " has methods with invalid maxLocals, skipping flow obfuscation");
                    // Skip flow obfuscation but still pass through the class with maxLocals fixing
                    this.classNode.accept(new MaxLocalsFixingClassVisitor(this.cv));
                    return;
                }
                // Process methods and obfuscate control flow
                this.obfuscateMethods();
                // SECOND: Validate again after modifications (in case something went wrong)
                MaxLocalsCalculator.validateAndFixMaxLocals(this.classNode);
                // Verify again before accepting
                if (MaxLocalsCalculator.allMethodsNotHaveValidMaxLocals(this.classNode)) {
                    LOGGER.warning("Class " + this.classNode.name +
                        " has methods with invalid maxLocals after obfuscation, skipping flow obfuscation");
                    // Skip flow obfuscation but still pass through the class with maxLocals fixing
                    this.classNode.accept(new MaxLocalsFixingClassVisitor(this.cv));
                    return;
                }
                // Visit the class node with obfuscated control flow
                // ClassWriter with COMPUTE_FRAMES will recalculate frames automatically
                // FINAL CHECK: Force fix ALL methods one more time and verify
                MaxLocalsCalculator.validateAndFixMaxLocals(this.classNode);
                // Additional safety: check each method individually and force fix if needed
                if (this.classNode.methods != null) {
                    for (final org.objectweb.asm.tree.MethodNode method : this.classNode.methods) {
                        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0 &&
                            method.instructions != null && method.instructions.size() > 0) {
                            // Force fix maxLocals one more time
                            if (method.maxLocals < 1) {
                                MaxLocalsCalculator.fixMaxLocals(method);
                            }
                            // Final safety check
                            if (method.maxLocals < 1) {
                                method.maxLocals = (method.access & Opcodes.ACC_STATIC) == 0 ? 20 : 10;
                            }
                        }
                    }
                }
                if (MaxLocalsCalculator.allMethodsNotHaveValidMaxLocals(this.classNode)) {
                    LOGGER.warning("Class " + this.classNode.name +
                        " still has invalid maxLocals after all fixes, skipping flow obfuscation");
                    // Use MaxLocalsFixingClassVisitor to fix maxLocals during acceptance
                    this.classNode.accept(new MaxLocalsFixingClassVisitor(this.cv));
                    return;
                }
                // CRITICAL: Before accepting, FORCE fix ALL methods with VERY safe maxLocals values
                // MethodNode.accept() reads method.maxLocals directly, so we MUST fix it in the MethodNode
                // This is the ONLY way to prevent NegativeArraySizeException
                if (this.classNode.methods != null) {
                    for (final org.objectweb.asm.tree.MethodNode method : this.classNode.methods) {
                        // Fix ALL methods with code, regardless of current maxLocals value
                        if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                            if (method.instructions != null && method.instructions.size() > 0) {
                                // Calculate safe maxLocals based on method signature and usage
                                method.maxLocals = MaxLocalsCalculator.calculateSafeMaxLocalsForMethod(method);
                            } else {
                                method.maxLocals = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
                            }
                        }
                    }
                }
                this.classNode.accept(new MaxLocalsFixingClassVisitor(this.cv));
            } catch (final Exception exception) {
                final boolean isFrameError = exception.getCause() instanceof NegativeArraySizeException ||
                    (exception.getMessage() != null && (exception.getMessage().contains("-1") ||
                        exception.getMessage().contains("NegativeArraySize") || exception.getMessage().contains("Frame")));
                if (isFrameError) {
                    LOGGER.warning("Flow obfuscation failed for class " +
                        (this.classNode.name != null ? this.classNode.name : "unknown") +
                        " due to frame calculation error. Flow obfuscation will be skipped for this class.");
                    try {
                        if (this.classNode.methods != null) {
                            for (final org.objectweb.asm.tree.MethodNode method : this.classNode.methods) {
                                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                                    // Set very high maxLocals to avoid any issues
                                    method.maxLocals = 100;
                                }
                            }
                        }
                        this.classNode.accept(new MaxLocalsFixingClassVisitor(this.cv));
                    } catch (final Exception exception2) {
                        // If even that fails, we give up - let JarProcessor use original class
                        LOGGER.severe("Unable to fix frame calculation issues for class " +
                            (this.classNode.name != null ? this.classNode.name : "unknown") +
                            ". Original class will be used without flow obfuscation.");
                        throw new RuntimeException("Flow obfuscation skipped: " + exception.getMessage(), exception);
                    }
                } else {
                    // For other errors, rethrow
                    throw new RuntimeException("Failed to write obfuscated class: " + exception.getMessage(), exception);
                }
            }
        } else {
            super.visitEnd();
        }
    }

    /**
     * Obfuscates control flow in all methods
     */
    private void obfuscateMethods() {
        if (this.classNode.methods == null) {
            return;
        }
        for (final MethodNode method : this.classNode.methods) {
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                continue;
            }
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                continue;
            }
            if (method.instructions == null || method.instructions.size() == 0) {
                continue;
            }

            // Validate and fix maxLocals BEFORE any modifications
            // This prevents NegativeArraySizeException during frame calculation
            if (method.maxLocals < 0 || (method.maxLocals == 0 && (method.access & Opcodes.ACC_STATIC) == 0)) {
                MaxLocalsCalculator.fixMaxLocals(method);
            }
            try {
                if (this.strategy != null) {
                    final int addedCount = this.strategy.obfuscate(method);
                    this.stats.incrementInstructionsAdded(addedCount);
                }
                method.maxStack = 0;
                MaxLocalsCalculator.fixMaxLocals(method);
                if (method.localVariables != null) {
                    method.localVariables.clear();
                }
                if (method.visibleLocalVariableAnnotations != null) {
                    method.visibleLocalVariableAnnotations.clear();
                }
                if (method.invisibleLocalVariableAnnotations != null) {
                    method.invisibleLocalVariableAnnotations.clear();
                }

                this.stats.incrementMethodsObfuscated();
            } catch (final Exception exception) {
                // If obfuscation fails for this method, skip it
                LOGGER.warning("Failed to obfuscate flow in method " + method.name + ": " + exception.getMessage());
                LOGGER.throwing(FlowObfuscator.class.getName(), "obfuscateMethods", exception);
            }
        }
    }

    /**
     * Statistics for flow obfuscation
     */
    @Getter
    public static class ObfuscationStats {

        private int methodsObfuscated = 0;
        private int instructionsAdded = 0;

        public void incrementMethodsObfuscated() {
            this.methodsObfuscated++;
        }

        public void incrementInstructionsAdded(final int count) {
            this.instructionsAdded += count;
        }

    }

    /**
     * ClassVisitor that fixes maxLocals in MethodNode before accepting
     * The problem is that MethodNode.accept() uses method.maxLocals directly,
     * so we need to fix it in the MethodNode itself, not in visitMaxs.
     * However, we can't easily modify MethodNode after it's been created.
     * Instead, we intercept the visitMaxs call and fix it there.
     * But the real issue is that MethodNode.accept() may call visitMaxs with -1
     * before our visitor can intercept it.
     * <p>
     * Solution: We need to manually iterate through methods and fix maxLocals
     * BEFORE calling classNode.accept(). This is already done in validateAndFixMaxLocals().
     * But we also need to intercept visitMaxs as a safety net.
     */
    private static class MaxLocalsFixingClassVisitor extends ClassVisitor {

        public MaxLocalsFixingClassVisitor(final ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                                         final String signature, final String[] exceptions) {
            final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMaxs(final int maxStack, final int maxLocals) {
                    // Fix maxLocals if it's invalid (shouldn't happen if we fixed it in MethodNode)
                    int fixedMaxLocals = maxLocals;
                    if (maxLocals < 0) {
                        fixedMaxLocals = MaxLocalsCalculator.calculateSafeMaxLocals(access, descriptor);
                        LOGGER.warning("Fixed negative maxLocals in visitMaxs for method " + name);
                    } else if (maxLocals == 0 && (access & Opcodes.ACC_STATIC) == 0) {
                        fixedMaxLocals = 1;
                    } else if (maxLocals < 10) {
                        // Ensure minimum safe value
                        final int safeMin = MaxLocalsCalculator.calculateSafeMaxLocals(access, descriptor);
                        fixedMaxLocals = Math.max(maxLocals, safeMin);
                    }
                    super.visitMaxs(maxStack, fixedMaxLocals);
                }
            };
        }
    }
}
