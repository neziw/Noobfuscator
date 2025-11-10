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

import java.util.Map;
import ovh.neziw.obfuscator.obfuscator.NameGenerator;
import ovh.neziw.obfuscator.obfuscator.PatternMatcher;

/**
 * Maps original class names to obfuscated names
 * Single Responsibility: Generate and manage class name mappings
 */
public class ClassNameMapper {

    private final NameGenerator nameGenerator;
    private final PatternMatcher patternMatcher;
    private final Map<String, String> classNameMap;
    private final boolean obfuscatePackages;
    private final boolean obfuscateClassNames;

    public ClassNameMapper(final NameGenerator nameGenerator, final PatternMatcher patternMatcher,
                           final Map<String, String> classNameMap,
                           final boolean obfuscatePackages, final boolean obfuscateClassNames) {
        this.nameGenerator = nameGenerator;
        this.patternMatcher = patternMatcher;
        this.classNameMap = classNameMap;
        this.obfuscatePackages = obfuscatePackages;
        this.obfuscateClassNames = obfuscateClassNames;
    }

    /**
     * Generates an obfuscated class name for a given class
     */
    public String generateObfuscatedClassName(final String className) {
        if (this.obfuscatePackages && this.obfuscateClassNames) {
            return this.obfuscateBothPackageAndClass(className);
        } else if (this.obfuscatePackages) {
            return this.obfuscatePackageOnly(className);
        } else if (this.obfuscateClassNames) {
            return this.obfuscateClassOnly(className);
        } else {
            return className; // Should not happen
        }
    }

    /**
     * Gets or generates an obfuscated class name, storing it in the map
     */
    public String getOrGenerateObfuscatedName(final String className) {
        String obfuscated = this.classNameMap.get(className);
        if (obfuscated == null) {
            obfuscated = this.generateObfuscatedClassName(className);
            this.classNameMap.put(className, obfuscated);
        }
        return obfuscated;
    }

    /**
     * Obfuscates a class name reference if it should be obfuscated
     */
    public String obfuscateClassName(final String className) {
        if (className == null || className.isEmpty() || className.equals("java/lang/Object")) {
            return className;
        }
        // Check if this class should be obfuscated
        if (this.patternMatcher.matches(className)) {
            return this.getOrGenerateObfuscatedName(className);
        }
        return className;
    }

    private String obfuscateBothPackageAndClass(final String className) {
        final String packageName = PatternMatcher.getPackageName(className);
        final String simpleClassName = PatternMatcher.getSimpleClassName(className);

        if (!packageName.isEmpty()) {
            final String[] packageParts = packageName.split("/");
            final StringBuilder obfuscatedPackage = new StringBuilder();
            for (final String part : packageParts) {
                if (!obfuscatedPackage.isEmpty()) {
                    obfuscatedPackage.append("/");
                }
                obfuscatedPackage.append(this.nameGenerator.getObfuscatedName("pkg." + part));
            }
            final String obfuscatedSimpleName = this.nameGenerator.getObfuscatedName("cls." + simpleClassName);
            return obfuscatedPackage + "/" + obfuscatedSimpleName;
        } else {
            return this.nameGenerator.getObfuscatedName("cls." + simpleClassName);
        }
    }

    private String obfuscatePackageOnly(final String className) {
        final String packageName = PatternMatcher.getPackageName(className);
        final String simpleClassName = PatternMatcher.getSimpleClassName(className);

        if (!packageName.isEmpty()) {
            final String[] packageParts = packageName.split("/");
            final StringBuilder obfuscatedPackage = new StringBuilder();
            for (final String part : packageParts) {
                if (!obfuscatedPackage.isEmpty()) {
                    obfuscatedPackage.append("/");
                }
                obfuscatedPackage.append(this.nameGenerator.getObfuscatedName("pkg." + part));
            }
            return obfuscatedPackage + "/" + simpleClassName;
        } else {
            return className; // No package to obfuscate
        }
    }

    private String obfuscateClassOnly(final String className) {
        final String packageName = PatternMatcher.getPackageName(className);
        final String simpleClassName = PatternMatcher.getSimpleClassName(className);

        final String obfuscatedSimpleName = this.nameGenerator.getObfuscatedName("cls." + simpleClassName);
        if (!packageName.isEmpty()) {
            return packageName + "/" + obfuscatedSimpleName;
        } else {
            return obfuscatedSimpleName;
        }
    }
}

