package com.jeffdisher.thinktank.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

import com.jeffdisher.laminar.client.ClientConnection;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.thinktank.chat.support.IListenerTopicShim;
import com.jeffdisher.thinktank.chat.support.StringCodec;
import com.jeffdisher.thinktank.chat.support.TopicListener;
import com.jeffdisher.thinktank.chat.support.UUIDCodec;


/**
 * The real implementation of IChatWriter which is back-ended on a Laminar cluster via the topic name "chat".
 */
public class ChatLaminar implements IChatWriter {
	private static final TopicName TOPIC_NAME = TopicName.fromString("chat");

	private final ChatStore _chatStore;
	private final UUIDCodec _keyCodec;
	private final StringCodec _valueCodec;
	private final ClientConnection _writer;
	private final TopicListener<UUID, String> _listener;

	public ChatLaminar(ChatStore chatStore, InetSocketAddress laminarServer) throws IOException {
		_chatStore = chatStore;
		_keyCodec = new UUIDCodec();
		_valueCodec = new StringCodec();
		
		// Open the writing connection.
		_writer = ClientConnection.open(laminarServer);
		try {
			_writer.waitForConnectionOrFailure();
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
		
		// Create chat topic (we don't care about whether it was accepted as this may already exist).
		try {
			_writer.sendCreateTopic(TOPIC_NAME).waitForCommitted();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
		
		// Open the listening connection.
		_listener = new TopicListener<UUID, String>(laminarServer, TOPIC_NAME, new ChatListenerShim(), _keyCodec, _valueCodec);
		try {
			_listener.waitForConnectionOrFailure();
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}

	@Override
	public synchronized void post(UUID writer, String post) {
		Assert.assertTrue(null != post);
		// We will ignore the response, just waiting for it to commit.
		try {
			_writer.sendPut(TOPIC_NAME, _keyCodec.serialize(writer), _valueCodec.serialize(post)).waitForCommitted();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}

	@Override
	public void close() throws IOException {
		_writer.close();
		_listener.close();
	}


	private class ChatListenerShim implements IListenerTopicShim<UUID, String> {
		@Override
		public void delete(UUID key, long intentionOffset, long consequenceOffset) {
			throw Assert.unreachable("We don't delete keys");
		}
		@Override
		public void put(UUID key, String value, long intentionOffset, long consequenceOffset) {
			// We will send in the consequenceOffset since it is the dense and non-duplicated value.
			_chatStore.newMessageArrived(key, value, consequenceOffset);
		}
		@Override
		public void create(long intentionOffset, long consequenceOffset) {
			// No special action on create.
		}
		@Override
		public void destroy(long intentionOffset, long consequenceOffset) {
			throw Assert.unreachable("We don't destroy the topic");
		}
	}
}
