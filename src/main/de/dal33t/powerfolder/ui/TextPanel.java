package de.dal33t.powerfolder.ui;

import java.awt.Dimension;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.StyledDocument;

import de.dal33t.powerfolder.util.ui.UIPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Displays text
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.6 $
 */
public class TextPanel implements UIPanel {
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
