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
 * $Id: UploadsInformationCard.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.stats;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.transfer.BandwidthLimiterInfo;
import de.dal33t.powerfolder.transfer.CoalescedBandwidthStat;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.InformationCard;
import de.dal33t.powerfolder.util.Translation;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Hour;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

/**
 * Information card for a folder. Includes files, members and settings tabs.
 */
public class StatsInformationCard extends InformationCard {

    private JPanel uiComponent;
    private JComboBox dataTypeComboBox;
    private DefaultComboBoxModel dataTypeModel;
    private JComboBox graphTypeComboBox;
    private DefaultComboBoxModel graphTypeModel;

    private TimeSeries availableBandwidthSeries;
    private TimeSeries usedBandwidthSeries;
    private TimeSeries averageBandwidthSeries;

    /**
     * Constructor
     *
     * @param controller
     */
    public StatsInformationCard(Controller controller) {
        super(controller);
    }

    /**
     * Gets the image for the card.
     *
     * @return
     */
    public Image getCardImage() {
        return Icons.getImageById(Icons.STATS);
    }

    /**
     * Gets the title for the card.
     *
     * @return
     */
    public String getCardTitle() {
        return Translation.getTranslation("stats_information_card.title");
    }

    /**
     * Gets the ui component after initializing and building if necessary
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
            redrawBandwidthStats();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        dataTypeModel = new DefaultComboBoxModel();
        dataTypeModel.addElement(Translation.getTranslation("stats_information_card.wan_upload"));
        dataTypeModel.addElement(Translation.getTranslation("stats_information_card.wan_download"));
        dataTypeModel.addElement(Translation.getTranslation("stats_information_card.lan_upload"));
        dataTypeModel.addElement(Translation.getTranslation("stats_information_card.lan_download"));

        graphTypeModel = new DefaultComboBoxModel();
        graphTypeModel.addElement(Translation.getTranslation("stats_information_card.used_and_available"));
        graphTypeModel.addElement(Translation.getTranslation("stats_information_card.used_only"));
        graphTypeModel.addElement(Translation.getTranslation("stats_information_card.available_only"));
        graphTypeModel.addElement(Translation.getTranslation("stats_information_card.average"));

        usedBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.used"), Hour.class);
        availableBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.available"), Hour.class);
        averageBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.average"), Hour.class);
    }

    /**
     * Build the ui component pane.
     */
    private void buildUIComponent() {

        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
                "3dlu, fill:pref:grow, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();
        JTabbedPane tabbedPane = new JTabbedPane();

        builder.add(tabbedPane, cc.xy(2, 2));

        JPanel usedPanel = getUsedPanel();
        tabbedPane.add(usedPanel, Translation.getTranslation(
                "stats_information_card.used_graph.text"));
        tabbedPane.setToolTipTextAt(0, Translation.getTranslation(
                "stats_information_card.used_graph.tip"));

        uiComponent = builder.getPanel();
    }

    private JPanel getUsedPanel() {

        DateAxis domain = new DateAxis(Translation.getTranslation("stats_information_card.date"));
        TimeSeriesCollection series = new TimeSeriesCollection();
        NumberAxis axis = new NumberAxis(Translation.getTranslation("stats_information_card.bandwidth"));

        series.addSeries(availableBandwidthSeries);
        series.addSeries(usedBandwidthSeries);
        series.addSeries(averageBandwidthSeries);

        XYItemRenderer renderer = new StandardXYItemRenderer();
        XYPlot plot = new XYPlot(series, domain, axis, renderer);
        JFreeChart graph = new JFreeChart(plot);
        ChartPanel cp = new ChartPanel(graph);

        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
                "3dlu, pref , 3dlu, pref, 3dlu, fill:pref:grow, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel p = buildStatsControlPanel();

        builder.add(p, cc.xy(2,2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(cp, cc.xy(2,6));
        return builder.getPanel();
    }

    private JPanel buildStatsControlPanel() {
        FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu",
                "3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        dataTypeComboBox = new JComboBox(dataTypeModel);
        builder.add(dataTypeComboBox, cc.xy(2, 2));
        MyActionListener actionListener = new MyActionListener();
        dataTypeComboBox.addActionListener(actionListener);

        graphTypeComboBox = new JComboBox(graphTypeModel);
        builder.add(graphTypeComboBox, cc.xy(4, 2));
        graphTypeComboBox.addActionListener(actionListener);

        return builder.getPanel();
    }

    private void redrawBandwidthStats() {

        Cursor c = uiComponent.getCursor();
        try {
            uiComponent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            int graphType = graphTypeComboBox.getSelectedIndex();
            int dataType = dataTypeComboBox.getSelectedIndex();

            availableBandwidthSeries.clear();
            usedBandwidthSeries.clear();
            averageBandwidthSeries.clear();

            Set<CoalescedBandwidthStat> stats = getController().getTransferManager().getBandwidthStats();
            Calendar cal = Calendar.getInstance();
            boolean first = true;
            Date last = null;
            for (CoalescedBandwidthStat stat : stats) {
                if (stat.getInfo() == BandwidthLimiterInfo.WAN_INPUT && dataType == 0 ||
                        stat.getInfo() == BandwidthLimiterInfo.WAN_OUTPUT && dataType == 1 ||
                        stat.getInfo() == BandwidthLimiterInfo.LAN_INPUT && dataType == 2 ||
                        stat.getInfo() == BandwidthLimiterInfo.LAN_OUTPUT && dataType == 3) {
                    if (first) {
                        first = false;
                        cal.setTime(stat.getDate());
                    }
                    Hour hour = new Hour(stat.getDate());
                    if (graphType == 0 || graphType == 1) {
                        usedBandwidthSeries.add(hour, stat.getUsedBandwidth() / 1000.0);
                    }
                    if (graphType == 0 || graphType == 2) {
                        availableBandwidthSeries.add(hour, stat.getInitialBandwidth() / 1000.0);
                    }
                    if (graphType == 3) {
                        averageBandwidthSeries.add(hour, stat.getAverageUsedBandwidth() / 1000.0);
                    }
                    last = stat.getDate();
                }
            }
            if (last != null) {
                while (cal.getTime().before(last)) {
                    Hour hour = new Hour(cal.getTime());
                    if (availableBandwidthSeries.getDataItem(hour) == null) {
                        availableBandwidthSeries.add(hour, 0.0);
                    }
                    if (usedBandwidthSeries.getDataItem(hour) == null) {
                        usedBandwidthSeries.add(hour, 0.0);
                    }
                    if (averageBandwidthSeries.getDataItem(hour) == null) {
                        averageBandwidthSeries.add(hour, 0.0);
                    }
                    cal.add(Calendar.HOUR, 1);
                }
            }
        } finally {
            uiComponent.setCursor(c);
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == dataTypeComboBox || e.getSource() == graphTypeComboBox) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        redrawBandwidthStats();
                    }
                });
            }
        }
    }

}