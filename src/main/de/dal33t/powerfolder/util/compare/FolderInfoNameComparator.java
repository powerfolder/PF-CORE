package de.dal33t.powerfolder.util.compare;

import java.util.Comparator;

import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.logging.Loggable;

public final class FolderInfoNameComparator  extends Loggable implements
    Comparator<FolderInfo> {
    public static final FolderInfoNameComparator INSTANCE = new FolderInfoNameComparator();

    private FolderInfoNameComparator() {
    }

    public int compare(final FolderInfo o1, final FolderInfo o2) {
        if (o1.getName() == null) {
            return -1;
        }
        if (o2.getName() == null) {
            return 1;
        }
        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
    }
}
