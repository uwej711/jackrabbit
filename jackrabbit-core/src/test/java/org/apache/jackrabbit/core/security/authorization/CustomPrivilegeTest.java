/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.commons.privilege.PrivilegeDefinition;
import org.apache.jackrabbit.commons.privilege.PrivilegeDefinitionWriter;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <code>CustomPrivilegeTest</code>...
 */
public class CustomPrivilegeTest extends AbstractJCRTest {

    private NameResolver resolver;

    private FileSystem fs;
    private PrivilegeRegistry privilegeRegistry;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        resolver = ((SessionImpl) superuser);

        // setup the custom privilege file with cyclic references
        fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }

        privilegeRegistry = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            if (fs.exists("/privileges") && fs.isFolder("/privileges")) {
                fs.deleteFolder("/privileges");
            }
        } finally {
            super.tearDown();
        }
    }

    private static void assertPrivilege(PrivilegeRegistry registry, NameResolver resolver, PrivilegeRegistry.Definition def) throws RepositoryException {
        PrivilegeManagerImpl pmgr = new PrivilegeManagerImpl(registry, resolver);
        Privilege p = pmgr.getPrivilege(resolver.getJCRName(def.getName()));

        assertNotNull(p);

        assertEquals(def.isCustom(), pmgr.isCustomPrivilege(p));
        assertEquals(def.isAbstract(), p.isAbstract());
        Name[] danames = def.getDeclaredAggregateNames();
        assertEquals(danames.length > 0, p.isAggregate());
        assertEquals(danames.length, p.getDeclaredAggregatePrivileges().length);
    }

    private static void assertBits(int expected, PrivilegeRegistry.Definition def, PrivilegeRegistry registry) {
        assertEquals(expected, registry.getBits(new PrivilegeRegistry.Definition[] {def}));
    }

    private static Set<Name> createNameSet(Name... names) {
        Set<Name> set = new HashSet<Name>();
        set.addAll(Arrays.asList(names));
        return set;
    }

    public void testInvalidCustomDefinitions() throws RepositoryException, FileSystemException, IOException {
        // setup the custom privilege file with cyclic references
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><privileges><privilege isAbstract=\"false\" name=\"test\"><contains name=\"test2\"/></privilege></privileges>");

        Writer writer = new OutputStreamWriter(resource.getOutputStream(), "utf-8");
        writer.write(sb.toString());
        writer.flush();
        writer.close();

        try {
            new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            fail("Invalid names must be detected upon registry startup.");
        } catch (RepositoryException e) {
            // success
        } finally {
            fs.deleteFolder("/privileges");
        }
    }

    public void testCustomDefinitionsWithCyclicReferences() throws RepositoryException, FileSystemException, IOException {
        // setup the custom privilege file with cyclic references
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }

        OutputStream out = resource.getOutputStream();
        try {
            List<PrivilegeDefinition> defs = new ArrayList<PrivilegeDefinition>();
            defs.add(new PrivilegeDefinition("test", false, new String[] {"test2"}));
            defs.add(new PrivilegeDefinition("test4", true, new String[] {"test5"}));
            defs.add(new PrivilegeDefinition("test5", false, new String[] {"test3"}));
            defs.add(new PrivilegeDefinition("test3", false, new String[] {"test"}));
            defs.add(new PrivilegeDefinition("test2", false, new String[] {"test4"}));
            PrivilegeDefinitionWriter pdw = new PrivilegeDefinitionWriter("text/xml");
            pdw.writeDefinitions(out, defs.toArray(new PrivilegeDefinition[defs.size()]), Collections.<String, String>emptyMap());

            new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            fail("Cyclic definitions must be detected upon registry startup.");
        } catch (RepositoryException e) {
            // success
        } finally {
            out.close();
            fs.deleteFolder("/privileges");
        }
    }

    public void testCustomEquivalentDefinitions() throws RepositoryException, FileSystemException, IOException {
        // setup the custom privilege file with cyclic references
        FileSystem fs = ((RepositoryImpl) superuser.getRepository()).getConfig().getFileSystem();
        FileSystemResource resource = new FileSystemResource(fs, "/privileges/custom_privileges.xml");
        if (!resource.exists()) {
            resource.makeParentDirs();
        }

        OutputStream out = resource.getOutputStream();
        try {
            List<PrivilegeDefinition> defs = new ArrayList<PrivilegeDefinition>();
            defs.add(new PrivilegeDefinition("test", false, new String[] {"test2","test3"}));
            defs.add(new PrivilegeDefinition("test2", true, new String[] {"test4"}));
            defs.add(new PrivilegeDefinition("test3", true, new String[] {"test5"}));
            defs.add(new PrivilegeDefinition("test4", true, new String[0]));
            defs.add(new PrivilegeDefinition("test5", true, new String[0]));

            // the equivalent definition to 'test'
            defs.add(new PrivilegeDefinition("test6", false, new String[] {"test2","test5"}));

            PrivilegeDefinitionWriter pdw = new PrivilegeDefinitionWriter("text/xml");
            pdw.writeDefinitions(out, defs.toArray(new PrivilegeDefinition[defs.size()]), Collections.<String, String>emptyMap());

            new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            fail("Equivalent definitions must be detected upon registry startup.");
        } catch (RepositoryException e) {
            // success
        } finally {
            out.close();
            fs.deleteFolder("/privileges");
        }
    }

    public void testRegisterBuiltInPrivilege() throws RepositoryException, IllegalNameException, FileSystemException {
        Map<Name, Set<Name>> builtIns = new HashMap<Name, Set<Name>>();
        builtIns.put(NameConstants.JCR_READ, Collections.<Name>emptySet());
        builtIns.put(NameConstants.JCR_LIFECYCLE_MANAGEMENT, Collections.singleton(NameConstants.JCR_ADD_CHILD_NODES));
        builtIns.put(PrivilegeRegistry.REP_WRITE_NAME, Collections.<Name>emptySet());
        builtIns.put(NameConstants.JCR_ALL, Collections.<Name>emptySet());

        for (Name builtInName : builtIns.keySet()) {
            try {
                privilegeRegistry.registerDefinition(builtInName, false, builtIns.get(builtInName));
                fail("Privilege name already in use -> Exception expected");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testRegisterInvalidNewAggregate() throws RepositoryException, IllegalNameException, FileSystemException {
        Map<Name, Set<Name>> newAggregates = new HashMap<Name, Set<Name>>();
        // same as jcr:read
        newAggregates.put(resolver.getQName("jcr:newAggregate"), Collections.singleton(NameConstants.JCR_READ));
        // aggregated combining built-in and an unknown privilege
        newAggregates.put(resolver.getQName("jcr:newAggregate"), createNameSet(NameConstants.JCR_READ, resolver.getQName("unknownPrivilege")));
        // aggregate containing unknown privilege
        newAggregates.put(resolver.getQName("newAggregate"), createNameSet(resolver.getQName("unknownPrivilege")));
        // aggregated combining built-in and custom
        newAggregates.put(resolver.getQName("newAggregate"), createNameSet(NameConstants.JCR_READ, resolver.getQName("unknownPrivilege")));
        // custom aggregated contains itself
        newAggregates.put(resolver.getQName("newAggregate"), createNameSet(resolver.getQName("newAggregate")));
        // same as rep:write
        newAggregates.put(resolver.getQName("repWriteAggregate"), createNameSet(NameConstants.JCR_MODIFY_PROPERTIES, NameConstants.JCR_ADD_CHILD_NODES, NameConstants.JCR_NODE_TYPE_MANAGEMENT, NameConstants.JCR_REMOVE_CHILD_NODES,NameConstants.JCR_REMOVE_NODE));
        // aggregating built-in -> currently not supported
        newAggregates.put(resolver.getQName("aggrBuiltIn"), createNameSet(NameConstants.JCR_MODIFY_PROPERTIES, NameConstants.JCR_READ));

        for (Name name : newAggregates.keySet()) {
            try {
                privilegeRegistry.registerDefinition(name, true, newAggregates.get(name));
                fail("New aggregate referring to unknown Privilege  -> Exception expected");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testRegisterInvalidNewAggregate2() throws RepositoryException, FileSystemException {
        Map<Name, Set<Name>> newCustomPrivs = new LinkedHashMap<Name, Set<Name>>();
        newCustomPrivs.put(resolver.getQName("new"), Collections.<Name>emptySet());
        newCustomPrivs.put(resolver.getQName("new2"), Collections.<Name>singleton(resolver.getQName("new")));

        for (Name name : newCustomPrivs.keySet()) {
            boolean isAbstract = true;
            Set<Name> aggrNames = newCustomPrivs.get(name);
            privilegeRegistry.registerDefinition(name, isAbstract, aggrNames);
        }

        Map<Name, Set<Name>> newAggregates = new HashMap<Name, Set<Name>>();
        // a new aggregate of custom and built-in privilege
        newAggregates.put(resolver.getQName("newA1"), createNameSet(resolver.getQName("new"), NameConstants.JCR_READ));
        // other illegal aggregates already represented by registered definition.
        newAggregates.put(resolver.getQName("newA2"), Collections.<Name>singleton(resolver.getQName("new")));
        newAggregates.put(resolver.getQName("newA3"), Collections.<Name>singleton(resolver.getQName("new2")));

        for (Name name : newAggregates.keySet()) {
            boolean isAbstract = false;
            Set<Name> aggrNames = newAggregates.get(name);

            try {
                privilegeRegistry.registerDefinition(name, isAbstract, aggrNames);
                fail("Invalid aggregation in definition '"+ name.toString()+"' : Exception expected");
            } catch (RepositoryException e) {
                // success
            }
        }
    }

    public void testRegisterCustomPrivileges() throws RepositoryException, FileSystemException {
        Map<Name, Set<Name>> newCustomPrivs = new HashMap<Name, Set<Name>>();
        newCustomPrivs.put(resolver.getQName("new"), Collections.<Name>emptySet());
        newCustomPrivs.put(resolver.getQName("test:new"), Collections.<Name>emptySet());

        for (Name name : newCustomPrivs.keySet()) {
            boolean isAbstract = true;
            Set<Name> aggrNames = newCustomPrivs.get(name);

            privilegeRegistry.registerDefinition(name, isAbstract, aggrNames);

            // validate definition
            PrivilegeRegistry.Definition definition = privilegeRegistry.get(name);
            assertNotNull(definition);
            assertTrue(definition.isCustom());
            assertEquals(name, definition.getName());
            assertTrue(definition.isAbstract());
            assertTrue(definition.declaredAggregateNames.isEmpty());
            assertEquals(aggrNames.size(), definition.declaredAggregateNames.size());
            for (Name n : aggrNames) {
                assertTrue(definition.declaredAggregateNames.contains(n));
            }
            assertBits(PrivilegeRegistry.NO_PRIVILEGE, definition, privilegeRegistry);

            List<Name> allAgg = Arrays.asList(privilegeRegistry.get(NameConstants.JCR_ALL).getDeclaredAggregateNames());
            assertTrue(allAgg.contains(name));

            // re-read the filesystem resource and check if definition is correct
            PrivilegeRegistry registry = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            PrivilegeRegistry.Definition def = registry.get(name);
            assertEquals(isAbstract, def.isAbstract);
            assertEquals(aggrNames.size(), def.declaredAggregateNames.size());
            for (Name n : aggrNames) {
                assertTrue(def.declaredAggregateNames.contains(n));
            }

            assertPrivilege(privilegeRegistry, (SessionImpl) superuser, definition);
        }

        Map<Name, Set<Name>> newAggregates = new HashMap<Name, Set<Name>>();
        // a new aggregate of custom privileges
        newAggregates.put(resolver.getQName("newA2"), createNameSet(resolver.getQName("test:new"), resolver.getQName("new")));

        for (Name name : newAggregates.keySet()) {
            boolean isAbstract = false;
            Set<Name> aggrNames = newAggregates.get(name);
            privilegeRegistry.registerDefinition(name, isAbstract, aggrNames);
            PrivilegeRegistry.Definition definition = privilegeRegistry.get(name);

            assertNotNull(definition);
            assertTrue(definition.isCustom());
            assertEquals(name, definition.getName());
            assertFalse(definition.isAbstract());
            assertFalse(definition.declaredAggregateNames.isEmpty());
            assertEquals(aggrNames.size(), definition.declaredAggregateNames.size());
            for (Name n : aggrNames) {
                assertTrue(definition.declaredAggregateNames.contains(n));
            }

            assertBits(PrivilegeRegistry.NO_PRIVILEGE, definition, privilegeRegistry);

            List<Name> allAgg = Arrays.asList(privilegeRegistry.get(NameConstants.JCR_ALL).getDeclaredAggregateNames());
            assertTrue(allAgg.contains(name));

            // re-read the filesystem resource and check if definition is correct
            PrivilegeRegistry registry = new PrivilegeRegistry(superuser.getWorkspace().getNamespaceRegistry(), fs);
            PrivilegeRegistry.Definition def = registry.get(name);
            assertEquals(isAbstract, def.isAbstract);
            assertEquals(isAbstract, def.isAbstract);
            assertEquals(aggrNames.size(), def.declaredAggregateNames.size());
            for (Name n : aggrNames) {
                assertTrue(def.declaredAggregateNames.contains(n));
            }

            assertPrivilege(registry, (SessionImpl) superuser, def);
        }
    }

    public void testCustomPrivilege() throws RepositoryException, FileSystemException {
        boolean isAbstract = false;
        Name name = ((SessionImpl) superuser).getQName("test");
        privilegeRegistry.registerDefinition(name, isAbstract, Collections.<Name>emptySet());

        PrivilegeManagerImpl pm = new PrivilegeManagerImpl(privilegeRegistry, resolver);
        String privName = resolver.getJCRName(name);

        Privilege priv = pm.getPrivilege(privName);
        assertEquals(privName, priv.getName());
        assertEquals(isAbstract, priv.isAbstract());
        assertFalse(priv.isAggregate());
        assertEquals(PrivilegeRegistry.NO_PRIVILEGE, pm.getBits(priv));

        Privilege jcrWrite = pm.getPrivilege(Privilege.JCR_WRITE);
        assertEquals(pm.getBits(jcrWrite), pm.getBits(priv, jcrWrite));

    }

    public void testRegister100CustomPrivileges() throws RepositoryException, FileSystemException {
        for (int i = 0; i < 100; i++) {
            boolean isAbstract = true;
            Name name = ((SessionImpl) superuser).getQName("test"+i);
            privilegeRegistry.registerDefinition(name, isAbstract, Collections.<Name>emptySet());
            PrivilegeRegistry.Definition definition = privilegeRegistry.get(name);

            assertNotNull(definition);
            assertEquals(name, definition.getName());
        }
    }
}