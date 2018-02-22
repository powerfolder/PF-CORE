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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.jgoodies.binding.list.ObservableList;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.ui.model.SearchNodeTableModel;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.test.ControllerTestCase;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Tests the node table model.
 *
 * @see de.dal33t.powerfolder.ui.model.NodeTableModel
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class SearchNodeTableModelTest extends ControllerTestCase {
    private SearchNodeTableModel model;
    private MyTableModelListener listener;
    private Member moe;
    private Member homer;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        moe = getController().getNodeManager().addNode(
            new MemberInfo("Moe", IdGenerator.makeId(), null));
        homer = getController().getNodeManager().addNode(
            new MemberInfo("Homer", IdGenerator.makeId(), null));

        model = new SearchNodeTableModel(getController());
        listener = new MyTableModelListener();
        model.addTableModelListener(listener);
    }

    /**
     * Tests the direct mutation of the node table model via its own
     * modification methods.
     */
    public void testDirectChanges() {
        // Test = No users found
        assertEquals(1, model.getRowCount());
        assertTrue(model.getValueAt(0, 0) instanceof String);

        model.add(getController().getMySelf());
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(1, model.getRowCount());
        assertEquals(getController().getMySelf(), model.getDataAt(0));
        assertEquals(1, listener.events.size());

        model.add(getController().getMySelf());
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(2, model.getRowCount());
        assertEquals(getController().getMySelf(), model.getDataAt(1));
        assertEquals(2, listener.events.size());

        model.add(moe);
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(3, model.getRowCount());
        assertEquals(moe, model.getValueAt(2, 1));
        assertEquals(3, listener.events.size());
        assertTrue(model.contains(moe));

        model.remove(getController().getMySelf());
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(2, model.getRowCount());
        assertEquals(moe, model.getValueAt(1, 2));
        assertEquals(moe, model.getDataAt(1));
        assertEquals(4, listener.events.size());
        assertTrue(model.contains(getController().getMySelf()));

        model.clear();
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(1, model.getRowCount());
        assertTrue(model.getDataAt(0) instanceof String);
        assertEquals(5, listener.events.size());
        assertEquals(TableModelEvent.UPDATE, listener.lastEvent.getType());

        model.add(homer);
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(1, model.getRowCount());
        assertEquals(homer, model.getValueAt(0, 0));
        assertEquals(6, listener.events.size());
        assertTrue(model.contains(homer));
    }

    /**
     * Tests the mutation of the node table model via its listmodel.
     */
    public void testListModelChanges() {
        ObservableList<Member> list = model.getListModel();
        // Test = No users found
        assertEquals(1, model.getRowCount());
        assertTrue(model.getValueAt(0, 0) instanceof String);

        list.add(getController().getMySelf());
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(1, model.getRowCount());
        assertEquals(getController().getMySelf(), model.getDataAt(0));
        assertEquals(1, listener.events.size());

        list.add(getController().getMySelf());
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(2, model.getRowCount());
        assertEquals(getController().getMySelf(), model.getDataAt(1));
        assertEquals(2, listener.events.size());

        list.add(moe);
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(3, model.getRowCount());
        assertEquals(moe, model.getValueAt(2, 1));
        assertEquals(3, listener.events.size());
        assertTrue(model.contains(moe));

        list.remove(getController().getMySelf());
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(2, model.getRowCount());
        assertEquals(moe, model.getValueAt(1, 2));
        assertEquals(moe, model.getDataAt(1));
        assertEquals(4, listener.events.size());
        assertTrue(model.contains(getController().getMySelf()));

        list.clear();
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(1, model.getRowCount());
        assertTrue(model.getDataAt(0) instanceof String);
        assertEquals(5, listener.events.size());
        assertEquals(TableModelEvent.UPDATE, listener.lastEvent.getType());

        list.add(homer);
        // Give time for the listner event to be fired, which gets executed in
        // event dispatching thread.
        TestHelper.waitMilliSeconds(100);
        assertEquals(1, model.getRowCount());
        assertEquals(homer, model.getValueAt(0, 0));
        assertEquals(6, listener.events.size());
        assertTrue(model.contains(homer));
    }

    private static final class MyTableModelListener implements
        TableModelListener
    {
        public List<TableModelEvent> events = new ArrayList<TableModelEvent>();
        public TableModelEvent lastEvent;

        public void tableChanged(TableModelEvent e) {
            events.add(e);
            lastEvent = e;
        }
    }
}
