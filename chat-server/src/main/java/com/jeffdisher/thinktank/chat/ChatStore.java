package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import com.eclipsesource.json.JsonObject;
import com.jeffdisher.laminar.utils.Assert;


/**
 * Data structure which represents what the server knows about the state of the chatroom and all the connected users.
 */
public class ChatStore {
	private final Set<RemoteEndpoint> _connections = new HashSet<>();


	/**
	 * Tells the store that a new message has arrived which should be relayed to the connected users.
	 * 
	 * @param sender The sender of the message.
	 * @param content The message content.
	 * @param index The 1-indexed representation of the order this message arrived (after any message with a lower
	 * index).
	 */
	public synchronized void newMessageArrived(UUID sender, String content, long index) {
		String message = _messageAsJson(sender, content, index);
		for (RemoteEndpoint endpoint : _connections) {
			try {
				endpoint.sendString(message);
			} catch (IOException e) {
				// This is fatal since we don't have handling for it.
				throw Assert.unimplemented(e.getLocalizedMessage());
			}
		}
	}

	public synchronized void addConnection(RemoteEndpoint session) {
		Assert.assertTrue(null != session);
		boolean didAdd = _connections.add(session);
		Assert.assertTrue(didAdd);
	}

	public synchronized void removeConnection(RemoteEndpoint session) {
		Assert.assertTrue(null != session);
		boolean didRemove = _connections.remove(session);
		Assert.assertTrue(didRemove);
	}


	private static String _messageAsJson(UUID sender, String content, long index) {
		JsonObject object = new JsonObject();
		object.add("sender", sender.toString());
		object.add("content", content);
		object.add("index", index);
		return object.toString();
	}
}
