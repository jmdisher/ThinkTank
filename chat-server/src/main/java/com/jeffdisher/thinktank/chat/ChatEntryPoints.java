package com.jeffdisher.thinktank.chat;

import java.io.IOException;
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
	private static final int STATUS_MISSING_AUTH = 3000;
	private static final int STATUS_STALE_AUTH = 3001;
	private static final int STATUS_INVALID_ARGUMENTS = 3002;

	public static void registerEntryPoints(RestServer server, ChatStore chatStore, IChatWriter chatWriter, PublicKey key) {
		server.addWebSocketFactory("/chat", 1, true, false, (String[] variables) -> new WebSocketListener() {
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
				long previousConsequence = _readLongVariable(variables[0]);
				if (previousConsequence >= 0L) {
					String binaryToken = _getBinaryTokenCookie(session.getUpgradeRequest().getCookies());
					if (null != binaryToken) {
						UUID uuid = BinaryToken.validateToken(key, System.currentTimeMillis(), binaryToken);
						if (null != uuid) {
							_user = uuid;
							_session = session.getRemote();
							
							// We will send an initial message just so the client side knows the auth was accepted so it can start using the socket.
							try {
								_session.sendString("READY");
							} catch (IOException e) {
								// We will just end up closing this but we should see what this error is, for future analysis.
								e.printStackTrace();
							}
							
							chatStore.addConnectionAndSendBacklog(_session, previousConsequence);
						} else {
							session.close(STATUS_STALE_AUTH, "Stale/invalid BinaryToken");
						}
					} else {
						session.close(STATUS_MISSING_AUTH, "Missing BinaryToken");
					}
				} else {
					session.close(STATUS_INVALID_ARGUMENTS, "Invalid arguments");
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
			
			private long _readLongVariable(String variable) {
				long previousConsequence;
				try {
					previousConsequence = Long.parseLong(variable);
				} catch (NumberFormatException e) {
					previousConsequence = -1L;
				}
				return previousConsequence;
			}
		});
	}


	private static String _getBinaryTokenCookie(List<HttpCookie> cookies) {
		String value = null;
		for (HttpCookie cookie : cookies) {
			if ("BT".equals(cookie.getName())) {
				value = cookie.getValue();
				break;
			}
		}
		return value;
	}
}
