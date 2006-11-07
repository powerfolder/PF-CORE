package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.AddressRange;
import de.dal33t.powerfolder.util.ui.AddressEditor.EditorResult;

public class LANList extends PFComponent {
	private JPanel panel;
	private JList networklist;
	private JButton addButton, removeButton, editButton;
	private boolean modified;
	
	public LANList(Controller c) {
		super(c);
		modified = false;
		initComponents();
	}

	private void initComponents() {
		networklist = new JList(new DefaultListModel());
		networklist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		addButton = new JButton();
		addButton.setText(Translation
				.getTranslation("folderpanel.settingstab.addbutton.name"));
		removeButton = new JButton();
		removeButton.setText(Translation
				.getTranslation("folderpanel.settingstab.removebutton.name"));
		editButton = new JButton();
		editButton.setText(Translation
				.getTranslation("folderpanel.settingstab.editbutton.name"));
		
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AddressEditor editor = new AddressEditor(getController());
				editor.open();
				if (editor.result == EditorResult.OK) {
					modified = true;
					((DefaultListModel) networklist.getModel()).addElement(editor.addressRange);
				}
			}
		});
		
		editButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (networklist.isSelectionEmpty())
					return;
				AddressEditor editor = new AddressEditor(getController(),
						networklist.getSelectedValue().toString());
				editor.open();
				if (editor.result == EditorResult.OK) {
					((DefaultListModel) networklist.getModel()).set(networklist.getSelectedIndex(), editor.addressRange);
					modified = true;
				}
			}
		});
		
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (Object o: networklist.getSelectedValues()) {
					((DefaultListModel) networklist.getModel()).removeElement(o);
					modified = true;
				}
			}
		});
	}
	
	/**
	 * @return
	 */
	public JPanel getUIPanel() {
		if (panel == null) {
			FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu", 
					"3dlu, pref, 3dlu, pref, 3dlu");
			PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();
			builder.add(new JScrollPane(networklist), cc.xywh(2, 2, 5, 1));
			builder.add(addButton, cc.xy(2,4));
			builder.add(removeButton, cc.xy(4,4));
			builder.add(editButton, cc.xy(6,4));
			
			panel = builder.getPanel();
		}
		return panel;
	}
	
	public boolean save() {
		Object ips[] = ((DefaultListModel) networklist.getModel()).toArray();
		StringBuilder list = new StringBuilder();
		for (Object o: ips) {
			if (list.length() > 0) {
				list.append(", ");
			}
			list.append((String) o);
		}
		ConfigurationEntry.LANLIST.setValue(getController(), list.toString());
		return modified;
	}
	
	public void load() {
		String lanlist[] = ConfigurationEntry.LANLIST.getValue(getController()).split(",");
		for (String ip: lanlist) {
			AddressRange ar;
			try {
				ar = AddressRange.parseRange(ip);
			} catch (ParseException e) {
				log().warn("Invalid lanlist entry in configuration file!");
				continue;
			}
			((DefaultListModel) networklist.getModel()).addElement(ar.toString());
		}
	}
}
