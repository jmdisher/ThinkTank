package com.jeffdisher.thinktank.auth;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.util.resource.ResourceCollection;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.breakwater.utilities.Assert;
import com.jeffdisher.thinktank.exit.ExitEntryPoints;
import com.jeffdisher.thinktank.utilities.ResourceHelpers;


/**
 * The top-level launcher for the authentication system back-end, when it is being used in isolation.
 */
public class AuthRest {
	private static final int REST_PORT = 8080;

	public static void main(String[] args) {
		// The normal entry-point doesn't care about the latch so just create anything.
		_main(new CountDownLatch(0), args);
	}

	public static void mainInTest(CountDownLatch bindLatch, String[] args) {
		// We are being called from a test so we want to pass in the latch the test gave us.
		_main(bindLatch, args);
	}

	private static void _main(CountDownLatch bindLatch, String[] args) {
		// Create the server and start it.
		ResourceCollection combinedCollection;
		try {
			combinedCollection = ResourceHelpers.buildResourceCollection(
					System.getProperty("user.dir") + "/../resources/",
					System.getProperty("user.dir") + "/resources/"
			);
		} catch (IOException e1) {
			// We treat a failure to resolve this path as a fatal, and highly expected, error.
			throw Assert.unexpected(e1);
		}
		RestServer server = new RestServer(REST_PORT, combinedCollection);
		CountDownLatch stopLatch = new CountDownLatch(1);
		// -install the exit entry-point.
		ExitEntryPoints.registerEntryPoints(stopLatch, server);
		AuthEntryPoints.registerEntryPoints(server);
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
	}
}
