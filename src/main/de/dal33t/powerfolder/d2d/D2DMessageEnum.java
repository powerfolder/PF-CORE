/*
 * Copyright 2015 Christian Sprajc. All rights reserved.
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
 * @author Christoph Kappel <kappel@powerfolder.com>
 * @version $Id$
 */

package de.dal33t.powerfolder.d2d;

import com.google.protobuf.AbstractMessage;

public interface
D2DMessageEnum<T extends enum<T>>
{
  /** createFromD2DMessage
   * Create from D2D message
   * @param  mesg  Message to use data from
   * @return New instance of type T
   **/

  public T createFromD2DMessage(AbstractMessage mesg);

  /** toD2DMessage
   * Convert to D2D message
   * @return Converted D2D message
   **/

  public AbstractMessage toD2DMessage();
}