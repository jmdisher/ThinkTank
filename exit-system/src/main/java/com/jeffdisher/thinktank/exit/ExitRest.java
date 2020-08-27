package com.jeffdisher.thinktank.exit;

import java.util.concurrent.CountDownLatch;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.breakwater.utilities.Assert;


/**
 * The stand-alone launcher for the exit mechanism.  This is purely for testing/demonstration.
 */
public class ExitRest {
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
		// No static content for this test server.
		RestServer server = new RestServer(REST_PORT, null);
		CountDownLatch stopLatch = new CountDownLatch(1);
		ExitEntryPoints.registerEntryPoints(stopLatch, server);
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
