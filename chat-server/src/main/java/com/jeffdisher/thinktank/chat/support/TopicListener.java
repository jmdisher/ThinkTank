package com.jeffdisher.thinktank.chat.support;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import com.jeffdisher.laminar.client.ListenerConnection;
import com.jeffdisher.laminar.types.Consequence;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.types.payload.Payload_KeyDelete;
import com.jeffdisher.laminar.types.payload.Payload_KeyPut;
import com.jeffdisher.laminar.utils.Assert;


/**
 * Listens to updates from Laminar on the requested topic.
 * Internally decodes the key and value data in each consequence and sends their decoded meaning to the given
 * IListenerTopicShim.
 * 
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class TopicListener<K, V> implements Closeable {
	private final TopicName _topic;
	private final IListenerTopicShim<K, V> _shim;
	private final ICodec<K> _keyCodec;
	private final ICodec<V> _valueCodec;

	private final ListenerConnection _listener;
	private final Thread _listenerThread;

	public TopicListener(InetSocketAddress server, TopicName topic, IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) throws IOException {
		_topic = topic;
		_shim = shim;
		_keyCodec = keyCodec;
		_valueCodec = valueCodec;
		
		_listener = ListenerConnection.open(server, _topic, 0L);
		_listenerThread = new Thread(() -> {
			try {
				Consequence consequence = _listener.pollForNextConsequence();
				while (null != consequence) {
					switch (consequence.type) {
					case CONFIG_CHANGE:
						// Ignore.
						break;
					case INVALID:
						throw Assert.unreachable("INVALID consequence");
					case KEY_DELETE: {
						// Decode the key.
						K key = _keyCodec.deserialize(((Payload_KeyDelete)consequence.payload).key);
						// Pass the command to the shim.
						_shim.delete(key, consequence.intentionOffset);
					}
						break;
					case KEY_PUT: {
						// Decode the key and value.
						Payload_KeyPut payload = (Payload_KeyPut)consequence.payload;
						K key = _keyCodec.deserialize(payload.key);
						V value = _valueCodec.deserialize(payload.value);
						// Pass the command to the shim.
						_shim.put(key, value, consequence.intentionOffset);
					}
						break;
					case TOPIC_CREATE:
						// Pass the command to the shim.
						_shim.create(consequence.intentionOffset);
						break;
					case TOPIC_DESTROY:
						// Pass the command to the shim.
						_shim.destroy(consequence.intentionOffset);
						break;
					default:
						throw Assert.unreachable("Unknown consequence type");
					}
					consequence = _listener.pollForNextConsequence();
				}
			} catch (InterruptedException e) {
				// We don't use interruption.
				throw Assert.unexpected(e);
			}
		});
		_listenerThread.start();
	}

	public void waitForConnectionOrFailure() throws IOException, InterruptedException {
		_listener.waitForConnectionOrFailure();
	}

	@Override
	public void close() throws IOException {
		_listener.close();
		try {
			_listenerThread.join();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}
}
