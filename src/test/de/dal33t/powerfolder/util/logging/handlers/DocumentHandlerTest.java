/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.util.logging.handlers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.text.StyledDocument;

import org.junit.jupiter.api.Test;

/**
 * Test for the batched appends of {@link DocumentHandler}.
 */
public class DocumentHandlerTest {

    @Test
    public void testRecordsAreAppendedInOrder() throws Exception {
        DocumentHandler handler = new DocumentHandler();
        handler.publish(record(Level.INFO, "first-message"));
        handler.publish(record(Level.WARNING, "second-message"));
        handler.publish(record(Level.SEVERE, "third-message"));
        String text = awaitText(handler.getLogBuffer(), "third-message");
        assertTrue(text.indexOf("first-message") < text.indexOf("second-message"), text);
        assertTrue(text.indexOf("second-message") < text.indexOf("third-message"), text);
    }

    @Test
    public void testBufferStaysBounded() throws Exception {
        DocumentHandler handler = new DocumentHandler();
        for (int i = 0; i < 3 * DocumentHandler.MAX_PENDING_RECORDS; i++) {
            handler.publish(record(Level.INFO, "record " + i));
        }
        handler.publish(record(Level.INFO, "last-record"));
        awaitText(handler.getLogBuffer(), "last-record");
        // The flush (insert + trim) is one EDT event; wait for it to finish.
        java.awt.EventQueue.invokeAndWait(() -> { });
        assertTrue(handler.getLogBuffer().getLength() <= 50000, "length " + handler.getLogBuffer().getLength());
    }

    private static LogRecord record(Level level, String message) {
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(DocumentHandlerTest.class.getName());
        return record;
    }

    private static String awaitText(StyledDocument doc, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        String text = "";
        while (System.currentTimeMillis() < deadline) {
            text = doc.getText(0, doc.getLength());
            if (text.contains(expected)) {
                return text;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("'" + expected + "' not appended: " + text);
    }
}
