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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Generates mappings.json file with all obfuscation mappings
 */
public class MappingsGenerator {

    private final Gson gson; // TODO: why not static lol?

    public MappingsGenerator() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Generates mappings.json file from obfuscation mappings
     */
    public void generateMappings(
        final Map<String, String> classNameMap,
        final Map<String, Map<String, String>> methodNameMap,
        final Map<String, Map<String, String>> fieldMappings,
        final Map<String, Map<String, Map<String, String>>> localVariableMappings,
        final String outputPath) throws IOException {

        final JsonObject mappings = new JsonObject();

        // Classes
        if (classNameMap != null && !classNameMap.isEmpty()) {
            final JsonObject classes = new JsonObject();
            for (final Map.Entry<String, String> entry : classNameMap.entrySet()) {
                classes.addProperty(entry.getKey(), entry.getValue());
            }
            mappings.add("classes", classes);
        } else {
            mappings.add("classes", new JsonObject());
        }

        // Methods
        if (methodNameMap != null && !methodNameMap.isEmpty()) {
            final JsonObject methods = new JsonObject();
            for (final Map.Entry<String, Map<String, String>> classEntry : methodNameMap.entrySet()) {
                final String className = classEntry.getKey();
                final Map<String, String> methodMap = classEntry.getValue();
                if (methodMap != null && !methodMap.isEmpty()) {
                    final JsonObject classMethods = new JsonObject();
                    for (final Map.Entry<String, String> methodEntry : methodMap.entrySet()) {
                        // methodEntry.getKey() is "methodName(descriptor)"
                        classMethods.addProperty(methodEntry.getKey(), methodEntry.getValue());
                    }
                    methods.add(className, classMethods);
                }
            }
            mappings.add("methods", methods);
        } else {
            mappings.add("methods", new JsonObject());
        }

        // Fields (variables)
        if (fieldMappings != null && !fieldMappings.isEmpty()) {
            final JsonObject fields = new JsonObject();
            for (final Map.Entry<String, Map<String, String>> classEntry : fieldMappings.entrySet()) {
                final String className = classEntry.getKey();
                final Map<String, String> fieldMap = classEntry.getValue();
                if (fieldMap != null && !fieldMap.isEmpty()) {
                    final JsonObject classFields = new JsonObject();
                    for (final Map.Entry<String, String> fieldEntry : fieldMap.entrySet()) {
                        classFields.addProperty(fieldEntry.getKey(), fieldEntry.getValue());
                    }
                    fields.add(className, classFields);
                }
            }
            mappings.add("fields", fields);
        } else {
            mappings.add("fields", new JsonObject());
        }

        // Local variables
        if (localVariableMappings != null && !localVariableMappings.isEmpty()) {
            final JsonObject localVars = new JsonObject();
            for (final Map.Entry<String, Map<String, Map<String, String>>> classEntry : localVariableMappings.entrySet()) {
                final String className = classEntry.getKey();
                final Map<String, Map<String, String>> methodVarsMap = classEntry.getValue();
                if (methodVarsMap != null && !methodVarsMap.isEmpty()) {
                    final JsonObject classLocalVars = new JsonObject();
                    for (final Map.Entry<String, Map<String, String>> methodEntry : methodVarsMap.entrySet()) {
                        final String methodKey = methodEntry.getKey();
                        final Map<String, String> varMap = methodEntry.getValue();
                        if (varMap != null && !varMap.isEmpty()) {
                            final JsonObject methodVars = new JsonObject();
                            for (final Map.Entry<String, String> varEntry : varMap.entrySet()) {
                                methodVars.addProperty(varEntry.getKey(), varEntry.getValue());
                            }
                            classLocalVars.add(methodKey, methodVars);
                        }
                    }
                    localVars.add(className, classLocalVars);
                }
            }
            mappings.add("localVariables", localVars);
        } else {
            mappings.add("localVariables", new JsonObject());
        }
        // Write to file
        String mappingsFilePath = outputPath.replace(".jar", "_mappings.json");
        if (!mappingsFilePath.endsWith(".json")) {
            mappingsFilePath = outputPath + "_mappings.json"; // TODO: add date and time?
        }
        try (final FileWriter writer = new FileWriter(mappingsFilePath)) {
            this.gson.toJson(mappings, writer);
        }
    }
}

