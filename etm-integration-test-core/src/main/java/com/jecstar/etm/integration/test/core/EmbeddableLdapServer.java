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

package com.jecstar.etm.integration.test.core;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmbeddableLdapServer {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 10389;
    public static final int SECURE_PORT = 10636;
    public static final String BIND_DN = "cn=jecstar-admin,ou=system,dc=jecstar,dc=com";
    public static final String BIND_PASSWORD = "admin-password";
    public static final String GROUP_BASE_DN = "ou=groups,dc=jecstar,dc=com";
    public static final String USER_BASE_DN = "ou=people,dc=jecstar,dc=com";
    private static final String DEPARTMENT_1_USER_BASE_DN = "ou=department1," + USER_BASE_DN;
    private static final String DEPARTMENT_2_USER_BASE_DN = "ou=department2," + USER_BASE_DN;

    public static final String USER_ID_ATTRIBUTE = "uid";
    public static final String USER_NAME_ATTRIBUTE = "cn";
    public static final String USER_EMAIL_ATTRIBUTE = "mail";

    public static final String ADMIN_USER_ID = "etm-admin";
    public static final String ADMIN_GROUP_DN = "cn=etm-admin-group," + GROUP_BASE_DN;


    /**
     * The directory service
     */
    private DirectoryService service;

    /**
     * The LDAP server
     */
    private LdapServer server;

    /**
     * Add a new partition to the server
     *
     * @param partitionId The partition Id
     * @param partitionDn The partition DN
     * @param dnFactory   the DN factory
     * @return The newly added partition
     * @throws Exception If the partition can't be added
     */
    private Partition addPartition(String partitionId, String partitionDn, DnFactory dnFactory) throws Exception {
        // Create a new partition with the given partition id
        JdbmPartition partition = new JdbmPartition(service.getSchemaManager(), dnFactory);
        partition.setId(partitionId);
        partition.setPartitionPath(new File(service.getInstanceLayout().getPartitionsDirectory(), partitionId).toURI());
        partition.setSuffixDn(new Dn(partitionDn));
        service.addPartition(partition);

        return partition;
    }

    /**
     * Add a new set of index on the given attributes
     *
     * @param partition The partition on which we want to add index
     * @param attrs     The list of attributes to index
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addIndex(Partition partition, String... attrs) {
        // Index some attributes on the apache partition
        Set indexedAttributes = new HashSet();

        for (String attribute : attrs) {
            indexedAttributes.add(new JdbmIndex(attribute, false));
        }

        ((JdbmPartition) partition).setIndexedAttributes(indexedAttributes);
    }

    /**
     * initialize the schema manager and add the schema partition to diectory
     * service
     *
     * @throws Exception if the schema LDIF files are not found on the classpath
     */
    private void initSchemaPartition() throws Exception {
        InstanceLayout instanceLayout = service.getInstanceLayout();

        File schemaPartitionDirectory = new File(instanceLayout.getPartitionsDirectory(), "schema");

        // Extract the schema on disk (a brand new one) and load the registries
        if (schemaPartitionDirectory.exists()) {
            System.out.println("schema partition already exists, skipping schema extraction");
        } else {
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(instanceLayout.getPartitionsDirectory());
            extractor.extractOrCopy();
        }

        SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse
        // and normalize their suffix Dn
        schemaManager.loadAllEnabled();

        List<Throwable> errors = schemaManager.getErrors();

        if (errors.size() != 0) {
            throw new Exception(I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)));
        }

        service.setSchemaManager(schemaManager);

        // Init the LdifPartition with schema
        LdifPartition schemaLdifPartition = new LdifPartition(schemaManager, service.getDnFactory());
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());

        // The schema partition
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        service.setSchemaPartition(schemaPartition);
    }

    /**
     * Initialize the server. It creates the partition, adds the index, and
     * injects the context entries for the created partitions.
     *
     * @param workDir the directory to be used for storing the data
     * @throws Exception if there were some problems while initializing the system
     */
    private void initDirectoryService(File workDir) throws Exception {
        // Initialize the LDAP service
        service = new DefaultDirectoryService();
        service.setInstanceLayout(new InstanceLayout(workDir));

        CacheService cacheService = new CacheService();
        cacheService.initialize(service.getInstanceLayout());

        service.setCacheService(cacheService);

        // first load the schema
        initSchemaPartition();

        // then the system partition
        // this is a MANDATORY partition
        // DO NOT add this via addPartition() method, trunk code complains about
        // duplicate partition
        // while initializing
        JdbmPartition systemPartition = new JdbmPartition(service.getSchemaManager(), service.getDnFactory());
        systemPartition.setId("system");
        systemPartition.setPartitionPath(
                new File(service.getInstanceLayout().getPartitionsDirectory(), systemPartition.getId()).toURI());
        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
        systemPartition.setSchemaManager(service.getSchemaManager());

        // mandatory to call this method to set the system partition
        // Note: this system partition might be removed from trunk
        service.setSystemPartition(systemPartition);

        // Disable the ChangeLog system
        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);

        // Now we can create as many partitions as we need
        Partition jecstarPartition = addPartition("jecstar", "dc=jecstar,dc=com", service.getDnFactory());

        // Index some attributes on the jecstar partition
        addIndex(jecstarPartition, "objectClass", "ou", "uid");

        // And start the service
        service.startup();

        // Inject the context entry for dc=Jecstar,dc=Com partition
        if (!service.getAdminSession().exists(jecstarPartition.getSuffixDn())) {
            Dn dnJecstar = new Dn("dc=jecstar,dc=com");
            Entry entryJecstar = service.newEntry(dnJecstar);
            entryJecstar.add("objectClass", "top", "domain", "extensibleObject");
            entryJecstar.add("dc", "jecstar");
            service.getAdminSession().add(entryJecstar);

            // Add people context.
            Dn dnPeople = new Dn(USER_BASE_DN);
            Entry entryPeople = service.newEntry(dnPeople);
            entryPeople.add("objectClass", "organizationalUnit", "top");
            entryPeople.add("ou", "people");
            service.getAdminSession().add(entryPeople);

            // Add dep 1 context
            Dn dnDep1 = new Dn(DEPARTMENT_1_USER_BASE_DN);
            Entry entryDep1 = service.newEntry(dnDep1);
            entryDep1.add("objectClass", "organizationalUnit", "top");
            entryDep1.add("ou", "people");
            service.getAdminSession().add(entryDep1);

            // Add dep 1 context
            Dn dnDep2 = new Dn(DEPARTMENT_2_USER_BASE_DN);
            Entry entryDep2 = service.newEntry(dnDep2);
            entryDep2.add("objectClass", "organizationalUnit", "top");
            entryDep2.add("ou", "people");
            service.getAdminSession().add(entryDep2);

            // Add system context.
            Dn dnSystem = new Dn("ou=system,dc=jecstar,dc=com");
            Entry entrySystem = service.newEntry(dnSystem);
            entrySystem.add("objectClass", "organizationalUnit", "top");
            entrySystem.add("ou", "system");
            service.getAdminSession().add(entrySystem);

            // Add group context.
            Dn dnGroups = new Dn(GROUP_BASE_DN);
            Entry entryGroups = service.newEntry(dnGroups);
            entryGroups.add("objectClass", "organizationalUnit", "top");
            entryGroups.add("ou", "groups");
            service.getAdminSession().add(entryGroups);

            // Add the bind user
            Dn dnUser = new Dn(BIND_DN);
            Entry entryUser = service.newEntry(dnUser);
            entryUser.add("objectClass", "inetOrgPerson", "organizationalPerson", "person", "top");
            entryUser.add("cn", "Jecstar administrator");
            entryUser.add("sn", "jecstar-admin");
            entryUser.add("userPassword", BIND_PASSWORD);
            service.getAdminSession().add(entryUser);

            addUser(service, ADMIN_USER_ID, "ETM Administrator", USER_BASE_DN);
            addUser(service, "etm-searcher", "ETM Searcher", USER_BASE_DN);
            addUser(service, "test-user-1", "Test user 1", USER_BASE_DN);
            addUser(service, "test-user-2", "Test user 2", DEPARTMENT_1_USER_BASE_DN);
            addUser(service, "test-user-3", "Test user 3", DEPARTMENT_2_USER_BASE_DN);

            // Add some groups
            Dn dnGroup = new Dn(ADMIN_GROUP_DN);
            Entry entryGroup = service.newEntry(dnGroup);
            entryGroup.add("objectClass", "groupOfNames", "top");
            entryGroup.add("cn", "Etm admin group");
            entryGroup.add("member", "cn=" + ADMIN_USER_ID + ",ou=people,dc=jecstar,dc=com");
            service.getAdminSession().add(entryGroup);

            dnGroup = new Dn("cn=etm-searchers-group,ou=groups,dc=jecstar,dc=com");
            entryGroup = service.newEntry(dnGroup);
            entryGroup.add("objectClass", "groupOfNames", "top");
            entryGroup.add("cn", "Etm searchers group");
            entryGroup.add("member", "cn=etm-searcher,ou=people,dc=jecstar,dc=com");
            entryGroup.add("member", "cn=" + ADMIN_USER_ID + ",ou=people,dc=jecstar,dc=com");
            service.getAdminSession().add(entryGroup);
        }

        // We are all done !
    }

    private void addUser(DirectoryService service, String userId, String userName, String baseDN) throws LdapException {
        Dn dnUser = new Dn("cn=" + userId + "," + baseDN);
        Entry entryUser = service.newEntry(dnUser);
        entryUser.add("objectClass", "inetOrgPerson", "organizationalPerson", "person", "top");
        entryUser.add(USER_NAME_ATTRIBUTE, userName);
        entryUser.add("sn", userId);
        entryUser.add(USER_ID_ATTRIBUTE, userId);
        entryUser.add(USER_EMAIL_ATTRIBUTE, userId + "@jecstar.com");
        entryUser.add("userPassword", "password");
        service.getAdminSession().add(entryUser);

    }

    /**
     * Creates a new instance of EmbeddedADS. It initializes the directory
     * service.
     *
     * @throws Exception If something went wrong
     */
    public EmbeddableLdapServer() {
        try {
            File workDir = new File(System.getProperty("java.io.tmpdir") + "/server-work");
            if (workDir.exists()) {
                Files.walk(workDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

            }
            initDirectoryService(workDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * starts the LdapServer
     *
     * @throws Exception
     */
    public void startServer() {
        this.server = new LdapServer();
        this.server.setTransports(new TcpTransport(HOST, PORT));
        this.server.setDirectoryService(service);
        try {
            this.server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stopServer() {
        this.server.stop();
    }

    public static void main(String[] args) {
        new EmbeddableLdapServer().startServer();
    }

}
