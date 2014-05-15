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
package de.dal33t.powerfolder.security;

/**
 * Empty/Null account to avoid NPEs on not logged in users.
 * <p>
 * Its the default account for every unauthenticated call.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public class AnonymousAccount extends Account {

    private static final long serialVersionUID = 100L;

    public AnonymousAccount() {
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String toString() {
        return "Anonymous: " + super.toString();
    }

}
