package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.thinktank.chat.support.StringCodec;


public class ChatLocal implements IChatContainer {
	private static final UUIDCodec KEY_CODEC = new UUIDCodec();
	private static final StringCodec VALUE_CODEC = new StringCodec();

	private final Set<RemoteEndpoint> _connections;
	private final Queue<String> _messages;
	private final Thread _background;
	private boolean _keepRunning;

	public ChatLocal() {
		_connections = new HashSet<>();
		_messages = new LinkedList<>();
		_background = new Thread(() -> {
			String message = _waitNextMessage();
			while (null != message) {
				synchronized(this) {
					for (RemoteEndpoint endpoint : _connections) {
						try {
							endpoint.sendString(message);
						} catch (IOException e) {
							// This is fatal since we don't have handling for it.
							throw Assert.unimplemented(e.getLocalizedMessage());
						}
					}
				}
				message = _waitNextMessage();
			}
		});
		_keepRunning = true;
		_background.start();
	}

	@Override
	public void close() throws IOException {
		synchronized (this) {
			_keepRunning = false;
			this.notify();
		}
		try {
			_background.join();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}

	@Override
	public synchronized void addConnection(RemoteEndpoint session) {
		Assert.assertTrue(null != session);
		_connections.add(session);
	}

	@Override
	public synchronized void removeConnection(RemoteEndpoint session) {
		Assert.assertTrue(null != session);
		_connections.remove(session);
	}

	@Override
	public synchronized void post(UUID uuid, String message) {
		// Fake serialization, to verify the codecs work as expected.
		UUID key = KEY_CODEC.deserialize(KEY_CODEC.serialize(uuid));
		String value = VALUE_CODEC.deserialize(VALUE_CODEC.serialize(message));
		_messages.add(key + ": " + value);
		this.notify();
	}


	private synchronized String _waitNextMessage() {
		while (_keepRunning && _messages.isEmpty()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				// We don't use interruption.
				throw Assert.unexpected(e);
			}
		}
		return _keepRunning
				? _messages.remove()
				: null;
	}
}
