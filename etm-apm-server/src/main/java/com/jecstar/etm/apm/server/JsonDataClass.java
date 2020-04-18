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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonDataClass implements ClassToGenerate {
    public String packageName;
    public String name;
    public String description;
    public List<Field> fields = new ArrayList<>();

    public void addField(String type, String name, String jsonName, String description, String converterClass) {
        var field = new Field();
        field.type = type;
        field.name = name;
        field.jsonName = jsonName;
        field.description = description;
        field.converterClass = converterClass;
        if (this.fields.contains(field)) {
            System.out.println("Field '" + field.name + "' already present. Skipping this field!!");
        } else {
            this.fields.add(field);
        }
    }

    private class Field {
        public String name;
        public String type;
        public String jsonName;
        public String description;
        public String converterClass;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Field field = (Field) o;
            return Objects.equals(this.name, field.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name);
        }
    }

    @Override
    public String getContent() {
        StringBuilder data = new StringBuilder();
        data.append("package " + this.packageName + ";\n");
        data.append("\n");
        data.append("import com.jecstar.etm.server.core.converter.JsonField;\n");
        data.append("\n");
        if (this.description != null && this.description.trim().length() > 0) {
            data.append("/**\n");
            data.append(" * " + this.description + "\n");
            data.append(" */\n");
        }
        data.append("public class " + this.name + " {\n");
        data.append("\n");
        for (var field : this.fields) {
            if (field.converterClass != null) {
                data.append("    @JsonField(value = \"" + field.jsonName + "\", converterClass = " + field.converterClass + ")\n");
            } else {
                data.append("    @JsonField(\"" + field.jsonName + "\")\n");
            }
            data.append("    private " + field.type + " " + field.name + ";\n");
        }
        for (var field : this.fields) {
            data.append("\n");
            if (field.description != null && field.description.trim().length() > 0) {
                data.append("    /**\n");
                data.append("     * " + field.description + "\n");
                data.append("     */\n");
            }
            data.append("    public " + field.type + ("Boolean".equals(field.type) ? " is" : " get") + DomainGenerator.toJavaName(field.name, true) + "() {\n");
            data.append("        return this." + field.name + ";\n");
            data.append("    }\n");
        }
        data.append("}");
        return data.toString();
    }

    public JsonConverterClass createConverterClass(String converterPackageName, boolean topLevelClass) {

        var jsonConverterClass = new JsonConverterClass();
        jsonConverterClass.packageName = converterPackageName;
        jsonConverterClass.name = this.name + "Converter";
        jsonConverterClass.topLevelClass = topLevelClass;
        jsonConverterClass.classToConvertName = this.name;
        jsonConverterClass.classToConvertPackage = this.packageName;
        jsonConverterClass.description = "Converter class for the <code>" + this.name + "</code> class.";
        return jsonConverterClass;
    }
}
