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
package de.dal33t.powerfolder.util.compare;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderStatistic;

import java.util.Comparator;

/**
 * Comparator for members of a folder
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.12 $
 */
public class FolderMemberComparator implements Comparator {

    private Folder folder;

    /** Sorts members by connected / disconnected and friend / non-friend. */
    public static final FolderMemberComparator BY_TYPE =
            new FolderMemberComparator(0);

    /** Sorts members by nick name */
    public static final FolderMemberComparator BY_NICK =
            new FolderMemberComparator(1);

    /** Sorts nodes by sync status */
    public static final FolderMemberComparator BY_SYNC_STATUS =
            new FolderMemberComparator(2);

    /** Sorts nodes by local size */
    public static final FolderMemberComparator BY_LOCAL_SIZE =
            new FolderMemberComparator(3);

    private int type;

    private FolderMemberComparator(int type) {
        this.type = type;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public int compare(Object o1, Object o2) {
        if (o1 instanceof Member && o2 instanceof Member) {
            Member member1 = (Member) o1;
            Member member2 = (Member) o2;

            if (type == 0) {
                // Sort by type.
                boolean m1f = member1.isFriend();
                boolean m2f = member2.isFriend();
                boolean m1cc = member1.isCompleteyConnected()
                        || member1.isMySelf();
                boolean m2cc = member2.isCompleteyConnected()
                        || member2.isMySelf();
                if (m1f != m2f) {
                    return m1f ? 1 : -1;
                }
                if (m1cc != m2cc) {
                    return m1cc ? 1 : -1;
                }
                return 0;
            } else if (type == 1) {
                // Sort by nick name
                return member1.getNick().compareTo(member2.getNick());
            } else if (type == 2) {
                // Sort by sync status
                FolderStatistic statistic = folder.getStatistic();
                Long size1 = statistic.getSizeInSync(member1);
                Long size2 = statistic.getSizeInSync(member2);
                return size1.compareTo(size2);
            } else if (type == 3) {
                // Sort by local size
                FolderStatistic statistic = folder.getStatistic();
                Double syncPerc1 = statistic.getSyncPercentage(member1);
                Double syncPerc2 = statistic.getSyncPercentage(member2);
                return syncPerc1.compareTo(syncPerc2);
            }
        }
        return 0;
    }
}