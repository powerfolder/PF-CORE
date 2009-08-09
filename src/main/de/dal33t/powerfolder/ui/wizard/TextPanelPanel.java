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
 * $Id$
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.ui.UIUtil;
import jwf.WizardPanel;

import javax.swing.*;

import java.util.List;
import java.util.StringTokenizer;

/**
 * A general text panel, displays the given text and offers to finish wizard
 * process. No next panel
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public class TextPanelPanel extends PFWizardPanel {

    private boolean autoFadeOut;
    private String title;
    private String text;

    public TextPanelPanel(Controller controller, String title, String text) {
        this(controller, title, text, false);
    }

    public TextPanelPanel(Controller controller, String title, String text,
        boolean autoFadeOut)
    {
        super(controller);
        this.title = title;
        this.text = text;
        this.autoFadeOut = autoFadeOut;
    }

    public boolean hasNext() {
        return false;
    }

    @Override
    protected void afterDisplay() {
        if (autoFadeOut) {
            new FadeOutWorker().execute();
        }
    }

    public WizardPanel next() {
        return null;
    }

    public boolean canFinish() {
        return true;
    }

    public boolean canCancel() {
        return false;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout("pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Add text as labels
        StringTokenizer nizer = new StringTokenizer(text, "\n");
        int y = 1;
        while (nizer.hasMoreTokens()) {
            String line = nizer.nextToken();
            builder.appendRow("pref");
            builder.addLabel(line, cc.xy(1, y));
            y++;
        }
        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
    }

    protected JComponent getPictoComponent() {
        return new JLabel(getContextPicto());
    }

    protected String getTitle() {
        return title;
    }

    private class FadeOutWorker extends SwingWorker<Void, Integer> {

        @Override
        protected void process(List<Integer> chunks) {
            if (!getWizardDialog().isVisible()) {
                return;
            }
            // Translucency is 1 - opacity.
            float opacity = 1.0f - chunks.get(0) / 100.0f;
            UIUtil.applyTranslucency(getWizardDialog(), opacity);
        }

        @Override
        protected Void doInBackground() throws Exception {
            Thread.sleep(1000);
            if (!Constants.OPACITY_SUPPORTED) {
                Thread.sleep(1000);
                return null;
            }
            for (int i = 0; i < 100; i++) {
                publish(i);
                Thread.sleep(10);
            }
            publish(100);
            return null;
        }

        @Override
        protected void done() {
            JDialog diag = getWizardDialog();
            diag.setVisible(false);
            diag.dispose();
        }

        private JDialog getWizardDialog() {
            JDialog diag = (JDialog) getWizardContext().getAttribute(
                WizardContextAttributes.DIALOG_ATTRIBUTE);
            return diag;
        }

    }
}