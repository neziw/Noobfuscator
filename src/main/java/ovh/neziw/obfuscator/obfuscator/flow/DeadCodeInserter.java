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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Inserts dead code into methods to obfuscate control flow
 * Single Responsibility: Insert stack-neutral dead code patterns
 */
public class DeadCodeInserter {

    private final Random random;

    public DeadCodeInserter(final Random random) {
        this.random = random;
    }

    /**
     * Inserts dead code into a method using EASY mode patterns
     * Uses stack-neutral patterns that don't affect the stack state
     */
    public int insertEasyDeadCode(final MethodNode method) {
        final InsnList instructions = method.instructions;
        if (instructions.size() < 3) {
            return 0; // Too small to obfuscate
        }

        int addedCount = 0;
        final List<AbstractInsnNode> insertionPoints = this.findSafeInsertionPoints(instructions);

        // Add dead code to a limited number of safe locations
        final int numInsertions = Math.min(Math.max(1, insertionPoints.size() / 8), 5);
        Collections.shuffle(insertionPoints, this.random);

        for (int i = 0; i < numInsertions && i < insertionPoints.size(); i++) {
            final AbstractInsnNode target = insertionPoints.get(i);

            // Insert a stack-neutral pattern AFTER the target instruction
            final AbstractInsnNode next = target.getNext();
            if (next == null) {
                // Can't insert after last instruction safely
                continue;
            }

            final InsnList obfuscation = this.createStackNeutralPattern();
            instructions.insertBefore(next, obfuscation);

            addedCount += 5;
        }

        return addedCount;
    }

    /**
     * Inserts dead code into a method using HEAVY mode patterns
     * Adds more complex dead code than EASY mode
     */
    public int insertHeavyDeadCode(final MethodNode method) {
        // First apply EASY obfuscation
        int addedCount = this.insertEasyDeadCode(method);

        final InsnList instructions = method.instructions;
        if (instructions.size() < 5) {
            return addedCount;
        }

        final List<AbstractInsnNode> insertionPoints = this.findSafeInsertionPoints(instructions);

        // Add more insertions than EASY mode, but still use safe patterns
        final int insertions = Math.min(Math.max(3, insertionPoints.size() / 4), 15);
        Collections.shuffle(insertionPoints, this.random);

        for (int i = 0; i < insertions && i < insertionPoints.size(); i++) {
            final AbstractInsnNode target = insertionPoints.get(i);
            final AbstractInsnNode next = target.getNext();
            if (next == null) {
                // Can't insert after last instruction safely
                continue;
            }

            final InsnList obfuscation = this.createHeavyStackNeutralPattern();
            instructions.insertBefore(next, obfuscation);

            addedCount += 7;
        }

        return addedCount;
    }

    /**
     * Finds safe insertion points in the instruction list
     * Only after instructions that don't leave values on the stack
     */
    private List<AbstractInsnNode> findSafeInsertionPoints(final InsnList instructions) {
        final List<AbstractInsnNode> insertionPoints = new ArrayList<>();

        for (final AbstractInsnNode insn : instructions) {
            final int type = insn.getType();
            if (type == AbstractInsnNode.LABEL ||
                type == AbstractInsnNode.LINE ||
                type == AbstractInsnNode.FRAME) {
                continue;
            }

            final int opcode = insn.getOpcode();
            // Skip returns, jumps, and switches
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                continue;
            }
            if (opcode >= Opcodes.IFEQ && opcode <= Opcodes.IF_ACMPNE) {
                continue;
            }
            if (opcode == Opcodes.GOTO || opcode == Opcodes.JSR ||
                opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH) {
                continue;
            }

            // Skip instructions with complex stack manipulation
            if (opcode == Opcodes.DUP || opcode == Opcodes.DUP_X1 || opcode == Opcodes.DUP_X2 ||
                opcode == Opcodes.DUP2 || opcode == Opcodes.DUP2_X1 || opcode == Opcodes.DUP2_X2 ||
                opcode == Opcodes.SWAP || opcode == Opcodes.POP || opcode == Opcodes.POP2) {
                continue;
            }

            // Only insert after instructions that DON'T leave values on stack
            // Stores (ISTORE=54 to ASTORE_3=78) - consume value, leave nothing on stack
            if (opcode >= 54 && opcode <= 78) { // ISTORE to ASTORE_3
                insertionPoints.add(insn);
            }
        }

        return insertionPoints;
    }

    /**
     * Creates a stack-neutral dead code pattern: push constant -> if false -> dead code -> label -> pop
     * This is guaranteed stack-neutral: net stack change is 0
     */
    private InsnList createStackNeutralPattern() {
        final LabelNode skipLabel = new LabelNode();
        final InsnList obfuscation = new InsnList();
        obfuscation.add(new InsnNode(Opcodes.ICONST_1)); // Push 1 (stack: +1)
        obfuscation.add(new JumpInsnNode(Opcodes.IFEQ, skipLabel)); // If 0 (never true, consumes 1, stack: 0)
        // Dead code (unreachable, stack doesn't matter)
        obfuscation.add(new InsnNode(Opcodes.NOP)); // No-op
        obfuscation.add(skipLabel); // Label (stack: 0)
        obfuscation.add(new InsnNode(Opcodes.POP)); // Pop 1 (stack: -1, net: 0)
        return obfuscation;
    }

    /**
     * Creates a heavier stack-neutral dead code pattern with more no-ops
     */
    private InsnList createHeavyStackNeutralPattern() {
        final LabelNode skipLabel = new LabelNode();
        final InsnList obfuscation = new InsnList();
        obfuscation.add(new InsnNode(Opcodes.ICONST_1)); // Push 1 (stack: +1)
        obfuscation.add(new JumpInsnNode(Opcodes.IFEQ, skipLabel)); // If 0 (never, consumes 1, stack: 0)
        // Dead code (unreachable)
        obfuscation.add(new InsnNode(Opcodes.NOP)); // No-op
        obfuscation.add(new InsnNode(Opcodes.NOP)); // More no-ops
        obfuscation.add(new InsnNode(Opcodes.NOP));
        obfuscation.add(skipLabel); // Label (stack: 0)
        obfuscation.add(new InsnNode(Opcodes.POP)); // Pop 1 (stack: -1, net: 0)
        return obfuscation;
    }
}

