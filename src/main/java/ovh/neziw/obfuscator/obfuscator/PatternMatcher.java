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

import java.util.List;

/**
 * Matches class names against include patterns
 */
public class PatternMatcher {

    private final List<String> includePatterns;

    public PatternMatcher(final List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    /**
     * Gets the simple class name from a fully qualified name
     */
    public static String getSimpleClassName(final String className) {
        final int lastSlash = className.lastIndexOf('/');
        if (lastSlash >= 0) {
            return className.substring(lastSlash + 1);
        }
        final int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            return className.substring(lastDot + 1);
        }
        return className;
    }

    /**
     * Gets the package name from a fully qualified class name
     */
    public static String getPackageName(final String className) {
        final int lastSlash = className.lastIndexOf('/');
        if (lastSlash >= 0) {
            return className.substring(0, lastSlash);
        }
        final int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            return className.substring(0, lastDot);
        }
        return "";
    }

    /**
     * Checks if a class name matches any of the include patterns
     *
     * @param className Fully qualified class name in internal format (e.g., "com/example/TestClass")
     * @return true if the class should be obfuscated
     */
    public boolean matches(final String className) {
        if (this.includePatterns.isEmpty()) {
            return false; // If no patterns, don't obfuscate anything
        }
        // Convert class name from internal format (/) to package format (.)
        final String normalizedClassName = className.replace('/', '.');
        for (final String pattern : this.includePatterns) {
            if (this.matchesPattern(normalizedClassName, pattern.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a class name matches a specific pattern
     * <p>
     * Patterns:
     * - "com.example.*" - matches com.example package and all subpackages/classes
     * - "com.example.test.*" - matches com.example.test package and all subpackages/classes
     * - "com.test" - matches only classes directly in com.test package (not subpackages)
     * - "com.example.TestClass" - matches only the specific class
     */
    private boolean matchesPattern(final String className, final String pattern) {
        if (pattern.endsWith(".*")) {
            // Package wildcard pattern: "com.example.*"
            final String packagePrefix = pattern.substring(0, pattern.length() - 2);
            // Handle empty package prefix (match all classes)
            if (packagePrefix.isEmpty()) {
                return true;
            }
            // Check if className starts with packagePrefix followed by a dot
            // This handles both direct package classes and subpackages
            // Examples:
            // - "com.example.*" matches "com.example.TestClass"
            // - "com.example.*" matches "com.example.sub.OtherClass"
            // - "com.example.*" matches "com.example.sub.deep.MoreClass"
            if (className.startsWith(packagePrefix + ".")) {
                return true;
            }
            // Also check if the package name exactly matches (for package-info classes)
            final String classPackage = this.getPackageFromClassName(className);
            return classPackage.equals(packagePrefix);
        } else if (pattern.contains(".")) {
            // Check if it's an exact class match (e.g., "com.example.TestClass")
            if (className.equals(pattern)) {
                return true;
            }
            // Check if it's a package pattern without wildcard (e.g., "com.test")
            // This should match classes directly in that package, but not subpackages
            // Example: "com.test" matches "com.test.MyClass" but not "com.test.sub.OtherClass"
            final String packageName = this.getPackageFromClassName(className);
            // Class is directly in this package, match it
            return packageName.equals(pattern);
        } else {
            // Simple class name (no package) - exact match only
            final String simpleName = getSimpleClassName(className);
            return simpleName.equals(pattern);
        }
    }

    /**
     * Gets the package name from a fully qualified class name
     */
    private String getPackageFromClassName(final String className) {
        final int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        }
        return "";
    }
}
