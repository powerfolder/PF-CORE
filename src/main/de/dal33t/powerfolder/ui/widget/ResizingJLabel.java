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
 * $Id: ResizingJLabel.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Dimension;

import javax.swing.JLabel;

/**
 * Class that overrides 'get...Size()' so that text is truncated if the
 * component is too narrow.
 */
public class ResizingJLabel extends JLabel {

    public Dimension getPreferredSize() {
        return new Dimension(0, super.getPreferredSize().height);
    }

    public Dimension getSize() {
        return new Dimension(0, super.getSize().height);
    }

    public Dimension getMinimumSize() {
        return new Dimension(0, super.getMinimumSize().height);
    }

    public Dimension getMaximumSize() {
        return new Dimension(0, super.getMaximumSize().height);
    }

}
