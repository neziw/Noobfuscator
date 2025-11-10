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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.objectweb.asm.Type;

/**
 * Obfuscates type signatures (class names in signatures)
 * Single Responsibility: Obfuscate class names in type signatures
 */
public class SignatureObfuscator {

    private static final Logger LOGGER = Logger.getLogger(SignatureObfuscator.class.getName());

    private final ClassNameMapper classNameMapper;

    public SignatureObfuscator(final ClassNameMapper classNameMapper) {
        this.classNameMapper = classNameMapper;
    }

    /**
     * Obfuscates type signatures
     * Uses simple regex replacement for class names in signatures
     */
    public String obfuscateSignature(final String signature) {
        if (signature == null || signature.isEmpty()) {
            return signature;
        }
        // Use simple regex to find and replace class names in signatures
        // contain class names in format: Lfully/qualified/ClassName;
        final Pattern pattern = Pattern.compile("L([^;<>:]+);");
        final Matcher matcher = pattern.matcher(signature);
        final StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            final String className = matcher.group(1);
            final String obfuscated = this.classNameMapper.obfuscateClassName(className);
            if (!obfuscated.equals(className)) {
                matcher.appendReplacement(sb, "L" + obfuscated.replace(".", "/") + ";");
            } else {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Obfuscates a type descriptor (field type)
     */
    public String obfuscateTypeDescriptor(final String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return descriptor;
        }
        try {
            final Type type = Type.getType(descriptor);
            return this.remapType(type).getDescriptor();
        } catch (final Exception exception) {
            // If parsing fails, return original descriptor
            LOGGER.warning("Failed to parse type descriptor: " + descriptor + ", error: " + exception.getMessage());
            return descriptor;
        }
    }

    /**
     * Obfuscates a method descriptor
     */
    public String obfuscateMethodDescriptor(final String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return descriptor;
        }
        // Validate descriptor format - must start with '(' and contain ')'
        if (!descriptor.startsWith("(") || descriptor.length() < 3) {
            return descriptor;
        }
        final int closingParen = descriptor.indexOf(')', 1);
        if (closingParen < 0 || closingParen == descriptor.length() - 1) {
            return descriptor;
        }
        try {
            final Type methodType = Type.getMethodType(descriptor);
            final Type returnType = this.remapType(methodType.getReturnType());
            final Type[] argumentTypes = methodType.getArgumentTypes();
            final Type[] obfuscatedArgs = new Type[argumentTypes.length];
            for (int i = 0; i < argumentTypes.length; i++) {
                obfuscatedArgs[i] = this.remapType(argumentTypes[i]);
            }
            return Type.getMethodType(returnType, obfuscatedArgs).getDescriptor();
        } catch (final Exception exception) {
            LOGGER.warning("Failed to parse method descriptor: " + descriptor + ", error: " + exception.getMessage());
            return descriptor;
        }
    }

    /**
     * Remaps a type, obfuscating class names if needed
     */
    private Type remapType(final Type type) {
        if (type == null) {
            return type;
        }
        try {
            switch (type.getSort()) {
                case Type.OBJECT:
                    final String obfuscatedInternalName = this.classNameMapper.obfuscateClassName(type.getInternalName());
                    return Type.getObjectType(obfuscatedInternalName);
                case Type.ARRAY:
                    final Type elementType = this.remapType(type.getElementType());
                    if (elementType != null) {
                        return Type.getType("[" + elementType.getDescriptor());
                    }
                    return type;
                default:
                    return type; // Primitive types, etc.
            }
        } catch (final Exception exception) {
            LOGGER.warning("Failed to remap type: " + type + ", error: " + exception.getMessage());
            return type;
        }
    }
}

