/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.AccountDetailsProto;
import de.dal33t.powerfolder.protocol.AccountProto;
import de.dal33t.powerfolder.security.Account;
import de.dal33t.powerfolder.util.Format;

import java.io.Serializable;

/**
 * Capsulates a identity and adding additional information.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class AccountDetails extends Message implements Serializable, D2DObject {
    private static final long serialVersionUID = 100L;

    private Account user;
    private long spaceUsed;
    private Boolean needsToAgreeToS;

    /**
     * Still named "recycleBinSize" for Serialization compatibility reasons.
     * <P>
     * Remove after major distibution of 4.1.0
     */
    private long recycleBinSize;

    public AccountDetails(Account user, long spaceUsed, long archiveSize, Boolean needsToAgreeToS) {
        super();
        this.user = user;
        this.spaceUsed = spaceUsed;
        this.recycleBinSize = archiveSize;
        this.needsToAgreeToS = needsToAgreeToS;
    }

    public Account getAccount() {
        return user;
    }

    /**
     * @return the total usage of space (folders + archive)
     */
    public long getSpaceUsed() {
        return spaceUsed;
    }

    public long getArchiveSize() {
        return recycleBinSize;
    }

    public boolean isUnknown() {
        return spaceUsed < 0;
    }

    public boolean needsToAgreeToS() {
        if (needsToAgreeToS == null) {
            return false;
        }
        return needsToAgreeToS.booleanValue();
    }

    public String toString() {
        return "AccountDetails, " + user + ". "
            + Format.formatBytesShort(spaceUsed + recycleBinSize);
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void initFromD2D(AbstractMessage mesg) {
      if(mesg instanceof AccountDetailsProto.AccountDetails) {
          AccountDetailsProto.AccountDetails proto = 
              (AccountDetailsProto.AccountDetails)mesg;

          this.user            = new Account(proto.getAccount());
          this.spaceUsed       = proto.getSpaceUsed();
          this.needsToAgreeToS = proto.getNeedsToAgreeToS();
          this.recycleBinSize  = proto.getRecycleBinSize();
      }
    }

    /** toD2
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage toD2D() {
      AccountDetailsProto.AccountDetails.Builder builder = 
          AccountDetailsProto.AccountDetails.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());
      builder.setAccount((AccountProto.Account)this.user.toD2D());
      builder.setSpaceUsed(this.spaceUsed);
      if (this.needsToAgreeToS != null) { builder.setNeedsToAgreeToS(this.needsToAgreeToS); };
      builder.setRecycleBinSize(this.recycleBinSize);

      return builder.build();
    }
}