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
package de.dal33t.powerfolder.event;

import java.util.EventObject;
import java.util.Set;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

/** The event that tells which user joined which folders, used from NodeManager. */
public class AskForFriendshipEvent extends EventObject {

    private Set<FolderInfo> joinedFolders;
    private String message;

    /**
     * @param source
     *            the member that joined 1 or more folders
     * @param joinedFolders
     *            set of folders that the member joined
     */
    public AskForFriendshipEvent(MemberInfo source,
                                 Set<FolderInfo> joinedFolders) {
        super(source);
        this.joinedFolders = joinedFolders;
    }

    public AskForFriendshipEvent(MemberInfo source,
                                 String message) {
    	super(source);
        this.message = message;
	}

	public Set<FolderInfo> getJoinedFolders() {
        return joinedFolders;
    }

    public MemberInfo getMemberInfo() {
        return (MemberInfo) getSource();
    }

    public String getMessage() {
        return message;
    }
}
