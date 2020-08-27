package com.jeffdisher.thinktank;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.util.resource.ResourceCollection;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.thinktank.auth.AuthEntryPoints;
import com.jeffdisher.thinktank.chat.ChatEntryPoints;
import com.jeffdisher.thinktank.chat.ChatRest;
import com.jeffdisher.thinktank.chat.IChatContainer;
import com.jeffdisher.thinktank.exit.ExitEntryPoints;
import com.jeffdisher.thinktank.utilities.MainHelpers;
import com.jeffdisher.thinktank.utilities.ResourceHelpers;


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
		String hostname = MainHelpers.getArgument(args, ARG_HOSTNAME);
		String portString = MainHelpers.getArgument(args, ARG_PORT);
		boolean localOnly = MainHelpers.getFlag(args, ARG_LOCAL_ONLY);
		if (!localOnly && (null == hostname)) {
			MainHelpers.failStart("Missing hostname");
		}
		if (!localOnly && (null == portString)) {
			MainHelpers.failStart("Missing port");
		}
		
		// Start the chat container (owns the Laminar connection).
		IChatContainer chatContainer = ChatRest.buildChatContainer(hostname, portString, localOnly);
		
		// Create the server and start it.
		ResourceCollection combinedCollection;
		try {
			combinedCollection = ResourceHelpers.buildResourceCollection(
					System.getProperty("user.dir") + "/../auth-system/resources/",
					System.getProperty("user.dir") + "/../chat-server/resources/",
					System.getProperty("user.dir") + "/../resources/"
			);
		} catch (IOException e1) {
			// We treat a failure to resolve this path as a fatal, and highly expected, error.
			throw Assert.unexpected(e1);
		}
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
}
