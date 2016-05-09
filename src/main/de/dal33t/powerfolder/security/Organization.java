/*
 * Copyright 2004 - 2013 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id$
 */
package de.dal33t.powerfolder.security;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.*;
import javax.persistence.Entity;

import org.hibernate.annotations.*;

import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import org.hibernate.annotations.CascadeType;
import org.hibernate.cfg.IndexColumn;

/**
 * PFS-779: Domain object for PFS-779: Organization wide admin role to manage
 * user accounts per "admin domain"/Organization - Multitenancy -
 * Mandantenfähigkeit
 * 
 * @author Christian Sprajc
 * @version $Revision: 1.5 $
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Organization implements Serializable {
    private static final long serialVersionUID = 100L;

    public static final int UNLIMITED_USERS = 999999999;
    public static final String FILTER_MATCH_ALL = "/ALL/";
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_NAME = "name";
    public static final String PROPERTYNAME_LDAPDN = "ldapDN";
    public static final String PROPERTYNAME_NOTES = "notes";
    public static final String PROPERTYNAME_OSS = "osSubscription";
    public static final String PROPERTYNAME_MAX_USERS = "maxUsers";

    @Id
    private String oid;
    @Index(name = "IDX_ORGANIZATION_NAME")
    @Column(nullable = false)
    private String name;

    @Column(length = 2048)
    private String notes;

    private int maxUsers;

    @Index(name = "IDX_ORGANIZATION_LDAPDN")
    @Column(length = 512)
    private String ldapDN;
    
    // PFS-1446     
    @Column(length = 512)
    private String basePath;

    @Embedded
    @Fetch(FetchMode.JOIN)
    private OnlineStorageSubscription osSubscription;

    // PFS-2005
    @CollectionOfElements
    @org.hibernate.annotations.IndexColumn(name = "IDX_DOMAINS", base = 0, nullable = false)
    @Cascade(value = {CascadeType.ALL})
    @Column(name = "domain", length = 512)
    @BatchSize(size = 1337)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> domains;

    /**
     * PFS-1411
     */
    private String skin;

    public Organization() {
        // Generate unique id
        this(IdGenerator.makeId());
    }

    public Organization(String oid) {
        Reject.ifBlank(oid, "OID");
        this.oid = oid;
        this.osSubscription = new OnlineStorageSubscription();
    }

    public String getOID() {
        return oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }

    public void setUnlimitedUsers() {
        this.maxUsers = UNLIMITED_USERS;
    }

    public boolean hasMaxUsers() {
        return this.maxUsers > 0 && this.maxUsers != UNLIMITED_USERS;
    }

    public void setLdapDN(String ldapDN) {
        this.ldapDN = ldapDN;
    }

    public String getLdapDN() {
        return ldapDN;
    }
    
    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public OnlineStorageSubscription getOSSubscription() {
        return osSubscription;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = StringUtils.cutNotes(notes);
    }

    public List<String> getDomains() { return domains; };

    public void setDomains (List<String> domains){
        this.domains = domains;
    }
    
    /**
     * Adds a line of info with the current date to the notes of that account.
     *
     * @param infoText
     */
    public void addNotesWithDate(String infoText) {
        if (StringUtils.isBlank(infoText)) {
            return;
        }
        String infoLine = Format.formatDateCanonical(new Date());
        infoLine += ": ";
        infoLine += infoText;
        String newNotes;
        if (StringUtils.isBlank(notes)) {
            newNotes = infoLine;
        } else {
            newNotes = notes + "\n" + infoLine;
        }
        setNotes(newNotes);
    }

    @Override
    public String toString() {
        return "Organization [oid=" + oid + ", name=" + name + ", maxUsers="
            + maxUsers + ", osSubscription=" + osSubscription + ", notes="
            + notes + "]";
    }

}
