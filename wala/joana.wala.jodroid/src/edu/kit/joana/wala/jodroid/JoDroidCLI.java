/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package edu.kit.joana.wala.jodroid;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.ibm.wala.dalvik.util.AndroidEntryPointManager;


/**
 *  CommandLine Interface to JoDroid.
 *
 *  Run this class from the commandLine to start JoDroid.
 *
 *  Thich class handles all options, then calls JoDroidConstruction which contains 
 *  the execution-code.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 */
@SuppressWarnings("deprecation")
public class JoDroidCLI {
	private static final String JODROID_INVOCATION_NAME = "joana.wala.jodroid.jar";
	private static final String PARAMETERS = "<classpath>";
    private static final int LINE_WIDTH = 120;

    private static void printUsage(Options options) {
        PrintWriter pw = new PrintWriter(System.out, true);
        HelpFormatter formatter = new HelpFormatter();

        formatter.printUsage(pw, LINE_WIDTH, "[OPTIONS] " + JODROID_INVOCATION_NAME + " " + PARAMETERS + "\n");
        formatter.printWrapped(pw, LINE_WIDTH, /* nextLineTabStop = */ 4, "<classpath>:    specifies .apk file to analyze.");
        formatter.printWrapped(pw, LINE_WIDTH, /* nextLineTabStop = */ 0, "\nPossible Options are:");
        formatter.printOptions(pw, LINE_WIDTH, options, 4, 8);
	}

    /**
     *  Set, read and verify command-line options.
     */
    private static ExecutionOptions evaluateOptions(String[] args) {
        CommandLine commandLine;
        final Options options = new Options();
        @SuppressWarnings("deprecation")
		final CommandLineParser parser = new GnuParser();
        final ExecutionOptions p = new ExecutionOptions();

        //
        // Set available options
        //
        options.addOption( "h", "help", false, "show help message" );
        options.addOption( "v", "verbose", false, AnalysisPresets.OutputDescription.VERBOSE.description );
        options.addOption( "q", "quiet", false, AnalysisPresets.OutputDescription.QUIET.description );
        options.addOption( "d", "debug", false, AnalysisPresets.OutputDescription.DEBUG.description );
        OptionBuilder.withLongOpt( "outfile" );
		OptionBuilder
		.withDescription( "specifies the path to the file in which the resulting SDG is to be written. " +
		    "It defaults to <classpath>.pdg");
		OptionBuilder
		.hasArg() //.isRequired() 
;
		OptionBuilder
		.withArgName("FILE");
		options.addOption(  //"o", "outfile", 
                OptionBuilder
                .create("o") );
        OptionBuilder.withLongOpt( "entrypoint" );
		OptionBuilder
		.withDescription( 
		         "specifies the method at which the environment " +
		        "enters the app under analysis. Note that the name of the method must be fully qualified " +
		        "and its signature has to be given in bytecode notation. (Or @all)\n" + 
		        "Examples:\n" +
		        "    com.foo.bar.AClass.main([Ljava/lang/String;)V\n" +
		        "    com.foo.bar.AClass.addTwoInts(II)I\n" +
		        "    com.foo.bar.AClass$AnInnerClass.isTroodles()Z\n"
		    );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("SIG");
		options.addOption(  //"e", "entrypoint", 
                OptionBuilder
                .create("e") );
        OptionBuilder.withLongOpt( "analysis");
		OptionBuilder
		.withDescription(
		    "set the accuracy of the analysis. Possible values are:\n" +
		    AnalysisPresets.PresetDescription.dumpOptions()
		    );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("PRESET");
		options.addOption(  //"a", "analysis",
                OptionBuilder
                .create("a") );
        OptionBuilder.withLongOpt( "scan" );
		OptionBuilder
		.withDescription(
		    "scan for possible entry points. Possible values are:\n" +
		    ExecutionOptions.ScanMode.dumpOptions()
		    );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("SETTING");
		options.addOption(  //"s", "scan",
                OptionBuilder
                .create("s") );
        OptionBuilder.withLongOpt( "construct" );
		OptionBuilder
		.withDescription(
		    "construct the SDG. Possible values are:\n" +
		    ExecutionOptions.BuildMode.dumpOptions()
		    );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("SETTING");
		options.addOption(  //"c", "construct",
                OptionBuilder
                .create("c") );
        OptionBuilder.withLongOpt( "ep-file" );
		OptionBuilder
		.withDescription( "load entry points for file (generate with the '--scan'-option)" );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("FILE");
		options.addOption(  //"f", "ep-file"
                OptionBuilder
                .create("f") );
        OptionBuilder.withLongOpt( "lib" );
		OptionBuilder
		.withDescription( "specifies the path to the .jar or .dex which contains the android library." );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("FILE");
		options.addOption(  //"l", "lib", 
                OptionBuilder
                .create("l") );
        OptionBuilder.withLongOpt( "lib-java" );
		OptionBuilder
		.withDescription( "specifies the path to the .jar which contains the java library." );
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("FILE");
		options.addOption(  //"L", "lib-java", 
                OptionBuilder
                .create("L") );
        OptionBuilder.withLongOpt( "manifest" );
		OptionBuilder
		.withDescription( "Read in the manifest of an Android-Application. This mainly makes sense in a " +
		        "Context-sensitive analysis to get in hold of the registered Intents. The Manifest has to " +
		        "be in the extracted (human readable) XML-Format. You can extract it using apktool.");
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("FILE");
		options.addOption(  //"m", "manifest"
                OptionBuilder
                .create("m") );
         OptionBuilder.withLongOpt( "exclusions" );
		OptionBuilder
		.withDescription( "Read a file containing the classes to exclude from the analysis. " +
		        "The file contains one RegExp per line and uses '\\/' instead of dots for the " +
		        "notation");
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("FILE");
		options.addOption(  //"x", "exclusions"
                OptionBuilder
                .create("x") );
          OptionBuilder.withLongOpt( "intent" );
		OptionBuilder
		.withDescription( "The intent for INTENT-Construction mode.");
		OptionBuilder
		.hasArg();
		OptionBuilder
		.withArgName("SIG");
		options.addOption(  //"i", "intent"
                OptionBuilder
                .create("i") );

         //
         // Read options into ExecutionOptipons p
         //
        try {
            commandLine = parser.parse(options, args);
            
            if ( commandLine.hasOption( "help" ) ) {
                printUsage(options);
                System.exit(0);
            }

            { // read classpath
                String[] remainder = commandLine.getArgs();
                if (remainder.length != 1) {
                    System.out.println("Invalid number of command line arguments. " + remainder.length);
                    printUsage(options);
                    System.exit(1);
                } else {
                    p.setClassPath(remainder[0]);
                }
            }

            if ( commandLine.hasOption( "outfile" ) ) {
                p.setSdgFile(commandLine.getOptionValue("outfile"));
            }

            if ( commandLine.hasOption( "lib" ) ) {
                p.setAndroidLib(commandLine.getOptionValue("lib"));
            }

            if ( commandLine.hasOption( "lib-java" ) ) {
                p.setJavaStubs(commandLine.getOptionValue("lib-java"));
            }

            if ( commandLine.hasOption( "manifest" ) ) {
                p.setManifest(commandLine.getOptionValue("manifest"));
            }

            if ( commandLine.hasOption( "exclusions" ) ) {
                p.setExclusions(commandLine.getOptionValue("exclusions"));
            }

            if ( commandLine.hasOption( "intent" ) ) {
                p.setIntent(commandLine.getOptionValue("intent"));
            }

            if ( commandLine.hasOption( "analysis" ) ) {
                p.setPreset(AnalysisPresets.PresetDescription.valueOf(commandLine.getOptionValue("analysis").toUpperCase()));
            }

            if ( commandLine.hasOption( "construct" ) ) {
                p.setConstruct(ExecutionOptions.BuildMode.valueOf(commandLine.getOptionValue("construct").toUpperCase()));
            }

            // Entry points
            if ((commandLine.hasOption("scan")?10:0) + 
                    (commandLine.hasOption("ep-file")?10:0) + 
                    (commandLine.hasOption("e")?10:0) != 10) {
                System.err.println("There has to be exactly one option out of '--entrypoint', '--ep-file' or '--scan'.");
                System.exit(3);
            }

            if (commandLine.hasOption("e")) {
                final String entryMethod = commandLine.getOptionValue("e");
                if (entryMethod.equals("@all")) {
                    //p.setScan(true);
                    //p.setConstruct(true);
                } else {
                    //p.setScan(false);
                    //p.setConstruct(true);
                    p.setEntryMethod(entryMethod);
                }
            }
           
            if (commandLine.hasOption("scan")) {
                p.setScan(ExecutionOptions.ScanMode.valueOf(commandLine.getOptionValue("scan").toUpperCase()));
            }

            if (commandLine.hasOption("ep-file")) {
                p.setEpFile(commandLine.getOptionValue("ep-file"));
            }

            if ((commandLine.hasOption("verbose")?10:0) + 
                    (commandLine.hasOption("quiet")?10:0) + 
                    (commandLine.hasOption("debug")?10:0) > 10) {
                System.err.println("The Options verbose, quiet and debug are mutually exlusive.");
                System.exit(3);
            }
            
            if (commandLine.hasOption("verbose")) p.setOutput(AnalysisPresets.OutputDescription.VERBOSE);
            if (commandLine.hasOption("quiet")) p.setOutput(AnalysisPresets.OutputDescription.QUIET);
            if (commandLine.hasOption("debug")) p.setOutput(AnalysisPresets.OutputDescription.DEBUG);

        } catch (ParseException exception) {
            System.out.println(exception.getMessage());
            printUsage(options);
        }

        return p;
    }

	public static void main(String[] args) throws IOException, SDGConstructionException { 
        final ExecutionOptions p = evaluateOptions(args); 
        
        try {
            AndroidEntryPointManager manager = new AndroidEntryPointManager();
            JoDroidConstruction.dispatch(manager, p);
        } catch (SDGConstructionException m) {
            System.out.println("Error: " + m.getCause());
            return;
        }
	}
}
