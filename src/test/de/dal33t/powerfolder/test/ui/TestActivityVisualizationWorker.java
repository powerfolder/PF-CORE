package de.dal33t.powerfolder.test.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;

/**
 * Example program which shows how to use the
 * <code>ActivityVisualizationWorker</code>
 * 
 * @see de.dal33t.powerfolder.util.ui.ActivityVisualizationWorker
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class TestActivityVisualizationWorker {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            // Set l&f
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        final JFrame frame = new JFrame();
        frame.setIconImage(Icons.POWERFOLDER_IMAGE);
        frame.setPreferredSize(new Dimension(200, 100));
        frame.setLocation(500, 500);

        JButton startButton = new JButton("Start working");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MyWorker worker = new MyWorker(frame);
                worker.start();
            }
        });

        frame.getContentPane().add(startButton);
        frame.pack();
        frame.setVisible(true);
    }

    private static class MyWorker extends ActivityVisualizationWorker {
        public MyWorker(JFrame frame) {
            super(frame);
        }

        @Override
        protected String getTitle()
        {
            return "Heavily working";
        }

        @Override
        protected String getWorkingText()
        {
            return "Yes, I am busy man!";
        }

        @Override
        public Object construct()
        {
            try {
                System.out.println("We are doing...");
                Thread.sleep(1000);
                System.out.println("...now some really...");
                Thread.sleep(1000);
                System.out.println("...heavy stuff...");
                Thread.sleep(1000);
                System.out.println("...that just...");
                Thread.sleep(1000);
                System.out.println("...takes ages...");
                Thread.sleep(1000);
                System.out.println("...to complete...");
                Thread.sleep(1000);
                System.out.println("...");
                Thread.sleep(1000);
                System.out.println("...");
                Thread.sleep(1000);
                System.out.println("...finally done. ;)");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
