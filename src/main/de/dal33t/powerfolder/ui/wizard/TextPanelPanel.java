/* $Id: TextPanelPanel.java,v 1.4 2005/06/12 22:57:10 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.util.StringTokenizer;

import javax.swing.Icon;
import javax.swing.JLabel;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;

/**
 * A general text panel, displays the given text and offers to finish wizard
 * process. No next panel
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class TextPanelPanel extends PFWizardPanel {
    private boolean initalized = false;

    private String title;
    private String text;

    public TextPanelPanel(Controller controller, String title, String text) {
        super(controller);
        this.title = title;
        this.text = text;
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        return null;
    }

    public boolean canFinish() {
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

        //setBorder(new TitledBorder(title));
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, pref, pref");

        PanelBuilder builder = new PanelBuilder(this, layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(title), cc.xy(4, 2));

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        // Add text as labels
        StringTokenizer nizer = new StringTokenizer(text, "\n");
        int y = 4;
        while (nizer.hasMoreTokens()) {
            String line = nizer.nextToken();
            builder.appendRow("pref");
            builder.addLabel(line, cc.xy(4, y));
            y++;
        }
        builder.appendRow("pref:grow");

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        // Init
    }
}