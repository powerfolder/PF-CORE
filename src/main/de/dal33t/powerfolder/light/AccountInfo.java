/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: FileInfo.java 8381 2009-06-24 01:39:20Z tot $
 */
package de.dal33t.powerfolder.light;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.Transient;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.AccountInfoProto;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.intern.AccountInfoInternalizer;
import de.dal33t.powerfolder.util.intern.Internalizer;

/**
 * Leightweight reference/info object to an {@link Account}
 *
 * @author sprajc
 */
@Embeddable
public class AccountInfo implements Serializable, D2DObject {
    public static final String PROPERTYNAME_OID = "oid";
    public static final String PROPERTYNAME_USERNAME = "username";

    private static final long serialVersionUID = 100L;
    private static final Internalizer<AccountInfo> INTERNALIZER = new AccountInfoInternalizer();

    private String oid;
    private String username;
    @Transient
    private String displayName;

    private AccountInfo() {
        // For hibernate.
    }

    public AccountInfo(String oid, String username, String displayName) {
        this.oid = oid;
        this.username = username;
        this.displayName = displayName;
    }

    public AccountInfo(String oid, String username) {
        this(oid, username, null);
    }

    /** AccountInfo
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    public
    AccountInfo(AbstractMessage mesg)
    {
      initFromD2D(mesg);
    }

    public String getOID() {
        return oid;
    }

    public String getDisplayName() {
        if (StringUtils.isNotBlank(displayName)) {
            return displayName;
        }
        return username;
    }

    public String getScrabledDisplayName() {
        return getScrabledName(getDisplayName());
    }

    /**
     * TODO Don't actually transfer unscrambled emails to any client.
     *
     * @return a scrabled version of the username in case its a email.
     */
    public String getScrabledUsername() {
        return getScrabledName(username);
    }

    /**
     * TODO Don't actually transfer unscrambled emails to any client.
     *
     * @return a scrabled version of the username in case its a email.
     */
    private String getScrabledName(String val) {
        if (val == null) {
            return null;
        }
        int i = val.indexOf('@');
        if (i < 0) {
            return val;
        }
        int chopIndex = Math.max(i - 3, 1);
        return val.substring(0, chopIndex) + "..." + val.substring(i);
    }

    public String getUsername() {
        return username;
    }

    public AccountInfo intern(boolean force) {
        if (force) {
            return INTERNALIZER.rename(this);
        } else {
            return intern();
        }
    }

    public AccountInfo intern() {
        return INTERNALIZER.intern(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oid == null) ? 0 : oid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AccountInfo other = (AccountInfo) obj;
        if (oid == null) {
            if (other.oid != null)
                return false;
        } else if (!oid.equals(other.oid))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AccountInfo '" + getScrabledDisplayName() + "' (" + oid + ')';
    }

    // Serializing ************************************************************

    private static final long extVersionUID = 100L;

    public static AccountInfo readExt(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.readExternal(in);
        return accountInfo;
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", expected: " + extVersionUID);
        }
        oid = in.readUTF();
        username = in.readUTF();
        if (in.readBoolean()) {
            displayName = in.readUTF();
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);
        out.writeUTF(oid);
        out.writeUTF(username);
        if (displayName != null) {
            out.writeBoolean(true);
            out.writeUTF(displayName);
        } else {
            out.writeBoolean(false);
        }
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof AccountInfoProto.AccountInfo)
        {
          AccountInfoProto.AccountInfo ainfo = (AccountInfoProto.AccountInfo)mesg;

          this.oid         = ainfo.getOid();
          this.username    = ainfo.getUsername();
          this.displayName = ainfo.getDisplayName();
        }
    }

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      AccountInfoProto.AccountInfo.Builder builder = AccountInfoProto.AccountInfo.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());
      builder.setOid(this.oid);
      builder.setUsername(this.username);
      builder.setDisplayName(this.displayName);

      return builder.build();
    }
}
