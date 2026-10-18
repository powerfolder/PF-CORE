/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.security;

import de.dal33t.powerfolder.util.Reject;
import java.io.Serializable;

/**
 * An abstract permission. Basically the right to do/view or perform something.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface Permission extends Serializable {

    /**
     * PFS-5818: Rejects an id that cannot be told apart inside a permission id.
     * <p>
     * A permission is stored as {@code <oid><separator><class>} and searched for by the anchored
     * prefix {@code <oid><separator>%}. An oid CONTAINING a separator makes that search ambiguous in
     * the database itself: the pattern of a folder "A" also matches the permission of a folder
     * literally named {@code A_FP_B}, and no parsing can tell the two apart. So the id is refused
     * where it is minted - in the constructor of what carries it - and never reaches a permission.
     * <p>
     * Practically unreachable: the separators are upper case while a migrated id carries a lower case
     * Alfresco short name, and the comparison is case sensitive. Loud is still better than a
     * permission that quietly answers for the wrong folder.
     *
     * @param oid  the id about to be given to a folder, group or organization
     * @param what what the id belongs to, for the message
     **/
    static void rejectSeparatorIn(String oid, String what) {
        if (oid == null) {
            return;
        }
        for (String separator : new String[] {FolderPermission.ID_SEPARATOR,
            GroupAdminPermission.ID_SEPARATOR, OrganizationAdminPermission.ID_SEPARATOR})
        {
            Reject.ifTrue(oid.contains(separator), what + " id '" + oid
                + "' contains the permission id separator '" + separator
                + "' - a permission on it could not be told from another one's");
        }
    }

    public static long serialVersionUID = -7019372990245242530l;

    boolean implies(Permission impliedPermision);

    String getId();

    /**
     * MUST be implemented
     *
     * @param other
     * @return
     */
    boolean equals(Object other);
}
