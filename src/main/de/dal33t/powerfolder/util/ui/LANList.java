package de.dal33t.powerfolder.util.ui;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Translation;

public class LANList extends PFComponent {
	private JPanel panel;
	private JList networklist;
	private JButton addButton, removeButton, editButton;
	
	public LANList() {
		initComponents();
	}

	private void initComponents() {
		networklist = new JList();
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
	
	private class AddressEditor extends JDialog {
		private AddressEditor() {
			super((JFrame) null, false);
		}
	}
}
