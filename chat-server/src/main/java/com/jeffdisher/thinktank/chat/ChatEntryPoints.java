package com.jeffdisher.thinktank.chat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.breakwater.StringMultiMap;


/**
 * Manages responding to and creating the handlers for the REST server.
 */
public class ChatEntryPoints {
	/**
	 * Set a UUID to act as when testing while not logged in.
	 */
	public static UUID TESTING_UUID = null;

	public static void registerEntryPoints(RestServer server, IChatContainer chatContainer) {
		server.addPostHandler("/chat/send", 0, (HttpServletRequest request, HttpServletResponse response, String[] pathVariables, StringMultiMap<String> formVariables, StringMultiMap<byte[]> multiPart, byte[] rawPost) -> {
			UUID uuid = null;
			if (null == TESTING_UUID) {
				HttpSession session = request.getSession(false);
				uuid = (null != session)
						? (UUID) session.getAttribute("uuid")
						: null;
			} else {
				uuid = TESTING_UUID;
			}
			if (null != uuid) {
				String message = new String(rawPost, StandardCharsets.UTF_8);
				chatContainer.post(uuid, message);
				response.setStatus(HttpServletResponse.SC_OK);
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Required UUID");
			}
		});
		server.addWebSocketFactory("/chat/listen", 0, true, false, (String[] variables) -> new WebSocketListener() {
			private RemoteEndpoint _session;
			@Override
			public void onWebSocketError(Throwable cause) {
				// This is usually just a timeout but we will print stack trace while still testing.
				cause.printStackTrace();
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
}
