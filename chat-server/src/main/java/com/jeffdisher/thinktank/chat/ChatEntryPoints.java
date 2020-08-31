package com.jeffdisher.thinktank.chat;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.breakwater.StringMultiMap;
import com.jeffdisher.thinktank.crypto.BinaryToken;


/**
 * Entry-points related to chat system:
 * -POST /chat/send
 * -WebSocket listen for "text" /chat/listen
 */
public class ChatEntryPoints {
	public static void registerEntryPoints(RestServer server, IChatContainer chatContainer, PublicKey key) {
		server.addPostHandler("/chat/send", 0, (HttpServletRequest request, HttpServletResponse response, String[] pathVariables, StringMultiMap<String> formVariables, StringMultiMap<byte[]> multiPart, byte[] rawPost) -> {
			UUID uuid = _getUuidFromCookie(request, key);
			if (null != uuid) {
				String message = new String(rawPost, StandardCharsets.UTF_8);
				chatContainer.post(uuid, message);
				response.setStatus(HttpServletResponse.SC_OK);
			} else {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.getWriter().println("Required UUID");
			}
		});
		server.addWebSocketFactory("/chat/listen", 0, true, false, (String[] variables) -> new WebSocketListener() {
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
				_session = session.getRemote();
				chatContainer.addConnection(_session);
			}
			
			@Override
			public void onWebSocketClose(int statusCode, String reason) {
				chatContainer.removeConnection(_session);
			}
			
			@Override
			public void onWebSocketText(String message) {
			}
			
			@Override
			public void onWebSocketBinary(byte[] payload, int offset, int len) {
			}
		});
	}


	private static UUID _getUuidFromCookie(HttpServletRequest request, PublicKey key) {
		UUID value = null;
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if ("BT".equals(cookie.getName())) {
				String encoded = cookie.getValue();
				value = BinaryToken.validateToken(key, System.currentTimeMillis(), encoded);
				break;
			}
		}
		return value;
	}
}
