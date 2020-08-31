package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.thinktank.chat.support.StringCodec;
import com.jeffdisher.thinktank.chat.support.UUIDCodec;


/**
 * A local-only implementation of the IChatWriter.  This is used for testing or other stand-alone environments where
 * there is no Laminar cluster.
 * A background thread sends the messages out so the system still provides the same asynchronous behaviour as a real
 * back-end.
 */
public class ChatLocal implements IChatWriter {
	private static final UUIDCodec KEY_CODEC = new UUIDCodec();
	private static final StringCodec VALUE_CODEC = new StringCodec();

	private final ChatStore _chatStore;
	private final Queue<String> _messages;
	private final Thread _background;
	private boolean _keepRunning;

	public ChatLocal(ChatStore chatStore) {
		_chatStore = chatStore;
		_messages = new LinkedList<>();
		_background = new Thread(() -> {
			String message = _waitNextMessage();
			while (null != message) {
				_chatStore.newMessageArrived(message);
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
