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
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.CursorUtils;
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
    private JComboBox usedDataTypeComboBox;
    private DefaultComboBoxModel usedDataTypeModel;
    private JComboBox usedGraphTypeComboBox;
    private DefaultComboBoxModel usedGraphTypeModel;

    private DefaultComboBoxModel percentDataTypeModel;

    private TimeSeries availableBandwidthSeries;
    private TimeSeries usedBandwidthSeries;
    private TimeSeries averageBandwidthSeries;

    private TimeSeries percentageBandwidthSeries;
    private JComboBox percentDataTypeComboBox;

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
            redrawUsedBandwidthStats();
            redrawPercentageBandwidthStats();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        usedDataTypeModel = new DefaultComboBoxModel();
        usedDataTypeModel.addElement(Translation.getTranslation("stats_information_card.wan_upload"));
        usedDataTypeModel.addElement(Translation.getTranslation("stats_information_card.wan_download"));
        usedDataTypeModel.addElement(Translation.getTranslation("stats_information_card.lan_upload"));
        usedDataTypeModel.addElement(Translation.getTranslation("stats_information_card.lan_download"));

        usedGraphTypeModel = new DefaultComboBoxModel();
        usedGraphTypeModel.addElement(Translation.getTranslation("stats_information_card.used_and_available"));
        usedGraphTypeModel.addElement(Translation.getTranslation("stats_information_card.used_only"));
        usedGraphTypeModel.addElement(Translation.getTranslation("stats_information_card.available_only"));
        usedGraphTypeModel.addElement(Translation.getTranslation("stats_information_card.average"));

        percentDataTypeModel = new DefaultComboBoxModel();
        percentDataTypeModel.addElement(Translation.getTranslation("stats_information_card.wan_upload"));
        percentDataTypeModel.addElement(Translation.getTranslation("stats_information_card.wan_download"));
        percentDataTypeModel.addElement(Translation.getTranslation("stats_information_card.lan_upload"));
        percentDataTypeModel.addElement(Translation.getTranslation("stats_information_card.lan_download"));

        usedBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.used"), Hour.class);
        availableBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.available"), Hour.class);
        averageBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.average"), Hour.class);

        percentageBandwidthSeries = new TimeSeries(Translation.getTranslation("stats_information_card.percentage"), Hour.class);

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

        JPanel averagePanel = getAveragePanel();
        tabbedPane.add(averagePanel, Translation.getTranslation(
                "stats_information_card.percentage_graph.text"));
        tabbedPane.setToolTipTextAt(1, Translation.getTranslation(
                "stats_information_card.percentage_graph.tip"));

        uiComponent = builder.getPanel();
    }

    private JPanel getAveragePanel() {
        DateAxis domain = new DateAxis(Translation.getTranslation("stats_information_card.date"));
        TimeSeriesCollection series = new TimeSeriesCollection();
        NumberAxis axis = new NumberAxis(Translation.getTranslation("stats_information_card.percentage"));

        series.addSeries(percentageBandwidthSeries);

        XYItemRenderer renderer = new StandardXYItemRenderer();
        XYPlot plot = new XYPlot(series, domain, axis, renderer);
        JFreeChart graph = new JFreeChart(plot);
        ChartPanel cp = new ChartPanel(graph);

        FormLayout layout = new FormLayout("3dlu, fill:pref:grow, 3dlu",
                "3dlu, pref , 3dlu, pref, 3dlu, fill:pref:grow, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JPanel p = buildPercentStatsControlPanel();

        builder.add(p, cc.xy(2,2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(cp, cc.xy(2,6));
        return builder.getPanel();
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

        JPanel p = buildUsedStatsControlPanel();

        builder.add(p, cc.xy(2,2));
        builder.addSeparator(null, cc.xyw(1, 4, 3));
        builder.add(cp, cc.xy(2,6));
        return builder.getPanel();
    }

    private JPanel buildUsedStatsControlPanel() {
        FormLayout layout = new FormLayout("3dlu, pref, 3dlu, pref, 3dlu",
                "3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        usedDataTypeComboBox = new JComboBox(usedDataTypeModel);
        builder.add(usedDataTypeComboBox, cc.xy(2, 2));
        MyActionListener actionListener = new MyActionListener();
        usedDataTypeComboBox.addActionListener(actionListener);

        usedGraphTypeComboBox = new JComboBox(usedGraphTypeModel);
        builder.add(usedGraphTypeComboBox, cc.xy(4, 2));
        usedGraphTypeComboBox.addActionListener(actionListener);

        return builder.getPanel();
    }

    private JPanel buildPercentStatsControlPanel() {
        FormLayout layout = new FormLayout("3dlu, pref, 3dlu",
                "3dlu, pref, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        MyActionListener actionListener = new MyActionListener();
        percentDataTypeComboBox = new JComboBox(percentDataTypeModel);
        builder.add(percentDataTypeComboBox, cc.xy(2, 2));
        percentDataTypeComboBox.addActionListener(actionListener);

        return builder.getPanel();
    }

    private void redrawUsedBandwidthStats() {

        Cursor c = CursorUtils.setWaitCursor(uiComponent);
        try {

            int usedGraphType = usedGraphTypeComboBox.getSelectedIndex();
            int usedDataType = usedDataTypeComboBox.getSelectedIndex();

            availableBandwidthSeries.clear();
            usedBandwidthSeries.clear();
            averageBandwidthSeries.clear();

            Set<CoalescedBandwidthStat> stats = getController().getTransferManager().getBandwidthStats();
            Calendar cal = Calendar.getInstance();
            boolean first = true;
            Date last = null;
            for (CoalescedBandwidthStat stat : stats) {
                Hour hour = new Hour(stat.getDate());
                if (first) {
                    first = false;
                    cal.setTime(stat.getDate());
                }
                if (stat.getInfo() == BandwidthLimiterInfo.WAN_INPUT && usedDataType == 0 ||
                        stat.getInfo() == BandwidthLimiterInfo.WAN_OUTPUT && usedDataType == 1 ||
                        stat.getInfo() == BandwidthLimiterInfo.LAN_INPUT && usedDataType == 2 ||
                        stat.getInfo() == BandwidthLimiterInfo.LAN_OUTPUT && usedDataType == 3) {
                    if (usedGraphType == 0 || usedGraphType == 1) {
                        usedBandwidthSeries.add(hour, stat.getUsedBandwidth() / 1000.0);
                    }
                    if (usedGraphType == 0 || usedGraphType == 2) {
                        availableBandwidthSeries.add(hour, stat.getInitialBandwidth() / 1000.0);
                    }
                    if (usedGraphType == 3) {
                        averageBandwidthSeries.add(hour, stat.getAverageUsedBandwidth() / 1000.0);
                    }
                }
                last = stat.getDate();
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
            CursorUtils.returnToOriginal(uiComponent, c);
        }
    }

    private void redrawPercentageBandwidthStats() {

        Cursor c = CursorUtils.setWaitCursor(uiComponent);
        try {

            int percentDataType = percentDataTypeComboBox.getSelectedIndex();

            percentageBandwidthSeries.clear();

            Set<CoalescedBandwidthStat> stats = getController().getTransferManager().getBandwidthStats();
            Calendar cal = Calendar.getInstance();
            boolean first = true;
            Date last = null;
            for (CoalescedBandwidthStat stat : stats) {
                Hour hour = new Hour(stat.getDate());
                if (first) {
                    first = false;
                    cal.setTime(stat.getDate());
                }
                if (stat.getInfo() == BandwidthLimiterInfo.WAN_INPUT && percentDataType == 0 ||
                        stat.getInfo() == BandwidthLimiterInfo.WAN_OUTPUT && percentDataType == 1 ||
                        stat.getInfo() == BandwidthLimiterInfo.LAN_INPUT && percentDataType == 2 ||
                        stat.getInfo() == BandwidthLimiterInfo.LAN_OUTPUT && percentDataType == 3) {
                    percentageBandwidthSeries.add(hour, stat.getPercentageUsedBandwidth());
                }
                last = stat.getDate();
            }
            if (last != null) {
                while (cal.getTime().before(last)) {
                    Hour hour = new Hour(cal.getTime());
                    if (percentageBandwidthSeries.getDataItem(hour) == null) {
                        percentageBandwidthSeries.add(hour, 0.0);
                    }
                    cal.add(Calendar.HOUR, 1);
                }
            }
        } finally {
            CursorUtils.returnToOriginal(uiComponent, c);
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == usedDataTypeComboBox || e.getSource() == usedGraphTypeComboBox) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        redrawUsedBandwidthStats();
                    }
                });
            } else if (e.getSource() == percentDataTypeComboBox) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        redrawPercentageBandwidthStats();
                    }
                });
            }
        }
    }

}