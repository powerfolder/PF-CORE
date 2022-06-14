/*
 * Copyright 2004 - 2022 Christian Sprajc. All rights reserved.
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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Date;

@Embeddable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AuditFields implements Serializable {
    private static final long serialVersionUID = 100L;

    private static final String SEPARATOR = "|";
    private Date creationDate;
    private String creationAccount;
    private Date modifiedDate;
    private String modifiedAccount;

    public AuditFields() {
    }

    public void setCreatedNowBy(final Account account) {
        this.creationAccount = toAccountField(account);
        this.creationDate = new Date();
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getCreationAccountOID() {
        return getOID(creationAccount);
    }

    public String getCreationAccountUsername() {
        return getUsername(creationAccount);
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public String getModifiedAccountOID() {
        return getOID(modifiedAccount);
    }

    public String getModifiedAccountUsername() {
        return getUsername(modifiedAccount);
    }

    public void setModifiedNowBy(final Account account) {
        this.modifiedAccount = toAccountField(account);
        this.modifiedDate = new Date();
    }

    private String getUsername(String fieldContent) {
        if (fieldContent == null) {
            return null;
        }
        return fieldContent.split(SEPARATOR)[1];
    }

    private String getOID(String fieldContent) {
        if (fieldContent == null) {
            return null;
        }
        return fieldContent.split(SEPARATOR)[0];
    }

    private String toAccountField(Account account) {
        if (account == null) {
            return null;
        }
        return account.getOID() + SEPARATOR + account.getUsername();
    }
}
