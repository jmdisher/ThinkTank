package com.jeffdisher.thinktank;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.thinktank.auth.AuthEntryPoints;
import com.jeffdisher.thinktank.chat.ChatEntryPoints;
import com.jeffdisher.thinktank.chat.ChatRest;
import com.jeffdisher.thinktank.chat.IChatContainer;
import com.jeffdisher.thinktank.exit.ExitEntryPoints;


public class ThinkTankRest {
	private static final String ARG_HOSTNAME = "hostname";
	private static final String ARG_PORT = "port";
	private static final String ARG_LOCAL_ONLY = "local_only";

	public static void main(String[] args) {
		// The normal entry-point doesn't care about the latch so just create anything.
		_main(new CountDownLatch(0), args);
	}

	public static void mainInTest(CountDownLatch bindLatch, String[] args) {
		// We are being called from a test so we want to pass in the latch the test gave us.
		_main(bindLatch, args);
	}

	private static void _main(CountDownLatch bindLatch, String[] args) {
		// Parse arguments.
		String hostname = _getArgument(args, ARG_HOSTNAME);
		String portString = _getArgument(args, ARG_PORT);
		boolean localOnly = _getFlag(args, ARG_LOCAL_ONLY);
		if (!localOnly && (null == hostname)) {
			_failStart("Missing hostname");
		}
		if (!localOnly && (null == portString)) {
			_failStart("Missing port");
		}
		
		// Start the chat container (owns the Laminar connection).
		IChatContainer chatContainer = ChatRest.buildChatContainer(hostname, portString, localOnly);
		
		// Create the server and start it.
		PathResource globalResource = _createPathResource(System.getProperty("user.dir") + "/../resources/");
		PathResource authResource = _createPathResource(System.getProperty("user.dir") + "/../auth-system/resources/");
		PathResource chatResource = _createPathResource(System.getProperty("user.dir") + "/../chat-server/resources/");
		ResourceCollection combinedCollection = new ResourceCollection(authResource, chatResource, globalResource);
		RestServer server = new RestServer(8080, combinedCollection);
		CountDownLatch stopLatch = new CountDownLatch(1);
		// Install all the entry-points.
		ExitEntryPoints.registerEntryPoints(stopLatch, server);
		AuthEntryPoints.registerEntryPoints(server);
		ChatEntryPoints.registerEntryPoints(server, chatContainer);
		server.start();
		
		// Count-down the latch in case we are part of a testing environment.
		bindLatch.countDown();
		
		// Wait until we are told to shut down.
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
		server.stop();
		try {
			chatContainer.close();
		} catch (IOException e) {
			// If this happens on shutdown, just print it.
			e.printStackTrace();
		}
	}

	private static String _getArgument(String[] args, String flag) {
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

	private static RuntimeException _failStart(String problem) {
		System.err.println(problem);
		System.err.println("Usage: ThinkTankRest (--hostname <hostname> --port <port>)|--local_only");
		System.exit(1);
		// We never reach this point but it allows us to throw in the caller so flow control is explicit.
		throw new RuntimeException();
	}

	private static PathResource _createPathResource(String path) throws AssertionError {
		// NOTE:  We MUST create this as a "real path" or Jetty decides it is an alias and doesn't follow it when loading resources.
		PathResource pathResource;
		try {
			pathResource = new PathResource(Paths.get(path).toRealPath(new LinkOption[0]));
		} catch (IOException e1) {
			// We treat a failure to resolve this path as a fatal, and highly expected, error.
			throw Assert.unexpected(e1);
		}
		return pathResource;
	}

	private static boolean _getFlag(String[] args, String flag) {
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
}
