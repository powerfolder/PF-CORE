package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.AddressEditor.EditorResult;

public class LANList extends PFComponent {
	private JPanel panel;
	private JList networklist;
	private JButton addButton, removeButton, editButton;
	
	public LANList(Controller c) {
		super(c);
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
				}
			}
		});
		
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (Object o: networklist.getSelectedValues()) {
					((DefaultListModel) networklist.getModel()).removeElement(o);
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
}
