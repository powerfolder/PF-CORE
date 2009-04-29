This is a demonstration of a custom Synthetica Look And Feel (LAF).
The only difference between it and the standard Synthetica LAF is that it has
green text (see the synth.xml SyntheticaDefaultFont).
To compile, use the 'demo' buld target, then include the CustomLookAndFeel.jar
in the class path, and include a '-y' parameter indicating the path to the
custom LAF class, e.g.

 java -cp ./PowerFolder.jar;./CustomLookAndFeel.jar de.dal33t.powerfolder.PowerFolder -y demo.CustomLookAndFeel

The LAF will then appear in Preferences / User Interface / Color theme combo,
and will function as a normal Synthetica LAF.

