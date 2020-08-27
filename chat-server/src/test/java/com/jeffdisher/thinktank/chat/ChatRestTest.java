package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.Test;


public class ChatRestTest {
	private static final String AUTH_URL = "http://localhost:8080/";

	@Test
	public void testChat() throws Throwable {
		// Disable auth for this test (since this module doesn't have its own auth to populate the session).
		// (in the future, this can be removed once we just pass a signed cookie around instead of using session).
		ChatEntryPoints.TESTING_UUID = UUID.randomUUID();
		
		// Start the test.
		CyclicBarrier barrier = new CyclicBarrier(2);
		String[] messageReceivedRef = new String[1];
		ChatWrapper membrane = ChatWrapper.localWrapper();
		WebSocketClient ws = new WebSocketClient();
		ws.start();
		ws.connect(new WebSocketListener() {
			@Override
			public void onWebSocketClose(int statusCode, String reason) {
			}
			@Override
			public void onWebSocketConnect(Session session) {
			}
			@Override
			public void onWebSocketError(Throwable cause) {
			}
			@Override
			public void onWebSocketBinary(byte[] payload, int offset, int len) {
			}
			@Override
			public void onWebSocketText(String message) {
				messageReceivedRef[0] = message;
				try {
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					Assert.fail("Not expected in test");
				}
			}
		}, new URI("ws://localhost:8080/chat/listen")).get();
		
		HttpClient httpClient = new HttpClient();
		httpClient.start();
		String content = _sendRequest(httpClient, HttpMethod.POST, AUTH_URL + "chat/send", "Testing1");
		Assert.assertNotNull(content);
		barrier.await();
		Assert.assertEquals(ChatEntryPoints.TESTING_UUID + ": Testing1", messageReceivedRef[0]);
		content = _sendRequest(httpClient, HttpMethod.POST, AUTH_URL + "chat/send", "Testing2");
		Assert.assertNotNull(content);
		barrier.await();
		Assert.assertEquals(ChatEntryPoints.TESTING_UUID + ": Testing2", messageReceivedRef[0]);
		content = _sendRequest(httpClient, HttpMethod.POST, AUTH_URL + "chat/send", "Testing3");
		Assert.assertNotNull(content);
		barrier.await();
		Assert.assertEquals(ChatEntryPoints.TESTING_UUID + ": Testing3", messageReceivedRef[0]);
		httpClient.stop();
		
		ws.stop();
		membrane.stop();
	}

	private String _sendRequest(HttpClient httpClient, HttpMethod method, String url, String message) throws Throwable {
		Request request = httpClient.newRequest(url);
		request.method(method);
		if (null != message) {
			request.content(new StringContentProvider(message));
		}
		ContentResponse response = request.send();
		Assert.assertEquals(200, response.getStatus());
		return new String(response.getContent(), StandardCharsets.UTF_8);
	}



	private static class ChatWrapper {
		public static ChatWrapper localWrapper() throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Thread runner = new Thread(() -> {
				ChatRest.mainInTest(latch, new String[] {"--local_only"});
			});
			runner.start();
			latch.await();
			return new ChatWrapper(runner);
		}
		
		private final Thread _runner;
		private ChatWrapper(Thread runner) {
			_runner = runner;
		}
		public void stop() throws MalformedURLException, IOException, InterruptedException {
			HttpURLConnection connection = (HttpURLConnection)new URL(AUTH_URL + "exit").openConnection();
			connection.setRequestMethod("DELETE");
			// Note that sometimes the shutdown happens so quickly that we don't even get a response so just force the send.
			connection.getContentLength();
			connection.disconnect();
			_runner.join();
		}
	}
}
