package de.dal33t.powerfolder.ui.information.folder.members;

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

import java.util.Comparator;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.security.FolderPermission;

/**
 * Comparator for members of a folder.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public enum FolderMemberComparator implements Comparator<FolderMember> {

    /** Sorts members by connected / disconnected and friend / non-friend. */
    BY_TYPE() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            Member member1 = o1.getMember();
            Member member2 = o2.getMember();
            if (member1 == null) {
                return member2 == null ? 0 : -1;
            }
            if (member2 == null) {
                return 1;
            }
            // Sort by type.
            boolean m1f = member1.isFriend();
            boolean m2f = member2.isFriend();
            boolean m1cc = member1.isCompletelyConnected()
                || member1.isMySelf();
            boolean m2cc = member2.isCompletelyConnected()
                || member2.isMySelf();
            if (m1f != m2f) {
                return m1f ? 1 : -1;
            }
            if (m1cc != m2cc) {
                return m1cc ? 1 : -1;
            }
            return 0;
        }
    },

    BY_COMPUTER_NAME() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            Member member1 = o1.getMember();
            Member member2 = o2.getMember();
            if (member1 == null) {
                return member2 == null ? 0 : 1;
            }
            if (member2 == null) {
                return -1;
            }
            // Sort by nick name
            return member1.getNick().compareTo(member2.getNick());
        }
    },

    BY_SYNC_STATUS() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            Double size1 = o1.getFolder().getStatistic().getSyncPercentage(
                o1.getMember());
            Double size2 = o2.getFolder().getStatistic().getSyncPercentage(
                o2.getMember());
            if (size1 == null) {
                return size2 == null ? 0 : -1;
            }
            if (size2 == null) {
                return 1;
            }
            return size1.compareTo(size2);
        }
    },

    BY_LOCAL_SIZE() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            Long size1 = o1.getFolder().getStatistic().getSizeInSync(
                o1.getMember());
            Long size2 = o2.getFolder().getStatistic().getSizeInSync(
                o2.getMember());
            if (size1 == null) {
                return size2 == null ? 0 : 1;
            }
            if (size2 == null) {
                return -1;
            }
            return size1.compareTo(size2);
        }
    },

    BY_USERNAME() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            AccountInfo a1 = o1.getAccountInfo();
            AccountInfo a2 = o2.getAccountInfo();
            if (a1 == null) {
                return a2 == null ? 0 : 1;
            }
            if (a2 == null) {
                return -1;
            }
            if (a1.getUsername() == null) {
                return a2.getUsername() == null ? 0 : 1;
            }
            if (a2.getUsername() == null) {
                return -1;
            }
            return a1.getUsername().compareTo(a2.getUsername());
        }
    },

    BY_DISPLAY_NAME() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            AccountInfo a1 = o1.getAccountInfo();
            AccountInfo a2 = o2.getAccountInfo();
            if (a1 == null) {
                return a2 == null ? 0 : 1;
            }
            if (a2 == null) {
                return -1;
            }
            if (a1.getDisplayName() == null) {
                return a2.getDisplayName() == null ? 0 : 1;
            }
            if (a2.getDisplayName() == null) {
                return -1;
            }
            return a1.getDisplayName().compareTo(a2.getDisplayName());
        }
    },

    BY_PERMISSION() {
        @Override
        public int compare(FolderMember o1, FolderMember o2) {
            FolderPermission fp1 = o1.getPermission();
            FolderPermission fp2 = o2.getPermission();
            if (fp1 == null) {
                return fp2 == null ? 0 : -1;
            }
            if (fp2 == null) {
                return 1;
            }
            return fp1.getName().compareTo(fp2.getName());
        }
    }

    ;

    public int compare(FolderMember o1, FolderMember o2) {
        throw new UnsupportedOperationException("Not implemented");
    }
}