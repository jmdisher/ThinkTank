package com.jeffdisher.thinktank.chat;

import java.io.Closeable;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;


/**
 * The interface of common chat container objects.
 * Manages the add/remove of WebSocket end-point listeners and posting of new data to the chat room.
 */
public interface IChatContainer extends Closeable {
	/**
	 * Adds a new WebSocket listener.  Any chat messages which arrive after this point will be send to this listener.
	 * 
	 * @param session The WebSocket end-point.
	 */
	void addConnection(RemoteEndpoint session);

	/**
	 * Removes an existing WebSocket listener.  The listener will no longer receive posted messages.
	 * 
	 * @param session The WebSocket end-point.
	 */
	void removeConnection(RemoteEndpoint session);

	/**
	 * Posts a message to the chat room which will be asynchronously sent to all attached listeners.
	 * 
	 * @param uuid The UUID of the user posting the message.
	 * @param message The message posted.
	 */
	void post(UUID uuid, String message);
}
