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

package com.jecstar.etm.apm.server;

import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class parses the relevant APM json schema's and generates domain objects.
 */
public class DomainGenerator {

    private static final String BASE_TARGET_PACKAGE = "com.jecstar.etm.apm.server.domain";
    private static final String BASE_SOURCE_URL = "https://raw.githubusercontent.com/elastic/apm-server/7.6/docs/spec/";

    private final JsonConverter converter = new JsonConverter();

    public static void main(String[] args) throws IOException {
        new DomainGenerator().create("transactions/transaction.json");
    }

    private void create(String entityPath) throws IOException {
        var url = new URL(BASE_SOURCE_URL + entityPath);
        var entityBaseUrl = url.toString().substring(0, url.toString().lastIndexOf("/") + 1);
        var entityName = url.toString().substring(url.toString().lastIndexOf("/") + 1, url.toString().lastIndexOf("."));
        var content = loadContent(url);
        var schemaMap = this.converter.toMap(content);
        createClass(schemaMap, entityBaseUrl, entityName);
    }

    @SuppressWarnings("unchecked")
    private void createClass(Map<String, Object> objectValues, String entityBaseUrl, String entityName) {
        if (!"object".equals(this.converter.getString("type", objectValues))) {
            throw new RuntimeException("Entity '" + entityName + "' @ '" + entityBaseUrl + "' is not an object.");
        }
        if (!entityBaseUrl.startsWith(BASE_SOURCE_URL)) {
            throw new RuntimeException("Entity outside of base source url: '" + entityBaseUrl + "'.");
        }
        var packageName = BASE_TARGET_PACKAGE;
        if (!entityBaseUrl.equals(BASE_SOURCE_URL)) {
            packageName = BASE_TARGET_PACKAGE + "." + entityBaseUrl.substring(BASE_SOURCE_URL.length(), entityBaseUrl.length() - 1).replaceAll("/", ".");
        }
        File packageDirectory = new File("./etm-apm-server/src/main/java", packageName.replaceAll("\\.", "/"));
        if (!packageDirectory.exists()) {
            packageDirectory.mkdirs();
        }
        var classToGenerate = new ClassToGenerate();
        classToGenerate.packageName = packageName;
        classToGenerate.name = this.converter.getString("title", objectValues, entityName).replaceAll(" ", "");
        classToGenerate.name = classToGenerate.name.substring(0, 1).toUpperCase() + classToGenerate.name.substring(1);
        classToGenerate.description = this.converter.getString("description", objectValues);

        var attributeValuesList = (List<Map<String, Object>>) this.converter.getArray("allOf", objectValues);
        for (var attributeValues : attributeValuesList) {
            if (attributeValues.containsKey("$ref")) {
                var reference = this.converter.getString("$ref", attributeValues);
                System.out.println(reference);
            } else if (attributeValues.containsKey("properties")) {
                var properties = this.converter.getObject("properties", attributeValues);
                for (var jsonKey : properties.keySet()) {
                    var javaName = Arrays.stream(jsonKey.split("_")).map(f -> f.substring(0, 1).toUpperCase() + f.substring(1).toLowerCase()).collect(Collectors.joining());
                    javaName = javaName.substring(0, 1).toLowerCase() + javaName.substring(1);
                    var propertyValues = this.converter.getObject(jsonKey, properties);
                    if (propertyValues.containsKey("type")) {
                        Object type = propertyValues.get("type");
                        var types = new ArrayList<String>();
                        if (type instanceof String) {
                            types.add((String) type);
                        } else if (type instanceof ArrayList) {
                            types.addAll((Collection<? extends String>) type);
                        }
                        if (types.contains("string")) {
                            classToGenerate.addField("String", javaName, jsonKey, this.converter.getString("description", propertyValues));
                        } else if (types.contains("number")) {
                            classToGenerate.addField("Long", javaName, jsonKey, this.converter.getString("description", propertyValues));
                        } else if (types.contains("boolean")) {
                            classToGenerate.addField("Boolean", javaName, jsonKey, this.converter.getString("description", propertyValues));
                        } else {
                            System.out.println("unknown types: " + types.stream().collect(Collectors.joining(", ")));
                        }
                    } else if (propertyValues.containsKey("$ref")) {
                        var reference = this.converter.getString("$ref", propertyValues);
                        System.out.println(reference);
                    }
                }
            }
        }
        System.out.println(classToGenerate.toString());
    }

    private String loadContent(URL url) throws IOException {
        try (var in = url.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private class ClassToGenerate {

        public String packageName;
        public String name;
        public String description;
        public List<Field> fields = new ArrayList<>();

        public void addField(String type, String name, String jsonName, String description) {
            var field = new Field();
            field.type = type;
            field.name = name;
            field.jsonName = jsonName;
            field.description = description;
            this.fields.add(field);
        }

        private class Field {
            public String name;
            public String type;
            public String jsonName;
            public String description;
        }

        @Override
        public String toString() {
            StringBuilder data = new StringBuilder();
            data.append("package " + this.packageName + ";\n");
            data.append("\n");
            data.append("import com.jecstar.etm.server.core.converter.JsonField;\n");
            data.append("\n");
            if (this.description != null) {
                data.append("/**\n");
                data.append(" * " + this.description + "\n");
                data.append(" */\n");
            }
            data.append("public class " + this.name + " {\n");
            data.append("\n");
            for (var field : this.fields) {
                data.append("    @JsonField(\"" + field.jsonName + "\")\n");
                data.append("    private " + field.type + " " + field.name + ";\n");
            }
            for (var field : this.fields) {
                data.append("\n");
                if (field.description != null) {
                    data.append("    /**\n");
                    data.append("     * " + field.description + "\n");
                    data.append("     */\n");
                }
                data.append("    public " + field.type + ("Boolean".equals(field.type) ? " is" : " get") + field.name.substring(0, 1).toUpperCase() + field.name.substring(1) + "() {\n");
                data.append("        return this." + field.name + ";\n");
                data.append("    }\n");
            }
            data.append("}");
            return data.toString();
        }
    }
}
