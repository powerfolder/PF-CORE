/**
 * Simple editor for IP address ranges.
 */
package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.net.AddressRange;

class AddressEditor extends BaseDialog {
	public enum EditorResult {
		OK, CANCEL
	};
	
	private JTextField inputLine;
	private JLabel errorLabel;
	public EditorResult result;
	public String addressRange;
	
	public AddressEditor(Controller controller) {
		this(controller, "");
	}
	
	public AddressEditor(Controller controller, String string) {
		super(controller, true, true);
		result = EditorResult.CANCEL;
		addressRange = string;
		initComponents();
	}

	public EditorResult showEditor(String title, String oldValue) {
		inputLine.setText(oldValue);
		setVisible(true);
		addressRange = inputLine.getText();
		return result;
	}

	private void initComponents() {
		FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref, 3dlu", 
			"3dlu, pref, 3dlu, pref, 3dlu");
		PanelBuilder builder = new PanelBuilder(layout);
		CellConstraints cc = new CellConstraints();
		
		inputLine = new JTextField();
		inputLine.setColumns(40);
		
		builder.add(inputLine, cc.xywh(2, 2, 3, 1));
	}

	@Override
	protected Component getButtonBar() {
		FormLayout layout = new FormLayout("pref, 3dlu, pref", 
			"pref");
		PanelBuilder builder = new PanelBuilder(layout);
		CellConstraints cc = new CellConstraints();
		builder.add(createCancelButton(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		}), cc.xy(1, 1));
		
		builder.add(createOKButton(new OKAction()), cc.xy(3, 1));
		
		return builder.getPanel();
	}

	@Override
	protected Component getContent() {
		FormLayout layout = new FormLayout("pref", 
		"pref, 3dlu, pref");
		PanelBuilder builder = new PanelBuilder(layout);
		CellConstraints cc = new CellConstraints();
		inputLine = new JTextField();
		inputLine.setColumns(40);
		inputLine.addActionListener(new OKAction());
		inputLine.setText(addressRange);

		errorLabel = new JLabel("");
		errorLabel.setForeground(Color.RED);
		
		builder.add(inputLine, cc.xy(1, 3));
		builder.add(errorLabel, cc.xy(1, 1));
		return builder.getPanel();
	}

	@Override
	protected Icon getIcon() {
		return null;
	}

	@Override
	public String getTitle() {
		return Translation.getTranslation("addressEditor.title");
	}
	
	private class OKAction implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			if (validateInput(inputLine.getText())) {
				result = EditorResult.OK;
				addressRange = inputLine.getText();
				close();
			}
		}
	}

	private boolean validateInput(String range) {
		try {
			return AddressRange.parseRange(range) != null;
		} catch (ParseException e) {
			errorLabel.setText(Translation.getTranslation("addressEditor.parseerror"));
			getUIComponent().pack();
		}
		return false;
	}
}