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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Reject;

/**
 * PFS-779: Domain object for PFS-779: Organization wide admin role to manage
 * user accounts per "admin domain"/Organization - Multitenancy -
 * Mandantenfähigkeit
 * 
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Organization implements Serializable {
    private static final long serialVersionUID = 100L;

    public static final String FILTER_MATCH_ALL = "/ALL/";
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_NAME = "name";

    @Id
    private String oid;
    @Index(name = "IDX_ORGANIZATION_NAME")
    @Column(nullable = false)
    private String name;

    @Column(length = 1024)
    private String notes;

    public Organization() {
        // Generate unique id
        this(IdGenerator.makeId());
    }

    Organization(String oid) {
        Reject.ifBlank(oid, "OID");
        this.oid = oid;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
