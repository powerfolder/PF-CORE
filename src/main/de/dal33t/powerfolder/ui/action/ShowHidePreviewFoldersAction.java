package de.dal33t.powerfolder.ui.action;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.jgoodies.binding.value.ValueModel;

import javax.swing.*;

/**
 * Action for toggeling whether preview folders should display.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 3.1 $
 */
@SuppressWarnings("serial")
public class ShowHidePreviewFoldersAction extends BaseAction {

    private ValueModel hidePreviewFoldersVM;

    public ShowHidePreviewFoldersAction(ValueModel hidePreviewFoldersVM,
        Controller controller) {
        super("hide_preview", controller);
        if (hidePreviewFoldersVM == null) {
            throw new NullPointerException("hidePreviewFoldersVM is null");
        }
        this.hidePreviewFoldersVM = hidePreviewFoldersVM;
        hidePreviewFoldersVM.addValueChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                changeTitle((Boolean) evt.getNewValue());
            }
        });
    }

    private void changeTitle(Boolean hide) {
        if (hide) {
            putValue(Action.NAME, Translation.getTranslation("show_preview.name"));
            setMnemonicKey(Translation.getTranslation("show_preview.key"));
            putValue(Action.SHORT_DESCRIPTION, Translation.getTranslation("show_preview.description"));
        } else {
            putValue(Action.NAME, Translation.getTranslation("hide_preview.name"));
            setMnemonicKey(Translation.getTranslation("hide_preview.key"));
            putValue(Action.SHORT_DESCRIPTION, Translation.getTranslation("hide_preview.description"));
        }
    }

    public void actionPerformed(ActionEvent e) {
        // Toggle visibility
        hidePreviewFoldersVM.setValue(!((Boolean) hidePreviewFoldersVM.getValue()));
    }
}
