package de.dal33t.powerfolder.ui.webservice;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.ui.widget.FolderListPanel;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;

public class OnlineStoragePanel extends PFUIPanel {
    private OnlineStorageClientModel model;
    private JComponent panel;

    private QuickInfoPanel quickInfo;
    private JComponent toolbar;

    private Component foldersListPanel;
    private SelectionInList foldersListModel;

    public OnlineStoragePanel(Controller controller,
        OnlineStorageClientModel model)
    {
        super(controller);
        Reject.ifNull(model, "model is null");
        this.model = model;
    }

    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(createContentPanel());
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * TODO #495
     * 
     * @return the title
     */
    public String getTitle() {
        return Translation.getTranslation("general.webservice");
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow", "fill:pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(foldersListPanel, cc.xy(1, 1));
        return builder.getPanel();
    }

    private void initComponents() {
        quickInfo = new OnlineStorageQuickInfoPanel(getController());

        // Create toolbar
        toolbar = createToolBar();

        foldersListModel = new SelectionInList(getUIController()
            .getOnlineStorageClientModel().getMirroredFoldersModel());
        foldersListPanel = new FolderListPanel(foldersListModel)
            .getUIComponent();
    }

    /**
     * @return the toolbar
     */
    private JComponent createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JButton(new OpenWebServiceInBrowserAction(
            getController())));
        bar.addRelatedGap();
        JButton mirrorButton = new JButton(model.getMirrorFolderAction());
        bar.addGridded(mirrorButton);
        mirrorButton.setEnabled(!getController().isLanOnly());
        bar.addRelatedGap();
        bar.addGridded(new JButton(new SyncFolderRightsAction(getController()
            .getOSClient())));
        bar.addUnrelatedGap();
        bar.addGridded(new JButton(new AboutWebServiceAction(getController())));

        // bar.addRelatedGap();
        // bar.addGridded(new JButton(clearCompletedAction));

        JPanel barPanel = bar.getPanel();
        barPanel.setBorder(Borders.DLU4_BORDER);

        return barPanel;
    }
}
