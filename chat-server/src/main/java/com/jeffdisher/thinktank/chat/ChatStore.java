package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import com.eclipsesource.json.JsonObject;
import com.jeffdisher.laminar.utils.Assert;


/**
 * Data structure which represents what the server knows about the state of the chatroom and all the connected users.
 */
public class ChatStore {
	private static final int CACHE_SIZE = 10;
	private final Set<RemoteEndpoint> _connections = new HashSet<>();
	private final Queue<MessageTuple> _cache = new LinkedList<>();


	/**
	 * Tells the store that a new message has arrived which should be relayed to the connected users.
	 * 
	 * @param sender The sender of the message.
	 * @param content The message content.
	 * @param index The 1-indexed representation of the order this message arrived (after any message with a lower
	 * index).
	 */
	public synchronized void newMessageArrived(UUID sender, String content, long index) {
		MessageTuple tuple = new MessageTuple(sender, content, index);
		_cache.add(tuple);
		if (_cache.size() > CACHE_SIZE) {
			_cache.remove();
		}
		String message = tuple.toJson();
		for (RemoteEndpoint endpoint : _connections) {
			try {
				endpoint.sendString(message);
			} catch (IOException e) {
				// This is fatal since we don't have handling for it.
				throw Assert.unimplemented(e.getLocalizedMessage());
			}
		}
	}

	public synchronized void addConnectionAndSendBacklog(RemoteEndpoint session, long previousIndex) {
		Assert.assertTrue(null != session);
		boolean didAdd = _connections.add(session);
		Assert.assertTrue(didAdd);
		
		// Send off anything in the cache which is after this index.
		try {
			for (MessageTuple tuple : _cache) {
				if (tuple.index > previousIndex) {
					session.sendString(tuple.toJson());
				}
			}
		} catch (IOException e) {
			// This is fatal since we don't have handling for it.
			throw Assert.unimplemented(e.getLocalizedMessage());
		}
	}

	public synchronized void removeConnection(RemoteEndpoint session) {
		Assert.assertTrue(null != session);
		boolean didRemove = _connections.remove(session);
		Assert.assertTrue(didRemove);
	}


	private static class MessageTuple {
		public final UUID sender;
		public final String content;
		public final long index;
		
		public MessageTuple(UUID sender, String content, long index) {
			this.sender = sender;
			this.content = content;
			this.index = index;
		}
		
		public String toJson() {
			JsonObject object = new JsonObject();
			object.add("sender", this.sender.toString());
			object.add("content", this.content);
			object.add("index", this.index);
			return object.toString();
		}
	}
}
