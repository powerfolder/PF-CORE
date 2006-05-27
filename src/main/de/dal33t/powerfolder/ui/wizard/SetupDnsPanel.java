 /* $Id: SetupDnsPanel.java,v 1.14 2005/11/04 14:11:34 schaatser Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.LinkLabel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * Panel which is used to configure the dyndns
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.14 $
 */
public class SetupDnsPanel extends PFWizardPanel {
    private boolean initalized = false;
    private boolean dnsValidateOK = false;
    private JButton dnsValidateBtn;
    private JTextField dnsField;


    public SetupDnsPanel(Controller controller) {
        super(controller);

    }

    /**
     * Validates the settings before saving them pesistantly
     * 
     * @return the ui component which validation has failed (giving a chance to
     *         the caller to give it the input focus) or null if all is good.
     */
    private JComponent validateSettings() {
         if (!getController().getDynDnsManager().validateDynDns(dnsField.getText())) {
  		      return dnsField;
        }

        // all validations have passed
        return null;
    }

    // Application logic ******************************************************

    /**
     * Saves the dns
     */
    private void saveDns() {
        //  Save dyndns
        log().warn("Setting up dyndns with '" + dnsField.getText() + "'");
        if (!StringUtils.isBlank(dnsField.getText())) {
            getController().getConfig().put("mydyndns", dnsField.getText());
        } else {
            getController().getConfig().remove("mydyndns");
        }
        // Save config
        getController().saveConfig();
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return dnsValidateOK;
    }

    public WizardPanel next() {
        // save
        saveDns();

        // Displays the sucess panel
        return (WizardPanel) getWizardContext().getAttribute(
            PFWizard.SUCCESS_PANEL);
    }

    public boolean canFinish() {
        // Can finish
        return true;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        //setBorder(new TitledBorder("Setup (dynamic) dns"));
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 15dlu, left:pref, 60dlu, pref:grow",
            "5dlu, pref, 15dlu, pref, pref, pref, pref, 4dlu, pref, 4dlu, pref, pref:grow");

        PanelBuilder builder = new PanelBuilder(this, layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.setupdns.title")), cc.xy(4, 2));

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.addLabel(Translation
            .getTranslation("wizard.setupdns.nodnssetup"), cc.xy(4, 4));

        builder.addLabel(Translation
            .getTranslation("wizard.setupdns.improvesread"), cc.xy(4, 5));

        LinkLabel dyndnsLink = new LinkLabel(Translation
            .getTranslation("wizard.setupdns.dyndnshomepage"),
            "http://www.dyndns.org");
        builder.add(dyndnsLink, cc.xy(4, 6));

        builder
            .addLabel(Translation.getTranslation("wizard.setupdns.enterdns"),
                cc.xy(4, 7));

        builder.add(dnsField, cc.xy(4, 9));
        builder.add(dnsValidateBtn, cc.xy(4, 11));

        // initalized
        initalized = true;
    }

    /**
     * Creates an internationlaized ok button
     * 
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    protected JButton createValidateButton(ActionListener listener) {
        JButton btn = new JButton("validate");
        btn.addActionListener(listener);
        return btn;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        // Create text field
        dnsField = SimpleComponentFactory.createTextField(true);
        // Listen to the changes in the field and
        // disable the next button enforcing the user to
        // validate the new dyndns entered
        dnsField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent evt) {
                dnsValidateOK = false;
                updateButtons();
            }

            public void removeUpdate(DocumentEvent evt) {
                dnsValidateOK = false;
                updateButtons();
            }

            public void changedUpdate(DocumentEvent evt) {
                dnsValidateOK = false;
                updateButtons();
            }
        });
        dnsField.setText(getController().getConfig().getProperty("mydyndns"));
        Util.ensureMinimumWidth(107, dnsField);
        dnsValidateBtn = new JButton("validate");
        dnsValidateBtn = createValidateButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread("Dyndns validator") {
                    public void run() {
                        JComponent failedComponent = validateSettings();
                        if (failedComponent == null) {
                            dnsValidateOK = true;
                            updateButtons();
                        } else {
                            dnsValidateOK = false;
                            updateButtons();

                            failedComponent.grabFocus();
                            if (failedComponent instanceof JTextComponent) {
                                // if it is a text component then also select
                                // all the text
                                // in it.
                                JTextComponent comp = (JTextComponent) failedComponent;
                                comp.selectAll();
                            }
                        }
                    }
                }.start();
            }
        });
    }
}