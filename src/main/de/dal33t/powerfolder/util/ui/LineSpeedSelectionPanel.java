package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.*;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Panel with a combobox for selecting the line speed and a textfield for
 * entering upload speed.
 * Editing the textfield is only possible if Custom linespeed was chosen first.
 * 
 * @author Bytekeeper
 * @version $revision$
 */
public class LineSpeedSelectionPanel extends JPanel {
    private final Logger LOG = Logger.getLogger(LineSpeedSelectionPanel.class);
	
	/**
     * Container holding the description and upload rate. 
	 */
	public static class LineSpeed {
		private long uploadSpeed = 0;
        private long downloadSpeed = 0;
		private String desc;
        private boolean editable;
		
		/**
         * Creates a new LineSpeed
		 * @param desc the "name" of the speed value
		 * @param uploadSpeed a value >= 0. If this value is below 0 
         *      the user may enter a speed in Kilobytes per second.
		 */
		public LineSpeed(String desc, long uploadSpeed, long downloadSpeed, boolean editable) {
			this.desc = desc;
			this.uploadSpeed = uploadSpeed;
            this.downloadSpeed = downloadSpeed;
            this.editable = editable;
		}
		
		public long getUploadSpeed() {
			return uploadSpeed;
		}
		
		public String toString() {
			return desc;
		}

        public boolean isEditable() {
            return editable;
        }

        public long getDownloadSpeed() {
            return downloadSpeed;
        }
	}
	
	private JComboBox speedSelectionBox;
	private JFormattedTextField customUploadSpeed, customDownloadSpeed;
    private LineSpeed defaultSpeed;
	
	/**
	 * Constructs a new LineSpeedSelectionPanel.
	 */
	public LineSpeedSelectionPanel() {
		initComponents();
	}

	protected  void initComponents() {
		setOpaque(false);
		
		FormLayout layout = new FormLayout("pref, 3dlu, pref:grow, 3dlu, pref", "pref, 3dlu, pref, 3dlu, pref");
		setLayout(layout);

		speedSelectionBox = new JComboBox();

		customUploadSpeed = new JFormattedTextField();
        customDownloadSpeed = new JFormattedTextField();

		CellConstraints cc = new CellConstraints();
		
		add(speedSelectionBox, cc.xywh(1, 1, 5, 1));
		
		add(new JLabel(Translation.getTranslation("linespeed.uploadspeed")), cc.xy(1, 3));
		
		add(customUploadSpeed, cc.xy(3, 3));

		add(new JLabel("KB/s"), cc.xy(5,3));

        add(new JLabel(Translation.getTranslation("linespeed.downloadspeed")), cc.xy(1, 5));
        
        add(customDownloadSpeed, cc.xy(3, 5));

        add(new JLabel("KB/s"), cc.xy(5,5));
		
		speedSelectionBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (((LineSpeed) speedSelectionBox.getSelectedItem()).isEditable()) {
					customUploadSpeed.setEnabled(true);
                    customDownloadSpeed.setEnabled(true);
                } else {
					customUploadSpeed.setEnabled(false);
                    customDownloadSpeed.setEnabled(false);
					
					customUploadSpeed.setText(Long.toString(((LineSpeed)speedSelectionBox.getSelectedItem()).getUploadSpeed()));
                    customDownloadSpeed.setText(Long.toString(((LineSpeed)speedSelectionBox.getSelectedItem()).getDownloadSpeed()));
				}
			}
		});
	}

    /**
     * Loads the selection with the default values for LAN
     */
    public void loadLANSelection() {
        addLineSpeed("linespeed.lan10", 1000, 0);
        addLineSpeed("linespeed.lan100", 10000, 0);
        addLineSpeed("linespeed.lan1000", 100000, 0);
        addLineSpeed("linespeed.unlimited", 0, 0);
        setDefaultLineSpeed(
            addLineSpeed("linespeed.customspeed", 0, 0, true));
    }
    
    /**
     * Loads the selection with the default values for WAN
     */
    public void loadWANSelection() {
        addLineSpeed("linespeed.isdnspeed64", 5, 0);
        addLineSpeed("linespeed.isdnspeed128", 11, 0);
        addLineSpeed("linespeed.adsl128", 11, 0);
        addLineSpeed("linespeed.adsl256", 23, 0);
        addLineSpeed("linespeed.adsl512", 46, 0);
        addLineSpeed("linespeed.adsl768", 69, 0);
        addLineSpeed("linespeed.T1", 140, 0);
        addLineSpeed("linespeed.T3", 3930, 0);
        addLineSpeed("linespeed.unlimited", 0, 0);
        setDefaultLineSpeed(
            addLineSpeed("linespeed.customspeed", 0, 0, true));
    }
    
    /**
     * Returns the default "fallback" linespeed if one was set, otherwise
     * returns the current selected speed.
     * @return
     */
    public LineSpeed getDefaultLineSpeed() {
        return defaultSpeed != null ? defaultSpeed :
            (LineSpeed) speedSelectionBox.getSelectedItem();
    }
    
    /**
     * Sets the default "fallback" linespeed. 
     * @param speed the LineSpeed or null it should be cleared
     */
    public void setDefaultLineSpeed(LineSpeed speed) {
        defaultSpeed = speed;
    }
    
    /**
     * Creates a LineSpeed instance with the given parameters then adds and
     * returns it.
     * @param descr the translation property's name whose value will be used
     * @param speed
     * @return
     */
    public LineSpeed addLineSpeed(String descr, long uploadSpeed, long downloadSpeed) {
        return addLineSpeed(descr, uploadSpeed, downloadSpeed, false);
    }
    
    /**
     * Creates a LineSpeed instance with the given parameters then adds and
     * returns it.
     * @param descr the translation property's name whose value will be used
     * @param speed
     * @param editable true if the user should be allowed to modify the 
     *  upload speed setting. (The value of LineSpeed.uploadSpeed remains
     *  untouched)
     * @return
     */
    public LineSpeed addLineSpeed(String descr, long uploadSpeed, long downloadSpeed, boolean editable) {
        LineSpeed ls = new LineSpeed(Translation.getTranslation(descr), uploadSpeed, downloadSpeed, editable);
        addLineSpeed(ls);
        return ls;
    }
    
    /**
     * Adds the given LineSpeed to the selection list. 
     * @param speed
     */
    public void addLineSpeed(LineSpeed speed) {
        speedSelectionBox.addItem(speed);
    }
    
    /**
     * Removes the given LineSpeed from the selection list. 
     * @param speed
     */
    public void removeLineSpeed(LineSpeed speed) {
        speedSelectionBox.removeItem(speed);
    }
    
	/**
	 * Updates the panel by selecting the correct Item for the given speed and also
	 * updates the custom value field with that value.
	 * TODO: Since some lines might have the same upload limit (like ISDN/DSL) this
	 * method currenlty selects the first matching item.  
	 * @param speed the speed in kb/s, 0 for unlimited
	 */
	public void setSpeedKBPS(long uploadSpeed, long downloadSpeed) {
		// Find the "best" item to select for the given speed
		// if none matches, falls thru tu "Custom"
		for (int i = 0; i < speedSelectionBox.getItemCount(); i++) {
            LineSpeed ls = (LineSpeed) speedSelectionBox.getItemAt(i);
			if (ls.getUploadSpeed() == uploadSpeed &&
                ls.getDownloadSpeed() == downloadSpeed) {
				speedSelectionBox.setSelectedItem(ls);
				break;
			}
		}
		
		customUploadSpeed.setValue(uploadSpeed);
        customDownloadSpeed.setValue(downloadSpeed);
        if (((LineSpeed) speedSelectionBox.getSelectedItem()).getUploadSpeed() 
            != uploadSpeed || ((LineSpeed) speedSelectionBox.getSelectedItem()).getDownloadSpeed() 
            != downloadSpeed)
            speedSelectionBox.setSelectedItem(getDefaultLineSpeed());
	}
	
	/**
	 * Returns the currently selected upload speed.
	 * @return The upload speed in kb/s or a number < 0 if an error occured
	 */
	public long getUploadSpeedKBPS() {
		try {
			return (Long)customUploadSpeed.getFormatter().stringToValue(customUploadSpeed.getText()) * 1024;
		} catch (ParseException e) {
            LOG.warn(
                    "Unable to parse uploadlimit '" + customUploadSpeed.getText() + "'");
		}
		return -1;
	}

    /**
     * Returns the currently selected download speed.
     * @return The download speed in kb/s or a number < 0 if an error occured
     */
    public long getDownloadSpeedKBPS() {
        try {
            return (Long)customDownloadSpeed.getFormatter().stringToValue(customDownloadSpeed.getText()) * 1024;
        } catch (ParseException e) {
            LOG.warn(
                    "Unable to parse downloadlimit '" + customDownloadSpeed.getText() + "'");
        }
        return -1;
    }
}
