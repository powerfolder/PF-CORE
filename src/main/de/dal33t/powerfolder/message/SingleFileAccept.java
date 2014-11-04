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
* $Id: SingleFileAccept.java 6723 2009-01-30 14:08:38Z harry $
*/
package de.dal33t.powerfolder.message;

import java.nio.file.Path;

import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Message accepting a single file transfer offer.
 */
public class SingleFileAccept extends Message  {

    private static final long serialVersionUID = 100L;

    private Path file;
    private MemberInfo offeringMemberInfo;
    private MemberInfo acceptingMemberInfo;

    public SingleFileAccept(Path file, MemberInfo offeringMemberInfo,
                            MemberInfo acceptingMemberInfo) {
        this.file = file;
        this.offeringMemberInfo = offeringMemberInfo;
        this.acceptingMemberInfo = acceptingMemberInfo;
    }

    public Path getFile() {
        return file;
    }

    public MemberInfo getOfferingMemberInfo() {
        return offeringMemberInfo;
    }

    public MemberInfo getAcceptingMemberInfo() {
        return acceptingMemberInfo;
    }
}