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
import java.util.LinkedList;
import java.util.List;
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
		}, new URI("ws://localhost:8080/chat/0"), upgradeRequest).get();
		
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

	@Test
	public void testResumedChat() throws Throwable {
		// Generate the token we will use for auth (expires at the end of time).
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		UUID uuid1 = UUID.randomUUID();
		UUID uuid2 = UUID.randomUUID();
		String token1 = BinaryToken.createToken(pair.getPrivate(), uuid1, Long.MAX_VALUE);
		String token2 = BinaryToken.createToken(pair.getPrivate(), uuid2, Long.MAX_VALUE);
		
		// Start the chat component.
		ChatWrapper chatter = ChatWrapper.localWrapper(pair.getPublic());
		
		// Start the first client and post a message.
		FakeClient client1 = FakeClient.startClient(token1, 0L);
		client1.sendMessage("Message1");
		client1.waitForMessageCount(1);
		Assert.assertEquals("Message1", client1.getLastMessageContent());
		
		// Start the second client, observe that it sees the original message, and have it post a new one.
		FakeClient client2 = FakeClient.startClient(token2, 0L);
		client2.waitForMessageCount(1);
		Assert.assertEquals("Message1", client2.getLastMessageContent());
		client2.sendMessage("Message2");
		
		// Both clients should see this.
		client1.waitForMessageCount(2);
		Assert.assertEquals("Message2", client1.getLastMessageContent());
		client2.waitForMessageCount(2);
		Assert.assertEquals("Message2", client2.getLastMessageContent());
		
		// Stop one client, post a new message, restart it, and see that it observes the message it missed.
		long previousIndex = client1.getLastMessageIndex();
		client1.stopClient();
		client2.sendMessage("Message3");
		client2.waitForMessageCount(3);
		Assert.assertEquals("Message3", client2.getLastMessageContent());
		
		client1 = FakeClient.startClient(token1, previousIndex);
		// It won't receive the old messages so we only expect 1.
		client1.waitForMessageCount(1);
		Assert.assertEquals("Message3", client1.getLastMessageContent());
		chatter.stop();
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


	private static class FakeClient {
		public static FakeClient startClient(String binaryToken, long previousOffset) throws Exception {
			FakeClient client = new FakeClient();
			
			WebSocketClient ws = new WebSocketClient();
			ws.start();
			// We manually create the upgrade request so we can send the token in the cookies.
			ClientUpgradeRequest upgradeRequest = new ClientUpgradeRequest();
			upgradeRequest.setCookies(Collections.singletonList(new HttpCookie("BT", binaryToken)));
			Session session = ws.connect(new WebSocketListener() {
				boolean _didReceiveInitialMessage = false;
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
					// We are currently using the first message as just empty to prove that the auth was verified so skip it.
					if (_didReceiveInitialMessage) {
						client._receiveMessage(message);
					} else {
						_didReceiveInitialMessage = true;
					}
				}
			}, new URI("ws://localhost:8080/chat/" + previousOffset), upgradeRequest).get();
			
			client._configure(ws, session);
			return client;
		}
		
		
		private final List<String> _received;
		private WebSocketClient _client;
		private Session _session;
		private RemoteEndpoint _remote;
		
		public void sendMessage(String message) throws IOException {
			_remote.sendString(message);
		}
		
		public synchronized void waitForMessageCount(int count) {
			while (_received.size() < count) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					Assert.fail("Not used ");
				}
			}
		}
		
		public String getLastMessageContent() {
			String rawJSon = _received.get(_received.size() - 1);
			JsonObject object = Json.parse(rawJSon).asObject();
			return object.getString("content", null);
		}
		
		public long getLastMessageIndex() {
			String rawJSon = _received.get(_received.size() - 1);
			JsonObject object = Json.parse(rawJSon).asObject();
			return object.getInt("index", -1);
		}
		
		public void stopClient() throws Exception {
			_session.close();
			_client.stop();
		}
		
		private FakeClient() {
			_received = new LinkedList<>();
		}
		
		private void _configure(WebSocketClient client, Session session) {
			_client = client;
			_session = session;
			_remote = session.getRemote();
		}
		
		private synchronized void _receiveMessage(String message) {
			_received.add(message);
			this.notify();
		}
	}
}
