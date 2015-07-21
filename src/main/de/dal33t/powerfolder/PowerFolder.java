/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import de.dal33t.powerfolder.util.BrowserLauncher;
import de.dal33t.powerfolder.util.JavaVersion;
import de.dal33t.powerfolder.util.MemoryMonitor;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.logging.LoggingManager;

/**
 * Main class for the PowerFolder application.
 * <p>
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.46 $
 */
public class PowerFolder {

    public static String NAME = "PowerFolder";
    private static final Logger log = Logger.getLogger(PowerFolder.class
        .getName());
    public static final Options COMMAND_LINE_OPTIONS;
    static {
        // Command line parsing
        Options options = new Options();
        Option configOption = OptionBuilder
            .withArgName("config")
            .withLongOpt("config")
            .hasOptionalArgs(2)
            .withDescription(
                "<config file>. Sets the configuration file to start. Default: PowerFolder.config")
            .create("c");

        options.addOption(configOption);
        options.addOption("u", "username", true,
            "<username>. The username to use when connecting.");
        options.addOption("p", "password", true,
            "<password>. The password to use when connecting.");

        options.addOption("m", "minimized", false,
            "Start PowerFolder minimized");
        options
            .addOption("s", "server", false,
                "Starts in console mode. Graphical user interface will be disabled");
        options
            .addOption("d", "dns", true,
                "<ip/dns>. Sets the dns/ip to listen to. May also be a dyndns address");
        options.addOption("h", "help", false, "Displays this help");
        options.addOption("n", "name", true,
            "<Name> Sets the name of this machine");
        options.addOption("b", "data", true,
            "Set the base data directory for PowerFolder");

        Option killOption = OptionBuilder.withArgName("kill")
            .withLongOpt("kill").hasOptionalArgs(1)
            .withDescription("Shuts down a running PowerFolder instance")
            .create("k");
        options.addOption(killOption);

        options
            .addOption(
                "l",
                "log",
                true,
                "<level> Sets console logging to severe, warning, info, fine or finer level (e.g. \"--log info\", sets info level and above");
        options
            .addOption(
                "f",
                "langfile",
                true,
                "<path\\file> Sets the language file to use (e.g. \"--langfile c:\\powerfolder\\translation\\translation_XX.properties\", forces PowerFolder to load this file as language file)");
        options.addOption("g", "language", true,
                "<language> Sets the language to use (e.g. \"--language de\", sets language to german)");
        options.addOption("e", "createfolder", true,
                "<createfolder> Creates a new folder");
        options.addOption("r", "removefolder", true,
                "<removefolder> Removes an existing folder");
        options.addOption("a", "copylink", true,
                "<copylink> Copies the PowerFolder link of that file");
        options.addOption("y", "notifyleft", false,
                "Show notification at left of screen");
        options.addOption("z", "nowarn", false,
                "Do not warn if already running");
        COMMAND_LINE_OPTIONS = options;
    }

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

        // Touch Logger immediately to initialize handlers.
        LoggingManager.isLogToFile();

        // Default exception logger
        Thread
            .setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                    log.log(Level.SEVERE,
                        "Exception in " + t + ": " + e.toString(), e);
                }
            });

        CommandLine commandLine = parseCommandLine(args);
        if (commandLine == null) {
            return;
        }

        // -l --log console log levels (severe, warning, info, fine and finer).
        if (commandLine.hasOption("l")) {
            String levelName = commandLine.getOptionValue("l");
            Level level = LoggingManager.levelForName(levelName);
            if (level != null) {
                LoggingManager.setConsoleLogging(level);
            }
        }

        if (commandLine.hasOption("s")) {
            // Server mode, suppress debug output on console
            // Logger.addExcludeConsoleLogLevel(Logger.DEBUG);
        }

        if (commandLine.hasOption("h")) {
            // Show help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PowerFolder", COMMAND_LINE_OPTIONS);
            return;
        }

        int rconPort = Integer.valueOf(ConfigurationEntry.NET_RCON_PORT
            .getDefaultValue());
        String portStr = commandLine.getOptionValue("k");
        if (StringUtils.isNotBlank(portStr)) {
            try {
                rconPort = Integer.valueOf(portStr.trim());
            } catch (Exception e) {
                log.warning("Unable to parse rcon port: " + portStr + ". " + e);
            }
        }

        boolean runningInstanceFound = RemoteCommandManager
            .hasRunningInstance(rconPort);

        if (commandLine.hasOption("k")) {
            if (runningInstanceFound) {
                System.out.println("Stopping " + NAME);
                // Send quit command
                RemoteCommandManager.sendCommand(rconPort,
                    RemoteCommandManager.QUIT);
            } else {
                System.err.println("Process not running");
            }

            // stop
            return;
        }

        // set language from commandline to preferences
        if (commandLine.hasOption("g")) {
            Preferences.userNodeForPackage(Translation.class).put("locale",
                commandLine.getOptionValue("g"));
        }

        JavaVersion jv = JavaVersion.systemVersion();

        if (jv.isOpenJDK()) {
            Object[] options = {"Open Oracle home page and exit", "Exit"};

            int n = JOptionPane.showOptionDialog(null,
                "You are using OpenJDK which is unsupported.\n" +
                "Please install the client with bundled JRE or install the Oracle JRE",
                "Unsupported JRE",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

            if (n == 0) {
                try {
                    BrowserLauncher.openURL("http://www.java.com");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

            return;
        }

        // Start: PFS-1721
        if (jv.getMinor() <= 7) {
            log.severe("You are trying to start using a JRE version lesser or equal to Java 7."
                + " Please update your JRE to version 8 or above to run.");
            return;
        }
        // End: PFS-1721

        // The controller.
        Controller controller = Controller.createController();

        String[] files = commandLine.getArgs();
        // Parsing of command line completed

        boolean commandContainsRemoteCommands = files != null
            && files.length >= 1 || commandLine.hasOption("e")
            || commandLine.hasOption("r") || commandLine.hasOption("a");
        // Try to start controller
        boolean startController = !commandContainsRemoteCommands
            || !runningInstanceFound;
        try {
            log.info("PowerFolder v" + Controller.PROGRAM_VERSION);

            // Start controller
            if (startController) {
                controller.startConfig(commandLine);
            }

            // Send remote command if there a files in commandline
            if (files != null && files.length > 0) {
                // Parse filenames and build remote command
                StringBuilder openFilesRCommand = new StringBuilder(
                    RemoteCommandManager.OPEN);

                for (String file : files) {
                    openFilesRCommand.append(file);
                    // FIXME: Add ; separator ?
                }

                // Send remote command to running PowerFolder instance
                RemoteCommandManager.sendCommand(openFilesRCommand.toString());
            }

            if (commandLine.hasOption("e")) {
                RemoteCommandManager
                    .sendCommand(RemoteCommandManager.MAKEFOLDER
                        + commandLine.getOptionValue("e"));
            }
            if (commandLine.hasOption("r")) {
                RemoteCommandManager
                    .sendCommand(RemoteCommandManager.REMOVEFOLDER
                        + commandLine.getOptionValue("r"));
            }
            if (commandLine.hasOption("a")) {
                RemoteCommandManager
                    .sendCommand(RemoteCommandManager.COPYLINK
                        + commandLine.getOptionValue("a"));
            }
        } catch (Throwable t) {
            t.printStackTrace();
            log.log(Level.SEVERE, "Throwable", t);
            return;
        }

        // Begin monitoring memory usage.
        if (controller.isStarted() && controller.isUIEnabled()) {
            ScheduledExecutorService service = controller.getThreadPool();
            service.scheduleAtFixedRate(new MemoryMonitor(controller), 1, 1,
                TimeUnit.MINUTES);
        }

        // Not go into console mode if ui is open
        if (!startController) {
            if (runningInstanceFound) {
                RemoteCommandManager.sendCommand(RemoteCommandManager.SHOW_UI);
            }
            return;
        }

        System.out.println("------------ " + NAME +" "
            + Controller.PROGRAM_VERSION + " started ------------");

        boolean restartRequested = false;
        do {
            // Go into restart loop
            while (controller.isStarted() || controller.isShuttingDown()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.log(Level.WARNING, "InterruptedException", e);
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
                log.info("Restarting controller");
                System.out.println("------------ " + NAME + " "
                    + Controller.PROGRAM_VERSION + " restarting ------------");
                controller = null;
                System.gc();
                controller = Controller.createController();
                // Start controller
                controller.startConfig(commandLine);
            }
        } while (restartRequested);
    }

    public static CommandLine parseCommandLine(String[] args) {
        CommandLineParser parser = new PosixParser();
        try {
            // parse the command line arguments
            return parser.parse(COMMAND_LINE_OPTIONS, args);
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Start failed. Reason: " + exp.getMessage());
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("PowerFolder", COMMAND_LINE_OPTIONS);
        }
        return null;
    }
}