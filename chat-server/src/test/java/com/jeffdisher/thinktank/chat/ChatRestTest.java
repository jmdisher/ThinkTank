package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.Assert;
import org.junit.Test;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.jeffdisher.thinktank.crypto.BinaryToken;
import com.jeffdisher.thinktank.crypto.CryptoHelpers;


public class ChatRestTest {
	private static final String BASE_URL = "http://localhost:8080/";

	@Test
	public void testChat() throws Throwable {
		// Generate the token we will use for auth (expires at the end of time).
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		UUID uuid = UUID.randomUUID();
		String token = BinaryToken.createToken(pair.getPrivate(), uuid, Long.MAX_VALUE);
		
		// Start the test.
		CyclicBarrier barrier = new CyclicBarrier(2);
		String[] messageReceivedRef = new String[1];
		ChatWrapper membrane = ChatWrapper.localWrapper(pair.getPublic());
		WebSocketClient ws = new WebSocketClient();
		ws.start();
		// We manually create the upgrade request so we can send the token in the cookies.
		ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
		upgradeRequest.setCookies(Collections.singletonList(new HttpCookie("BT", token)));
		Session session = ws.connect(new WebSocketListener() {
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
		}, new URI("ws://localhost:8080/chat"), upgradeRequest).get();
		
		RemoteEndpoint remote = session.getRemote();
		
		// Wait for auth string to come back.
		barrier.await();
		Assert.assertEquals("READY", messageReceivedRef[0]);
		
		remote.sendString("Testing1");
		barrier.await();
		JsonObject object = Json.parse(messageReceivedRef[0]).asObject();
		Assert.assertEquals(uuid.toString(), object.getString("sender", null));
		Assert.assertEquals("Testing1", object.getString("content", null));
		
		remote.sendString("Testing2");
		barrier.await();
		object = Json.parse(messageReceivedRef[0]).asObject();
		Assert.assertEquals(uuid.toString(), object.getString("sender", null));
		Assert.assertEquals("Testing2", object.getString("content", null));
		
		remote.sendString("Testing3");
		barrier.await();
		object = Json.parse(messageReceivedRef[0]).asObject();
		Assert.assertEquals(uuid.toString(), object.getString("sender", null));
		Assert.assertEquals("Testing3", object.getString("content", null));
		
		session.close();
		
		ws.stop();
		membrane.stop();
	}


	private static class ChatWrapper {
		public static ChatWrapper localWrapper(PublicKey key) throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Thread runner = new Thread(() -> {
				ChatRest.mainInTest(latch, new String[] {"--local_only", "--key", Base64.getEncoder().encodeToString(CryptoHelpers.serializePublic(key))});
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
			HttpURLConnection connection = (HttpURLConnection)new URL(BASE_URL + "exit").openConnection();
			connection.setRequestMethod("DELETE");
			// Note that sometimes the shutdown happens so quickly that we don't even get a response so just force the send.
			connection.getContentLength();
			connection.disconnect();
			_runner.join();
		}
	}
}
