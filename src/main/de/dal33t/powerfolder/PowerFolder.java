/* $Id: PowerFolder.java,v 1.46 2006/04/23 18:21:17 bytekeeper Exp $
 */
package de.dal33t.powerfolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.dal33t.powerfolder.net.ConnectionException;
import de.dal33t.powerfolder.util.Loggable;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Main class for the powerfolder application.
 * <p>
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.46 $
 */
public class PowerFolder extends Loggable {
    private static Logger LOG = Logger.getLogger(PowerFolder.class);

    public static void main(String[] args) {
        // Start PF
        startPowerFolder(args);
    }

    /**
     * Starts a PowerFolder controller with the given command line arguments
     * 
     * @param args
     */
    public static void startPowerFolder(String[] args) {
        // Default exception logger
        Thread
            .setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                public void uncaughtException(Thread t, Throwable e) {
                    LOG.error("Exception in " + t + ": " + e.toString(), e);
                }
            });
        // Command line parsing
        Options options = new Options();
        options
            .addOption(
                "c",
                "config",
                true,
                "<config file>. Sets the configuration file to start. Default: PowerFolder.config");
        options.addOption("t", "tester", false,
            "Enables tester mode. Will check for new development version");
        options.addOption("m", "minimized", false,
            "Start PowerFolder minimized");
        options.addOption("s", "server", false,
            "Start PowerFolder as server/supernode. GUI will be disabled");
        options
            .addOption("d", "dns", true,
                "<ip/dns>. Sets the dns/ip to listen to. May also be an dyndns address");
        options.addOption("h", "help", false, "Displays this help");
        options.addOption("n", "nick", true, "<nickname> Sets the nickname");
        options.addOption("k", "kill", false,
            "Shutsdown a running PowerFolder instance");
        options.addOption("l", "log", false,
            "Displays logging information on console");
        options
            .addOption(
                "f",
                "langfile",
                true,
                "<path\\file> Sets the language file to use (e.g. \"--langfile c:\\powerfolder\\translation\\translation_XX.properties\", forces PowerFolder to load this file as language file)");
        options
            .addOption(
                "g",
                "language",
                true,
                "<language> Sets the language to use (e.g. \"--language de\", sets language to german)");
        options.addOption("p", "createfolder", true, 
        		"<createfolder> Creates a new PowerFolder");
        
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;
        try {
            // parse the command line arguments
            commandLine = parser.parse(options, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Start failed. Reason: " + exp.getMessage());
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PowerFolder", options);
            return;
        }

        // Enable console logging if wanted (--log)
        Logger.setEnabledConsoleLogging(commandLine.hasOption("l"));

        if (commandLine.hasOption("s")) {
            // Server mode, supress debug output on console
            Logger.setExcludeConsoleLogLevel(Logger.DEBUG);
        }

        if (commandLine.hasOption("h")) {
            // Show help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PowerFolder", options);
            return;
        }

        if (commandLine.hasOption("k")) {
            if (!RemoteCommandManager.hasRunningInstance()) {
                System.err.println("PowerFolder not running");
            } else {
                System.out.println("Stopping PowerFolder");
                // Send quit command
                RemoteCommandManager.sendCommand(RemoteCommandManager.QUIT);
            }

            // stop
            return;
        }

        // set language from commandline to preferences
        if (commandLine.hasOption("g")) {
            Preferences.userNodeForPackage(Translation.class).put("locale",
                commandLine.getOptionValue("g"));
        }

        // The controller
        // Replace this line, when all calls to getInstance are removed
        Controller controller = Controller.createController();

        String[] files = commandLine.getArgs();
        // Parsing of command line completed

        // Start controller if no running instance
        boolean startController = !RemoteCommandManager.hasRunningInstance();
        try {
            LOG.info("PowerFolder v" + Controller.PROGRAM_VERSION);

            // Start controller
            if (startController) {
                controller.startConfig(commandLine);
            }

            // Send remote command if there a files in commandline
            if (files.length > 0) {
                // Parse filenames and build remote command
                StringBuffer openFilesRCommand = new StringBuffer(
                    RemoteCommandManager.OPEN);

                for (int i = 0; i < files.length; i++) {
                    openFilesRCommand.append(files[i]);
                    // FIXME: Add ; separator ?
                }

                // Send remote command to running PowerFolder instance
                RemoteCommandManager.sendCommand(openFilesRCommand.toString());
            }
            
            if (commandLine.hasOption("p")) {
            	RemoteCommandManager.sendCommand(RemoteCommandManager.MAKEFOLDER 
            			+ commandLine.getOptionValue("p"));
            }
        } catch (Throwable t) {
            LOG.error(t);
            return;
        }

        // Started
        if (startController) {
            System.out.println("------------ PowerFolder "
                + Controller.PROGRAM_VERSION + " started ------------");
        }

        // Not go into console mode if ui is open
        if (!startController) {
            return;
        }

        if (controller.isUIEnabled()) {
            boolean restartRequested = false;
            do {
                // Go into restart loop
                while (controller.isStarted() || controller.isShuttingDown()) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        LOG.warn(e);
                        return;
                    }
                }

                restartRequested = controller.isRestartRequested();
                if (restartRequested) {

                    Map<Thread, StackTraceElement[]> threads = Thread
                        .getAllStackTraces();
                    for (Thread thread : threads.keySet()) {
                        if (thread.getName().startsWith("PoolThread")
                            || thread.getName().startsWith("Reconnector")
                            || thread.getName().startsWith("ConHandler"))
                        {
                            thread.interrupt();
                        }
                    }
                    LOG.info("Restarting controller");
                    System.out
                        .println("------------ PowerFolder "
                            + Controller.PROGRAM_VERSION
                            + " restarting ------------");
                    controller = null;
                    System.gc();
                    controller = Controller.createController();
                    // Start controller
                    controller.startConfig(commandLine);
                }
            } while (restartRequested);

            // Exit
            return;
        }

        // Console mode comes here ...

        // Add shutdown hook
        LOG.verbose("Adding shutdown hook");
        final Controller con = controller;
        Runtime.getRuntime().addShutdownHook(
            new Thread("Shutdown hook for PowerFolder") {
                public void run() {
                    // Shut down controller on VM exit
                    con.shutdown();
                }
            });

        if (controller == null) {
            // Stop
            return;
        }

        // Console loop
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (controller.isStarted()) {
            try {
                String line = in.readLine();
                if (line == null) {
                    line = "";
                }
                line = line.toLowerCase();

                if (line.startsWith("x")) {
                    // x pressed, exit
                    controller.exit(0);
                } else if (line.startsWith("connect ")) {
                    String conStr = line.substring(8);
                    try {
                        controller.connect(conStr);
                    } catch (ConnectionException e) {
                        LOG.error("Unable to connect to " + conStr);
                    }
                } else if (line.startsWith("c ")) {
                    String conStr = line.substring(2);
                    try {
                        controller.connect(conStr);
                    } catch (ConnectionException e) {
                        LOG.error("Unable to connect to " + conStr);
                    }
                } else if (line.startsWith("ul ")) {
                    String ulimit = line.substring(3);
                    try {
                        controller.getTransferManager().setAllowedUploadCPSForWAN(
                            (long) Double.parseDouble(ulimit) * 1024);
                    } catch (NumberFormatException e) {
                        LOG.error("Unable to parse new upload limit bandwidth "
                            + ulimit);
                    }
                } else if (line.startsWith("r")) {
                    // write report
                    controller.writeDebugReport();
                    System.out.println(controller.getDebugReport());
                }

                // Sleep a bit, if commands are comming in too fast
                // on linux background processing
                Thread.sleep(200);
            } catch (IOException e) {
                LOG.error(e);
                break;
            } catch (InterruptedException e) {
                LOG.error(e);
                break;
            }
        }
    }
}