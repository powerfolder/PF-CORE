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
package de.dal33t.powerfolder.net;

/*
 * The DynDns interface is designed to provide a common protocol
 * for objects that wish to update DynDns IP address.
 * For example, DynDns is implemented by class DynDnsOrg.
 *
 * @author Albena Roshelova
 */


public interface DynDns {

    /*
     * Update the host IP with the DynDns
     *
     * @param   updateData: the DynDns account data (username, password, host).
     * @return  the server response.
     */
   public int update(DynDnsUpdateData updateData);

   /*
    * The method gets the user data: username, password, host, ip
    *
    * @return
    */

   public DynDnsUpdateData getDynDnsUpdateData();


   public void setDynDnsManager(DynDnsManager manager);


   /*
    * Gets the DynDns error
    *
    * @return  the error text.
    */
   public String getErrorText();


   /*
    * Gets the DynDns error
    *
    * @return the error text.
    */
   public String getErrorShortText();

}