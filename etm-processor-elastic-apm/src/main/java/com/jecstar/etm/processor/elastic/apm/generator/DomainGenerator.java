/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.processor.elastic.apm.generator;

import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class parses the relevant APM json schema's and generates domain objects.
 */
public class DomainGenerator {

    private static final String BASE_TARGET_PACKAGE = "com.jecstar.etm.processor.elastic.apm.domain";
    private static final String BASE_SOURCE_URL = "https://raw.githubusercontent.com/elastic/apm-server/7.6/docs/spec/";

    private final JsonConverter converter = new JsonConverter();

    public static void main(String[] args) throws IOException {
        new DomainGenerator().create("errors/error.json");
        new DomainGenerator().create("metadata.json");
        new DomainGenerator().create("metricsets/metricset.json");
        new DomainGenerator().create("sourcemaps/payload.json");
        new DomainGenerator().create("spans/span.json");
        new DomainGenerator().create("transactions/transaction.json");
    }

    private void create(String entityPath) throws IOException {
        var url = new URL(new URL(BASE_SOURCE_URL), entityPath);
        var entityBaseUrl = url.toString().substring(0, url.toString().lastIndexOf("/") + 1);
        var entityName = url.toString().substring(url.toString().lastIndexOf("/") + 1, url.toString().lastIndexOf("."));
        var content = loadContent(url);
        var schemaMap = this.converter.toMap(content);
        createClass(schemaMap, entityBaseUrl, entityName, true);
    }

    @SuppressWarnings("unchecked")
    private GenerationResult createClass(Map<String, Object> objectValues, String entityBaseUrl, String entityName, boolean topLevelClass) throws IOException {
        Object type = objectValues.get("type");
        var types = new ArrayList<String>();
        if (type instanceof String) {
            types.add((String) type);
        } else if (type instanceof ArrayList) {
            types.addAll((Collection<? extends String>) type);
        }
        if (!types.contains("object")) {
            throw new RuntimeException("Entity '" + entityName + "' @ '" + entityBaseUrl + "' is not an object.");
        }
        if (!entityBaseUrl.startsWith(BASE_SOURCE_URL)) {
            throw new RuntimeException("Entity outside of base source url: '" + entityBaseUrl + "'.");
        }
        var packageName = BASE_TARGET_PACKAGE;
        var converterPackageName = BASE_TARGET_PACKAGE + ".converter";
        if (!entityBaseUrl.equals(BASE_SOURCE_URL)) {
            var subPackage = entityBaseUrl.substring(BASE_SOURCE_URL.length(), entityBaseUrl.length() - 1).replaceAll("/", ".");
            packageName += "." + subPackage;
            converterPackageName += "." + subPackage;
        }
        var packageDirectory = new File("./etm-processor-elastic-apm/src/main/java", packageName.replaceAll("\\.", "/"));
        if (!packageDirectory.exists()) {
            packageDirectory.mkdirs();
        }
        var converterPackageDirectory = new File("./etm-processor-elastic-apm/src/main/java", converterPackageName.replaceAll("\\.", "/"));
        if (!converterPackageDirectory.exists()) {
            converterPackageDirectory.mkdirs();
        }
        var jsonDataClass = new JsonDataClass();
        jsonDataClass.packageName = packageName;
        jsonDataClass.name = toJavaName(this.converter.getString("title", objectValues, entityName), true);
        jsonDataClass.description = this.converter.getString("description", objectValues);

        var attributeValuesList = (List<Map<String, Object>>) this.converter.getArray("allOf", objectValues);
        if (attributeValuesList != null) {
            for (var attributeValues : attributeValuesList) {
                if (attributeValues.containsKey("$ref")) {
                    var reference = this.converter.getString("$ref", attributeValues);
                    Map<String, Object> referenceValues = this.converter.toMap(loadContent(new URL(new URL(entityBaseUrl), reference)));
                    if (referenceValues.containsKey("properties")) {
                        var properties = this.converter.getObject("properties", referenceValues);
                        addPropertiesToClass(properties, jsonDataClass, entityBaseUrl);
                    }
                } else if (attributeValues.containsKey("properties")) {
                    var properties = this.converter.getObject("properties", attributeValues);
                    addPropertiesToClass(properties, jsonDataClass, entityBaseUrl);
                }
            }
        }
        if (objectValues.containsKey("properties")) {
            var properties = this.converter.getObject("properties", objectValues);
            addPropertiesToClass(properties, jsonDataClass, entityBaseUrl);
        }
        var targetFile = new File(packageDirectory, jsonDataClass.name + ".java");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        try (var writer = new FileWriter(targetFile)) {
            writer.write(jsonDataClass.getContent());
        }

        var converterClass = jsonDataClass.createConverterClass(converterPackageName, topLevelClass);
        targetFile = new File(converterPackageDirectory, converterClass.name + ".java");
        if (targetFile.exists()) {
            targetFile.delete();
        }
        try (var writer = new FileWriter(targetFile)) {
            writer.write(converterClass.getContent());
        }
        var result = new GenerationResult();
        result.jsonDataClass = jsonDataClass;
        result.jsonConverterClass = converterClass;
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addPropertiesToClass(Map<String, Object> properties, JsonDataClass classToGenerate, String entityBaseUrl) throws IOException {
        for (var jsonKey : properties.keySet()) {
            var javaName = DomainGenerator.toJavaName(jsonKey, false);
            var propertyValues = this.converter.getObject(jsonKey, properties);
            if (propertyValues.containsKey("$ref")) {
                var reference = this.converter.getString("$ref", propertyValues);
                var url = new URL(new URL(entityBaseUrl), reference);
                var referenceValues = this.converter.toMap(loadContent(url));
                var entityName = url.toString().substring(url.toString().lastIndexOf("/") + 1, url.toString().lastIndexOf("."));
                var generationResult = createClass(referenceValues, url.toString().substring(0, url.toString().lastIndexOf("/") + 1), entityName, false);
                classToGenerate.addField(generationResult.jsonDataClass.packageName + "." + generationResult.jsonDataClass.name, javaName, jsonKey, generationResult.jsonDataClass.description, generationResult.jsonConverterClass.packageName + "." + generationResult.jsonConverterClass.name + ".class");
            } else if (propertyValues.containsKey("type")) {
                Object type = propertyValues.get("type");
                var types = new ArrayList<String>();
                if (type instanceof String) {
                    types.add((String) type);
                } else if (type instanceof ArrayList) {
                    types.addAll((Collection<? extends String>) type);
                }
                if (types.contains("string")) {
                    classToGenerate.addField("String", javaName, jsonKey, this.converter.getString("description", propertyValues), null);
                } else if (types.contains("number") || types.contains("integer")) {
                    classToGenerate.addField("Long", javaName, jsonKey, this.converter.getString("description", propertyValues), null);
                } else if (types.contains("boolean")) {
                    classToGenerate.addField("Boolean", javaName, jsonKey, this.converter.getString("description", propertyValues), null);
                } else if (types.contains("object") && propertyValues.containsKey("properties")) {
                    var generationResult = createClass(propertyValues, entityBaseUrl, jsonKey, false);
                    classToGenerate.addField(generationResult.jsonDataClass.packageName + "." + generationResult.jsonDataClass.name, javaName, jsonKey, generationResult.jsonDataClass.description, generationResult.jsonConverterClass.packageName + "." + generationResult.jsonConverterClass.name + ".class");
                } else if (types.contains("object") && propertyValues.containsKey("patternProperties")) {
                    classToGenerate.addField("java.util.Map<String, Object>", javaName, jsonKey, this.converter.getString("description", propertyValues), null);
                } else {
                    System.out.println("Element '" + jsonKey + "' has unknown types: " + types.stream().collect(Collectors.joining(", ")));
                }
            }
        }
    }

    private String loadContent(URL url) throws IOException {
        try (var in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String toJavaName(String name, boolean startWithUppercase) {
        var elements = name.replace(" ", "_").split("_");
        if (startWithUppercase) {
            return Arrays.stream(elements).map(e -> e.substring(0, 1).toUpperCase() + e.substring(1)).collect(Collectors.joining());
        }
        elements[0] = elements[0].substring(0, 1).toLowerCase() + elements[0].substring(1);
        if (elements.length == 1) {
            return elements[0];
        }
        return elements[0] + Arrays.stream(Arrays.copyOfRange(elements, 1, elements.length)).map(e -> e.substring(0, 1).toUpperCase() + e.substring(1)).collect(Collectors.joining());
    }
}
