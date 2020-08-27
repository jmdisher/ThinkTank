package com.jeffdisher.thinktank.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;

import com.jeffdisher.thinktank.auth.AuthRest;


public class AuthRestTest {
	private static final String ROOT_URL = "http://localhost:8080/";

	@Test
	public void testStartupShutdown() throws Throwable {
		CountDownLatch bindLatch = new CountDownLatch(1);
		Thread server = new Thread(() -> {
			AuthRest.mainInTest(bindLatch, new String[0]);
		});
		server.start();
		bindLatch.await();
		
		HttpClient httpClient = new HttpClient();
		httpClient.start();
		
		// Login.
		UUID uuid = UUID.randomUUID();
		String uuidString = uuid.toString();
		String content = _sendRequest(httpClient, HttpMethod.POST, ROOT_URL + "login/" + uuidString, null);
		Assert.assertEquals(uuidString + "\n", content);
		// Check ID.
		content = _sendRequest(httpClient, HttpMethod.GET, ROOT_URL + "getid", null);
		Assert.assertEquals(uuidString + "\n", content);
		// Logout.
		content = _sendRequest(httpClient, HttpMethod.POST, ROOT_URL + "logout", null);
		Assert.assertEquals("logged out\n", content);
		// Check ID.
		content = _sendRequest(httpClient, HttpMethod.GET, ROOT_URL + "getid", null);
		Assert.assertEquals("Not logged in\n", content);
		// Shutdown.
		content = _sendRequest(httpClient, HttpMethod.DELETE, ROOT_URL + "exit", null);
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
		String content = new String(request.send().getContent(), StandardCharsets.UTF_8);
		return content;
	}
}
