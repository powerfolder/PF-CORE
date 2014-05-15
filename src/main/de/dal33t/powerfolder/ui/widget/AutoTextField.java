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
 * $Id: AutoTextField.java 8099 2009-05-27 15:09:23Z harry $
 */
package de.dal33t.powerfolder.ui.widget;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JTextField;
import javax.swing.text.*;

/**
 * Text field class that tries to match entered text against a list of values.
 * If it matches, it highlights the remaining matching text.
 */
public class AutoTextField extends JTextField {

    /** List of candidate values. */
    private final List<String> dataList = new CopyOnWriteArrayList<String>();

    /**
     * Constructor
     */
    public AutoTextField() {
        this(null);
    }

    /**
     * Constructor
     *
     * @param list
     */
    public AutoTextField(List<String> list) {
        if (list != null) {
            dataList.addAll(list);
        }
        setDocument(new AutoDocument());
    }

    /**
     * Try to match against an item in the list.
     *
     * @param text
     * @return
     */
    private String getMatch(String text) {
        for (String dataItem : dataList) {
            if (dataItem != null) {
                if (dataItem.toLowerCase().startsWith(text.toLowerCase())) {
                    return dataItem;
                }
            }
        }

        return "";
    }

    /**
     * Replace the items in the value list.
     *
     * @param list
     */
    public void setDataList(List<String> list) {
        if (list != null) {
            dataList.clear();
            dataList.addAll(list);
        }
    }

    /**
     * Replace a section in the text.
     *
     * @param content
     */
    public void replaceSelection(String content) {
        AutoDocument autoDocument = (AutoDocument) getDocument();
        try {
            int i = Math.min(getCaret().getDot(), getCaret().getMark());
            int j = Math.max(getCaret().getDot(), getCaret().getMark());
            autoDocument.replace(i, j - i, content, null);
        } catch (Exception exception) {
        }
    }

    public String getText() {
        try {
            String text = getDocument().getText(0, getDocument().getLength());
            // Try to get correct case match from list.
            for (String item : dataList) {
                if (item != null && item.equalsIgnoreCase(text)) {
                    return item;
                }
            }
            return text;
        } catch (Exception e) {
            return "";
        }
    }

    public void clear() {
        try {
            getDocument().remove(0, getDocument().getLength());
        } catch (BadLocationException e) {
            // Ignore
        }
    }

    /**
     * Autodocument extension that highlights the best match.
     */
    private class AutoDocument extends PlainDocument {

        public void replace(int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException
        {
            super.remove(offset, length);
            insertString(offset, text, attrs);
        }

        public void insertString(int offs, String str, AttributeSet a)
            throws BadLocationException
        {
            if (str == null || str.length() == 0) {
                return;
            }
            String startText = getText(0, offs);
            String match = getMatch(startText + str);

            // No match? Just enter text.
            if (match.length() == 0) {
                super.insertString(offs, str, a);
                return;
            }

            // Enter match and highlight the guessed part.
            super.remove(0, getLength());

            // Enter the inserted text, then the remaining match text.
            // Must do this to preserve entered upper/lower case.
            super.insertString(0, startText, a);
            super.insertString(offs, str, a);
            super.insertString(offs + str.length(), match.substring(offs
                + str.length(), match.length()), a);

            // Select the guessed bit.
            setSelectionStart(offs + str.length());
            setSelectionEnd(getLength());
        }

        public void remove(int offs, int len) throws BadLocationException {

            // Do not match if everything is deleted.
            if (offs == 0 && len == getLength()) {
                super.remove(0, getLength());
                return;
            }

            int selectionStart = getSelectionStart();
            if (selectionStart > 0) {
                selectionStart--;
            }

            // All text removed. Display blank.
            if (selectionStart == 0) {
                super.remove(0, getLength());
                return;
            }

            // Try to match on text entered so far.
            String match = getMatch(getText(0, selectionStart));
            if (match.length() == 0) {
                // No match. Just remove the text.
                super.remove(offs, len);
            } else {
                String text = getText(0, getLength());
                super.remove(0, getLength());

                // Enter the existing text, then the remaining match text.
                // Must do this to preserve entered upper/lower case.
                super.insertString(0, text.substring(0, offs), null);
                super.insertString(offs, match.substring(offs, match.length()),
                    null);
            }
            try {
                setSelectionStart(selectionStart);
                setSelectionEnd(getLength());
            } catch (Exception exception) {
                // Don't really care.
            }
        }
    }
}
