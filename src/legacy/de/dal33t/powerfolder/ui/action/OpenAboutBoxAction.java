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
package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.dialog.AboutDialog;

import java.awt.event.ActionEvent;

/**
 * Creates an Action event that displays the About Box dialog.
 * 
 * @author <a href=mailto:xsj@users.sourceforge.net">Daniel Harabor</a>
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version 1.0 Last Modified: 10/04/05
 */
public class OpenAboutBoxAction extends BaseAction {
    public OpenAboutBoxAction(Controller controller) {
        super("action_open_about_box", controller);
    }

    public void actionPerformed(ActionEvent e) {
        new AboutDialog(getController()).open();
    }
}