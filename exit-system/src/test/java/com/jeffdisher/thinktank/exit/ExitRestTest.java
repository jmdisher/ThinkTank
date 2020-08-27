package com.jeffdisher.thinktank.exit;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;


public class ExitRestTest {
	private static final String EXIT_URL = "http://localhost:8080/exit";

	@Test
	public void testStartupShutdown() throws Throwable {
		CountDownLatch bindLatch = new CountDownLatch(1);
		Thread server = new Thread(() -> {
			ExitRest.mainInTest(bindLatch, new String[0]);
		});
		server.start();
		bindLatch.await();
		
		HttpClient httpClient = new HttpClient();
		httpClient.start();
		String content = _sendRequest(httpClient, HttpMethod.DELETE, EXIT_URL, null);
		Assert.assertEquals("Shutting down\n", content);
		httpClient.stop();
		
		server.join();
	}


	private String _sendRequest(HttpClient httpClient, HttpMethod method, String url, String message) throws Throwable {
		Request request = httpClient.newRequest(url);
		request.method(method);
		if (null != message) {
			request.content(new StringContentProvider(message));
		}
		return new String(request.send().getContent(), StandardCharsets.UTF_8);
	}
}
