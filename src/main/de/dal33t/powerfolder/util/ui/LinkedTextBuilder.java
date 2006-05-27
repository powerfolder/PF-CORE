package de.dal33t.powerfolder.util.ui;

import javax.swing.JLabel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * helper class to create a text with links.
 * 
 * @version $Revision: 1.1 $
 */
public class LinkedTextBuilder {

    /**
     * All lines starting with http:// are transformed into linkLabels.
     * 
     * @return PanelBuilder containing the generated JPanel
     */
    public static PanelBuilder build(String text) {
        int row = 1;
        CellConstraints cc = new CellConstraints();
        String[] txtTokens;

        FormLayout layout = new FormLayout("pref");
        PanelBuilder builder = new PanelBuilder(layout);
        // split into tokens
        txtTokens = text.split("\n"); // text items separated by \n

        for (int i = 0; i < txtTokens.length; i++) {
            String lineText = txtTokens[i];
            // Make it simple stoopid. A line can be a link or a text.
            // Simplifies things much
            if (lineText.toLowerCase().startsWith("http://")) {
                builder.appendRow("pref");
                builder.add(new LinkLabel(txtTokens[i], txtTokens[i]), cc.xy(1,
                    row));
            } else if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("4dlu");
            } else {
                builder.appendRow("pref");
                builder.add(new JLabel(txtTokens[i]), cc.xy(1, row));
            }
            row += 1;
        }
        return builder;
    }
}
