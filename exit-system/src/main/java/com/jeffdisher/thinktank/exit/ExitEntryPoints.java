package com.jeffdisher.thinktank.exit;

import java.util.concurrent.CountDownLatch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeffdisher.breakwater.RestServer;


/**
 * Installs the DELETE "/exit" entry-point which just unblocks the thread managing the server to enter its shutdown
 * phase.
 * Note that the exit capability, in general, is just for broader integration testing as this would likely be controlled
 * a different way (or just "run until crash/killed") in a production environment.
 */
public class ExitEntryPoints {
	public static void registerEntryPoints(CountDownLatch stopLatch, RestServer server) {
		server.addDeleteHandler("/exit", 0, (HttpServletRequest request, HttpServletResponse response, String[] variables) -> {
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("Shutting down");
			stopLatch.countDown();
		});
	}
}
