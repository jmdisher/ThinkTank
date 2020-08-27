package com.jeffdisher.thinktank.chat.support;


/**
 * The target of decoded Laminar messages found by the TopicListener.
 * 
 * @param <K> The key type.
 * @param <V> The value type.
 */
public interface IListenerTopicShim<K, V> {
	/**
	 * Deletes key from the data store.
	 * 
	 * @param key The key to be deleted.
	 * @param intentionOffset The intention responsible for the delete.
	 */
	void delete(K key, long intentionOffset);

	/**
	 * Puts a value for a given key in the data store.
	 * 
	 * @param key The key to be updated.
	 * @param value The new value for the key.
	 * @param intentionOffset The intention responsible for the put.
	 */
	void put(K key, V value, long intentionOffset);

	/**
	 * Creates the topic.
	 * 
	 * @param intentionOffset The intention responsible for the create.
	 */
	void create(long intentionOffset);

	/**
	 * Destroys the topic.
	 * 
	 * @param intentionOffset The intention responsible for the destroy.
	 */
	void destroy(long intentionOffset);
}
