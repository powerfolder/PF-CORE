/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 *
 * From http://java.sun.com/docs/books/tutorial/index.html
 *
 * http://www.java2s.com/Code/Java/Swing-JFC/AutocompleteTextField.htm
 */

package com.java2s;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JTextField;
import javax.swing.text.*;

public class Java2sAutoTextField extends JTextField {

    private final List<String> dataList = new CopyOnWriteArrayList<String>();

    public Java2sAutoTextField(List<String> list) {
        if (list != null) {
            dataList.addAll(list);
            setDocument(new AutoDocument());
        }
    }

    private String getMatch(String s) {
        for (String aDataList : dataList) {
            if (aDataList != null) {
                if (aDataList.toLowerCase().startsWith(s.toLowerCase())) {
                    return aDataList;
                }
            }
        }

        return null;
    }

    public void replaceSelection(String content) {
        AutoDocument autoDocument = (AutoDocument) getDocument();
        if (autoDocument != null) {
            try {
                int i = Math.min(getCaret().getDot(), getCaret().getMark());
                int j = Math.max(getCaret().getDot(), getCaret().getMark());
                autoDocument.replace(i, j - i, content, null);
            } catch (Exception exception) {
            }
        }
    }

    public void setDataList(List<String> list) {
        if (list != null) {
            dataList.clear();
            dataList.addAll(list);
        }
    }

    private class AutoDocument extends PlainDocument {

        public void replace(int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            super.remove(offset, length);
            insertString(offset, text, attrs);
        }

        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null || str.length() == 0) {
                return;
            }
            String startText = getText(0, offs);
            String match = getMatch(startText + str);
            int selectionStart = offs + str.length();
            if (match == null) {
                super.insertString(offs, str, a);
                return;
            }
            super.remove(0, getLength());
            super.insertString(0, match, a);
            setSelectionStart(selectionStart);
            setSelectionEnd(getLength());
        }

        public void remove(int offs, int len) throws BadLocationException {
            int selectionStart = getSelectionStart();
            if (selectionStart > 0) {
                selectionStart--;
            }
            String match = getMatch(getText(0, selectionStart));
            if (match == null) {
                super.remove(offs, len);
            } else {
                super.remove(0, getLength());
                super.insertString(0, match, null);
            }
            try {
                setSelectionStart(selectionStart);
                setSelectionEnd(getLength());
            } catch (Exception exception) {
            }
        }

    }

}

