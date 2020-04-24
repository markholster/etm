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

package com.jecstar.etm.apm.server.generator;

public class JsonConverterClass implements ClassToGenerate {

    public String packageName;
    public String name;
    public String description;
    public String classToConvertName;
    public String classToConvertPackage;
    public boolean topLevelClass;

    @Override
    public String getContent() {
        StringBuilder data = new StringBuilder();
        data.append("package " + this.packageName + ";\n");
        data.append("\n");
        if (this.topLevelClass) {
            data.append("import com.jecstar.etm.server.core.converter.JsonEntityConverter;\n");
        } else {
            data.append("import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;\n");
        }
        data.append("import " + this.classToConvertPackage + "." + this.classToConvertName + ";\n");
        data.append("\n");
        if (this.description != null && this.description.trim().length() > 0) {
            data.append("/**\n");
            data.append(" * " + this.description + "\n");
            data.append(" */\n");
        }
        data.append("public class " + this.name + " extends " + (this.topLevelClass ? "JsonEntityConverter" : "NestedObjectConverter") + "<" + this.classToConvertName + "> {\n");
        data.append("\n");
        data.append("   public " + this.name + "() { super(f -> new " + this.classToConvertName + "()); }\n");
        data.append("}");
        return data.toString();
    }
}
