package com.jeffdisher.thinktank.chat;

import java.net.HttpCookie;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.thinktank.crypto.BinaryToken;


/**
 * Entry-points related to chat system:
 * -POST /chat/send
 * -WebSocket listen for "text" /chat/listen
 */
public class ChatEntryPoints {
	/**
	 * Status codes [3000..4999] seem to be available for user-defined use.
	 */
	private static final int STATUS_AUTH = 3000;

	public static void registerEntryPoints(RestServer server, ChatStore chatStore, IChatWriter chatWriter, PublicKey key) {
		server.addWebSocketFactory("/chat", 0, true, false, (String[] variables) -> new WebSocketListener() {
			private UUID _user;
			private RemoteEndpoint _session;
			@Override
			public void onWebSocketError(Throwable cause) {
				// This is usually just a timeout closing the socket which is harmless but we want to log other errors.
				if (cause.getCause() instanceof TimeoutException) {
					// Do nothing.
				} else {
					cause.printStackTrace();
				}
			}
			
			@Override
			public void onWebSocketConnect(Session session) {
				UUID uuid = _getUuidFromCookie(session.getUpgradeRequest().getCookies(), key);
				if (null != uuid) {
					_user = uuid;
					_session = session.getRemote();
					chatStore.addConnection(_session);
				} else {
					session.close(STATUS_AUTH, "Missing BinaryToken");
				}
			}
			
			@Override
			public void onWebSocketClose(int statusCode, String reason) {
				if (null != _session) {
					chatStore.removeConnection(_session);
				}
			}
			
			@Override
			public void onWebSocketText(String message) {
				if (null != _user) {
					chatWriter.post(_user, message);
				}
			}
			
			@Override
			public void onWebSocketBinary(byte[] payload, int offset, int len) {
			}
		});
	}


	private static UUID _getUuidFromCookie(List<HttpCookie> cookies, PublicKey key) {
		UUID value = null;
		for (HttpCookie cookie : cookies) {
			if ("BT".equals(cookie.getName())) {
				String encoded = cookie.getValue();
				value = BinaryToken.validateToken(key, System.currentTimeMillis(), encoded);
				break;
			}
		}
		return value;
	}
}
