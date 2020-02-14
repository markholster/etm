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

package com.jecstar.etm.launcher;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class JvmOptionsParser {

    private static final Pattern PATTERN = Pattern.compile("((?<start>\\d+)(?<range>-)?(?<end>\\d+)?:)?(?<option>-.*)$");

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected a single argument with the location of jvm.options");
        }
        final int majorJavaVersion = getJavaVersion();
        final List<String> jvmOptions = new ArrayList<>();
        boolean hasErrors = false;

        try (InputStream inputStream = Files.newInputStream(Paths.get(args[0]));
             Reader reader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
             BufferedReader br = new BufferedReader(reader)) {
            int lineIx = 0;
            while (true) {
                String line = br.readLine();
                lineIx++;
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.startsWith("#")) {
                    continue;
                }
                if (line.length() == 0) {
                    continue;
                }
                final Matcher matcher = PATTERN.matcher(line);
                if (matcher.matches()) {
                    final String start = matcher.group("start");
                    final String end = matcher.group("end");
                    if (start == null) {
                        jvmOptions.add(line);
                    } else {
                        final int lower;
                        try {
                            lower = Integer.parseInt(start);
                        } catch (final NumberFormatException e) {
                            hasErrors = true;
                            System.err.println("Invalid JVM version number '" + start + "'at line " + lineIx + ".");
                            continue;
                        }
                        final int upper;
                        if (matcher.group("range") == null) {
                            upper = lower;
                        } else if (end == null) {
                            upper = Integer.MAX_VALUE;
                        } else {
                            try {
                                upper = Integer.parseInt(end);
                            } catch (final NumberFormatException e) {
                                hasErrors = true;
                                System.err.println("Invalid JVM version number '" + end + "'at line " + lineIx + ".");
                                continue;
                            }
                            if (upper < lower) {
                                hasErrors = true;
                                System.err.println("Invalid JVM version range '" + start + " < " + end + "'at line " + lineIx + ".");
                                continue;
                            }
                        }
                        if (lower <= majorJavaVersion && majorJavaVersion <= upper) {
                            jvmOptions.add(matcher.group("option"));
                        }
                    }
                } else {
                    hasErrors = true;
                    System.err.println("Invalid configuration at line " + lineIx + ".");
                    continue;
                }
            }
            if (hasErrors) {
                System.exit(-1);
            }
            StringBuilder result = new StringBuilder();
            Iterator<String> iterator = jvmOptions.iterator();
            while (iterator.hasNext()) {
                result.append(iterator.next());
                if (iterator.hasNext()) {
                    result.append(" ");
                }
            }
            System.out.println(result.toString());
        }
    }

    private static int getJavaVersion() {
        List<Integer> versionElements = Arrays.stream(System.getProperty("java.specification.version").split("\\.")).map(Integer::valueOf).collect(Collectors.toList());

        if (versionElements.size() >= 2 && versionElements.get(0) == 1 && versionElements.get(1) <= 8) {
            versionElements = new ArrayList<>(versionElements.subList(1, versionElements.size()));
        }
        return versionElements.get(0);
    }
}
