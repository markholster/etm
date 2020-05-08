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

import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.migrations.EtmMigrator;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MigrateCommand extends AbstractCommand {

    private DataRepository dateRepository;

    void migrate(String migratorName, Configuration configuration) {
        if (this.dateRepository != null) {
            return;
        }
        addShutdownHooks();
        this.dateRepository = createElasticsearchClient(configuration);
        try {
            Class<?> aClass = Class.forName("com.jecstar.etm.launcher.migrations." + migratorName);
            Constructor<?> constructor = aClass.getConstructor(DataRepository.class);
            EtmMigrator migrator = (EtmMigrator) constructor.newInstance(this.dateRepository);
            if (!migrator.shouldBeExecuted()) {
                System.out.println("Migrator check failed. Only run this migrator when instructed by the Jecstar support desk. Type 'YES' if you want to continue?");
                String input = null;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                    input = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!"YES".equals(input)) {
                    System.out.println("Aborted migrator execution.");
                    return;
                }
            }
            migrator.migrate(true);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        stop();
    }

    private void stop() {
        if (MigrateCommand.this.dateRepository != null) {
            try {
                MigrateCommand.this.dateRepository.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private void addShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }


}
