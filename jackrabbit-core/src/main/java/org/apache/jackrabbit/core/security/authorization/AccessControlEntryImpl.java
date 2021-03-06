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

import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.value.ValueHelper;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simple, immutable implementation of the
 * {@link javax.jcr.security.AccessControlEntry}
 * and the {@link JackrabbitAccessControlEntry} interfaces.
 */
public abstract class AccessControlEntryImpl implements JackrabbitAccessControlEntry {

    /**
     * All privileges contained in this entry
     */
    private final Privilege[] privileges;

    /**
     * PrivilegeBits calculated from built-in privileges
     */
    private final int privilegeBits;

    /**
     * the Principal of this entry
     */
    private final Principal principal;

    /**
     * Jackrabbit specific extension: if the actions contained are allowed or
     * denied.
     */
    private final boolean allow;

    /**
     * Jackrabbit specific extension: the list of additional restrictions to be
     * included in the evaluation.
     */
    private final Map<Name, Value> restrictions;

    /**
     * Hash code being calculated on demand.
     */
    private int hashCode = -1;

    /**
     * Calculated set of non-aggregate custom privileges (see also privilegeBits
     * above for the built-in privileges) used upon ac evaluation.
     */
    private Set<Privilege> customPrivs;

    /**
     * Construct an access control entry for the given principal and privileges.
     *
     * @param principal Principal for this access control entry.
     * @param privileges Privileges for this access control entry.
     * @throws AccessControlException if either principal or privileges are invalid.
     * @throws RepositoryException if another error occurs.
     */
    protected AccessControlEntryImpl(Principal principal, Privilege[] privileges)
            throws AccessControlException, RepositoryException {
        this(principal, privileges, true, null);
    }

    /**
     * Construct an access control entry for the given principal and privileges.
     *
     * @param principal Principal for this access control entry.
     * @param privileges Privileges for this access control entry.
     * @param isAllow <code>true</code> if this ACE grants the specified
     * privileges to the specified principal; <code>false</code> otherwise.
     * @param restrictions A map of restriction name (String) to restriction
     * (Value). See {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionNames()}
     * and {@link org.apache.jackrabbit.api.security.JackrabbitAccessControlList#getRestrictionType(String)}.
     * @throws AccessControlException if either principal or privileges are invalid.
     * @throws RepositoryException if another error occurs.
     */
    protected AccessControlEntryImpl(Principal principal, Privilege[] privileges,
                                     boolean isAllow, Map<String, Value> restrictions)
            throws AccessControlException, RepositoryException {
        if (principal == null) {
            throw new IllegalArgumentException();
        }
        // make sure no abstract privileges are passed.
        for (Privilege privilege : privileges) {
            if (privilege.isAbstract()) {
                throw new AccessControlException("Privilege " + privilege + " is abstract.");
            }
        }
        this.principal = principal;
        this.privileges = privileges;
        this.privilegeBits = getPrivilegeManager().getBits(privileges);
        this.allow = isAllow;

        if (restrictions == null) {
            this.restrictions = Collections.emptyMap();
        } else {
            this.restrictions = new HashMap<Name, Value>(restrictions.size());
            // validate the passed restrictions and fill the map
            for (String name : restrictions.keySet()) {
                Value value = ValueHelper.copy(restrictions.get(name), getValueFactory());
                this.restrictions.put(getResolver().getQName(name), value);
            }
        }
    }

    /**
     *
     * @param base
     * @param privileges
     * @param isAllow
     * @throws AccessControlException
     */
    protected AccessControlEntryImpl(AccessControlEntryImpl base, Privilege[] privileges, boolean isAllow)
            throws AccessControlException, RepositoryException {
        this(base.principal, privileges, isAllow, (base.restrictions.isEmpty()) ? null : Collections.<String, Value>emptyMap());

        if (!base.restrictions.isEmpty()) {
            // validate the passed restrictions and fill the map
            for (Name name : base.restrictions.keySet()) {
                Value value = ValueHelper.copy(base.restrictions.get(name), getValueFactory());
                this.restrictions.put(name, value);
            }
        }
    }
    
    /**
     * @return the permission bits that correspond to the privileges defined by this entry.
     */
    public int getPrivilegeBits() {
        return privilegeBits;
    }

    /**
     * @return A collection of all non-aggregate custom privileges defined by
     * this entry including those contained in the aggregated custom privileges.
     */
    public Set<Privilege> getCustomPrivileges() {
        if (customPrivs == null) {
            customPrivs = new HashSet<Privilege>();
            for (Privilege p : privileges) {
                try {
                    if (getPrivilegeManager().isCustomPrivilege(p)) {
                        if (p.isAggregate()) {
                            for (Privilege aggr : p.getAggregatePrivileges()) {
                                if (!aggr.isAggregate()) {
                                    customPrivs.add(p);
                                }
                            }
                        } else {
                            customPrivs.add(p);

                        }
                    }
                } catch (AccessControlException e) {
                    // ignore.
                }
            }
        }
        return customPrivs;
    }

    /**
     * Returns <code>true</code> if this ACE defines any restriction.
     *
     * @return <code>true</code> if this ACE defines any restriction;
     * <code>false</code> otherwise.
     */
    public boolean hasRestrictions() {
        return !restrictions.isEmpty();
    }

    /**
     * Returns the restrictions defined for this entry.
     *
     * @return the restrictions defined for this entry.
     */
    public Map<Name,Value> getRestrictions() {
        return Collections.unmodifiableMap(restrictions);
    }

    /**
     * @param restrictionName
     * @return The restriction with the specified name or <code>null</code>.
     */
    public Value getRestriction(Name restrictionName) {
        return ValueHelper.copy(restrictions.get(restrictionName), getValueFactory());
    }

    /**
     * @return Returns the name resolver used to convert JCR names to Name and vice versa.
     */
    protected abstract NameResolver getResolver();

    /**
     * @return The value factory to be used.
     */
    protected abstract ValueFactory getValueFactory();

    /**
     * @return The privilege manager in use.
     */
    protected abstract PrivilegeManagerImpl getPrivilegeManager();

    /**
     * Build the hash code.
     *
     * @return the hash code.
     */
    protected int buildHashCode() {
        int h = 17;
        h = 37 * h + principal.getName().hashCode();
        for (Privilege p : privileges) {
            h = 37 * h + p.hashCode();
        }
        h = 37 * h + Boolean.valueOf(allow).hashCode();
        h = 37 * h + restrictions.hashCode();
        return h;
    }

    //-------------------------------------------------< AccessControlEntry >---
    /**
     * @see javax.jcr.security.AccessControlEntry#getPrincipal()
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @see javax.jcr.security.AccessControlEntry#getPrivileges()
     */
    public Privilege[] getPrivileges() {
        return privileges;
    }


    //---------------------------------------< JackrabbitAccessControlEntry >---
    /**
     * @see JackrabbitAccessControlEntry#isAllow()
     */
    public boolean isAllow() {
        return allow;
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry#getRestrictionNames()
     */
    public String[] getRestrictionNames() throws NamespaceException {
        String[] restrNames = new String[restrictions.size()];
        int i = 0;
        for (Name n : restrictions.keySet()) {
            restrNames[i] = getResolver().getJCRName(n);
            i++;
        }
        return restrNames;
    }

    /**
     * @see org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry#getRestriction(String)
     */
    public Value getRestriction(String restrictionName) throws RepositoryException {
        return getRestriction(getResolver().getQName(restrictionName));
    }

    //-------------------------------------------------------------< Object >---
    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = buildHashCode();
        }
        return hashCode;
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AccessControlEntryImpl) {
            AccessControlEntryImpl other = (AccessControlEntryImpl) obj;
            return principal.getName().equals(other.principal.getName()) &&
                   privilegeBits == other.privilegeBits &&
                   allow == other.allow &&
                   restrictions.equals(other.restrictions) &&
                   equalCustomPrivileges(other);
        }
        return false;
    }

    private boolean equalCustomPrivileges(AccessControlEntryImpl other) {
        Set<Privilege> a1 = getCustomPrivileges();
        Set<Privilege> a2 = other.getCustomPrivileges();
        return a1.size() == a2.size() && a1.containsAll(a2);
    }
}