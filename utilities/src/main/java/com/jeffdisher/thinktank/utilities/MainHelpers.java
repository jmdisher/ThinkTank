package com.jeffdisher.thinktank.utilities;


/**
 * Helpers for common main entry-point idioms.  This includes things like argument parsing and failure handling.
 */
public class MainHelpers {
	public static String getArgument(String[] args, String flag) {
		String check1 = "--" + flag;
		String check2 = "-" + flag.substring(0, 1);
		String match = null;
		for (int i = 0; (null == match) && (i < (args.length - 1)); ++i) {
			if (check1.equals(args[i]) || check2.equals(args[i])) {
				match = args[i+1];
			}
		}
		return match;
	}

	public static boolean getFlag(String[] args, String flag) {
		String check1 = "--" + flag;
		String check2 = "-" + flag.substring(0, 1);
		boolean match = false;
		for (int i = 0; !match && (i < args.length); ++i) {
			if (check1.equals(args[i]) || check2.equals(args[i])) {
				match = true;
			}
		}
		return match;
	}

	public static RuntimeException failStart(String problem) {
		System.err.println(problem);
		System.err.println("Usage: ThinkTankRest (--hostname <hostname> --port <port>)|--local_only");
		System.exit(1);
		// We never reach this point but it allows us to throw in the caller so flow control is explicit.
		throw new RuntimeException();
	}
}
