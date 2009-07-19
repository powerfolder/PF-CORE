/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.clientserver;

/**
 * A provider to retrieve services.
 * 
 * @author sprajc
 */
public interface ServiceProvider {

    /**
     * Retrieves a service
     * 
     * @param <T>
     *            The interface class of the service
     * @param controller
     *            the controller
     * @param serviceInterface
     * @return the service interface
     */
    <T> T getService(Class<? extends T> serviceInterface);

}
