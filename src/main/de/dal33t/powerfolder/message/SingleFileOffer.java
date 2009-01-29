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
package de.dal33t.powerfolder.message;

import de.dal33t.powerfolder.light.MemberInfo;

import java.io.File;

/**
 * Message offering a single file transfer.
 */
public class SingleFileOffer extends Message  {

    private static final long serialVersionUID = 100L;

    private File file;
    private MemberInfo sourceMemberInfo;
    private String message;

    public SingleFileOffer(File file, MemberInfo sourceMemberInfo,
                                  String message) {
        this.file = file;
        this.sourceMemberInfo = sourceMemberInfo;
        this.message = message;
    }

    public File getFile() {
        return file;
    }

    public String getMessage() {
        return message;
    }

    public MemberInfo getSourceMemberInfo() {
        return sourceMemberInfo;
    }
}