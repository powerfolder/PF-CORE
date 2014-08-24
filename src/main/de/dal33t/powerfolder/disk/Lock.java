/*
 * Copyright 2004 - 2014 Christian Sprajc. All rights reserved.
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
 * $Id: FolderScanner.java 18828 2012-05-10 01:24:49Z tot $
 */
package de.dal33t.powerfolder.disk;

import java.io.Serializable;
import java.util.Date;

import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * PFC-1962: The main class representing a lock.
 * 
 * @author <a href="mailto:sprajc@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.114 $
 */
public class Lock implements Serializable {
    private static final long serialVersionUID = 100L;

    private Date created;
    private FileInfo fileInfo;
    private MemberInfo memberInfo;
    private AccountInfo accountInfo;

    public Lock(FileInfo fileInfo, MemberInfo memberInfo,
        AccountInfo accountInfo)
    {
        super();
        Reject.ifNull(fileInfo, "FileInfo");
        this.created = new Date();
        this.fileInfo = fileInfo;
        this.memberInfo = memberInfo;
        this.accountInfo = accountInfo;
    }

    // Getter

    public Date getCreated() {
        return created;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public MemberInfo getMemberInfo() {
        return memberInfo;
    }

    public AccountInfo getAccountInfo() {
        return accountInfo;
    }

    // General

    @Override
    public String toString() {
        return "Lock [fileInfo=" + fileInfo + ", created=" + created
            + ", accountInfo=" + accountInfo + ", memberInfo=" + memberInfo
            + "]";
    }
}
