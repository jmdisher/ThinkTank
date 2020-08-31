package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import com.jeffdisher.laminar.utils.Assert;


/**
 * Data structure which represents what the server knows about the state of the chatroom and all the connected users.
 */
public class ChatStore {
	private final Set<RemoteEndpoint> _connections = new HashSet<>();


	public synchronized void newMessageArrived(String message) {
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

}
