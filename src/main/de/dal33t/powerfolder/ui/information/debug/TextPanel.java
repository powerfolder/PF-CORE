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
package de.dal33t.powerfolder.ui.information.debug;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.StyledDocument;

import de.dal33t.powerfolder.ui.util.UIUtil;

/**
 * Displays text
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.6 $
 */
public class TextPanel {
    private AutoScrollDocumentListner docListener;
    private JTextPane textArea;
    private JScrollPane textAreaPane;
    private boolean autoScroll;

    public JComponent getUIComponent() {
        if (textAreaPane == null) {
            initComponents();
        }
        return textAreaPane;
    }

    private void initComponents() {
        textArea = new JTextPane() {
            // Override, otherwise textpane will wrap text at end of line,
            // instead of let the scollpane display scrollbar
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }

            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                if (getParent() instanceof JViewport) {
                    dim.width = Math.max(getParent().getWidth(), dim.width);
                }
                return dim;
            }
        };
        textAreaPane = new JScrollPane(textArea);
        textArea.setEditable(false);
        UIUtil.removeBorder(textAreaPane);
        UIUtil.setZeroWidth(textAreaPane);

        docListener = new AutoScrollDocumentListner();

    }

    public void setText(StyledDocument doc, boolean autoScroll) {
        this.autoScroll = autoScroll;
        if (textArea == null) {
            initComponents();
        }
        // Remove from old document
        textArea.getDocument().removeDocumentListener(docListener);
        textArea.setDocument(doc);

        if (autoScroll) {
            doc.addDocumentListener(docListener);
            docListener.insertUpdate(null);
        }

    }

    /**
     * For scrolling the document automatically
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class AutoScrollDocumentListner implements DocumentListener {
        public void changedUpdate(DocumentEvent e) {
        }

        public void insertUpdate(DocumentEvent e) {
            if (autoScroll) {
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
        }

        public void removeUpdate(DocumentEvent e) {
            if (autoScroll) {
                textArea.setCaretPosition(textArea.getDocument().getLength());
            }
        }
    }

    public void setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
    }

    public boolean isAutoScroll() {
        return autoScroll;
    }
}
