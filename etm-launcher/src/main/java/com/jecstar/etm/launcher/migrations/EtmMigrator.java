package com.jecstar.etm.launcher.migrations;

/**
 * Interface for all migrators.
 * <p>
 * A migrator is a piece of code that runs before ETM is fully started. It's purpose is to migrate the environment in
 * such a way that it is capable of running against that environment. For example, the upgrade from Elasticsearch 5.x to
 * Elasticsearch 6.x results in a change in the ETM mappings and hence needs to be converted at startup.
 */
public interface EtmMigrator {

    /**
     * Determine if this particular <code>EtmMigrator</code> instance should be executed.
     *
     * @return <code>true</code> when this <code>EtmMigrator</code> should be executed. <code>false</code> otherwise.
     */
    boolean shouldBeExecuted();

    /**
     * Run the migration.
     */
    void migrate();

}
