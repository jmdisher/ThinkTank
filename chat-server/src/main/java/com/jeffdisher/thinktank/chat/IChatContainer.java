package com.jeffdisher.thinktank.chat;

import java.io.Closeable;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;


public interface IChatContainer extends Closeable {
	void addConnection(RemoteEndpoint session);

	void removeConnection(RemoteEndpoint session);

	void post(UUID uuid, String message);
}
