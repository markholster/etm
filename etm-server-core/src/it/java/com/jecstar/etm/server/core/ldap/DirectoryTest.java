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

package com.jecstar.etm.server.core.ldap;

import com.jecstar.etm.integration.test.core.EmbeddableLdapServer;
import com.jecstar.etm.server.core.domain.configuration.LdapConfiguration;
import com.jecstar.etm.server.core.domain.principal.EtmGroup;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class DirectoryTest {

    private static EmbeddableLdapServer server;

    @BeforeAll
    public static void setup() {
        server = new EmbeddableLdapServer();
        server.startServer();
    }

    @AfterAll
    public static void tearDown() {
        if (server != null) {
            server.stopServer();
        }
    }

    private LdapConfiguration createLdapConfiguration() {
        LdapConfiguration ldapConfiguration = new LdapConfiguration();
        // Setup the connection.
        ldapConfiguration.setHost(EmbeddableLdapServer.HOST);
        ldapConfiguration.setPort(EmbeddableLdapServer.PORT);
//		ldapConfiguration.setConnectionSecurity(LdapConfiguration.ConnectionSecurity.STARTTLS);
        ldapConfiguration.setBindDn(EmbeddableLdapServer.BIND_DN);
        ldapConfiguration.setBindPassword(EmbeddableLdapServer.BIND_PASSWORD);
        // Setup the connection validator
        ldapConfiguration.setConnectionTestBaseDn(EmbeddableLdapServer.BIND_DN);
        ldapConfiguration.setConnectionTestSearchFilter("(objectClass=*)");
        // Setup the connection pool
        ldapConfiguration.setMinPoolSize(1);
        ldapConfiguration.setMaxPoolSize(10);
        // Setup the group filters
        ldapConfiguration.setGroupBaseDn(EmbeddableLdapServer.GROUP_BASE_DN);
        ldapConfiguration.setGroupSearchFilter("(cn={group})");
        // Setup the user filters
        ldapConfiguration.setUserBaseDn(EmbeddableLdapServer.USER_BASE_DN);
        ldapConfiguration.setUserSearchFilter("(uid={user})");
        // Setup the user attribute mapping
        ldapConfiguration.setUserIdentifierAttribute(EmbeddableLdapServer.USER_ID_ATTRIBUTE);
        ldapConfiguration.setUserFullNameAttribute(EmbeddableLdapServer.USER_NAME_ATTRIBUTE);
        ldapConfiguration.setUserEmailAttribute(EmbeddableLdapServer.USER_EMAIL_ATTRIBUTE);
        // Set the group mapping
        ldapConfiguration.setUserGroupsQueryBaseDn(EmbeddableLdapServer.GROUP_BASE_DN);
        ldapConfiguration.setUserGroupsQueryFilter("(|(member={dn})(uniqueMember={dn})(memberUid={uid}))");
        return ldapConfiguration;
    }

    @Test
    public void testAuthenticate() {
        Directory directory = new Directory(null, createLdapConfiguration());
        EtmPrincipal principal = directory.authenticate("etm-admin", "password");
        directory.close();
        assertEquals("ETM Administrator", principal.getName());
        assertEquals("etm-admin", principal.getId());
        assertEquals("etm-admin@jecstar.com", principal.getEmailAddress());
        assertSame(2, principal.getGroups().size());
    }

    @Test
    public void testAuthenticateWithWrongPassword() {
        Directory directory = new Directory(null, createLdapConfiguration());
        EtmPrincipal principal = directory.authenticate("etm-admin", "wrongPassword");
        assertNull(principal);
        principal = directory.authenticate("etm-admin", "password");
        assertNotNull(principal);
        directory.close();
    }

    @Test
    public void testAuthenticateWithoutSubtreeSearch() {
        LdapConfiguration ldapConfig = createLdapConfiguration();
        assertFalse(ldapConfig.isUserSearchInSubtree());
        Directory directory = new Directory(null, ldapConfig);
        // Test user 1 is placed in user base dn and should be able to login.
        assertNotNull(directory.authenticate("test-user-1", "password"));
        // Test users 2 & 3 are a level below the base dn and should not be able to login.
        assertNull(directory.authenticate("test-user-2", "password"));
        assertNull(directory.authenticate("test-user-3", "password"));
        directory.close();
    }

    @Test
    public void testAuthenticateWithSubtreeSearch() {
        LdapConfiguration ldapConfig = createLdapConfiguration();
        ldapConfig.setUserSearchInSubtree(true);
        assertTrue(ldapConfig.isUserSearchInSubtree());
        Directory directory = new Directory(null, ldapConfig);
        // Test user 1 is placed in user base dn and should be able to login.
        assertNotNull(directory.authenticate("test-user-1", "password"));
        // Test users 2 & 3 are a level below the base dn and should also able to login.
        assertNotNull(directory.authenticate("test-user-2", "password"));
        assertNotNull(directory.authenticate("test-user-3", "password"));
        directory.close();
    }

    @Test
    public void testGetGroups() {
        Directory directory = new Directory(null, createLdapConfiguration());
        Set<EtmGroup> groups = directory.getGroups();
        assertEquals(2, groups.size());
        directory.close();
    }

}
