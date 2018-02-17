package no.artorp.upnp_util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import no.artorp.upnp_util.util.Version;

public class Main {
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("For usage information, run");
			System.out.println("  upnp-util -h\n");
		}
		
		// Parse arguments
		CommandLineParser parser = new DefaultParser();
		
		Options options = new Options();
		
		options.addOption("h", "help", false, "print this message");
		options.addOption("v", "version", false, "print the version information and exit");
		options.addOption("r", "run", false, "run the port mapper wizard");
		
		boolean runWizard = false;
		
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("h")) {
				printUsageThenExit(options);
			} else if (cmd.hasOption("v")) {
				System.out.println("upnp-util " + Version.getVersion());
				System.exit(0);
			}
			runWizard = cmd.hasOption("r");
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			printUsageThenExit(options);
		}
		
		// launch program
		
		App app = new App(runWizard);
		app.run();
	}
	
	private static void printUsageThenExit(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		String footer = "Example:\n    upnp-util -l";
		formatter.printHelp("upnp-util [OPTIONS]", null, options, footer);
		System.exit(0);
		return;
	}
}
