package de.dal33t.powerfolder.util.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class TextLinesPanelBuilder {
    public static JPanel createTextPanel(String text, int fontsize) {
        // split into tokens
        String contentsArray[] = text.split("\n");
        FormLayout contentsForm = new FormLayout("pref");
        PanelBuilder builder = new PanelBuilder(contentsForm);
        
        int row = 1;
        CellConstraints cc = new CellConstraints();

        for (int i = 0; i < contentsArray.length; i++) {
            String lineText = contentsArray[i];
            if (StringUtils.isEmpty(lineText.trim())) {
                // Add gap
                builder.appendRow("4dlu");
            } else {
                builder.appendRow("pref");
                JLabel label = new JLabel("<HTML><BODY>" + contentsArray[i]
                    + "</BODY></HTML>");
                Font font = new Font(label.getFont().getFontName(), Font.BOLD,
                    fontsize);
                label.setFont(font);
                builder.add(label, cc.xy(1, row));
            }
            row += 1;
        }
        JPanel textBoxPanel = builder.getPanel();
        textBoxPanel.setBackground(Color.WHITE);
        return textBoxPanel;
    }
}
