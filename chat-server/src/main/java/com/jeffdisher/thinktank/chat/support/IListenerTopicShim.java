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
	 * @param consequenceOffset The offset of this consequence.
	 */
	void delete(K key, long intentionOffset, long consequenceOffset);

	/**
	 * Puts a value for a given key in the data store.
	 * 
	 * @param key The key to be updated.
	 * @param value The new value for the key.
	 * @param intentionOffset The intention responsible for the put.
	 * @param consequenceOffset The offset of this consequence.
	 */
	void put(K key, V value, long intentionOffset, long consequenceOffset);

	/**
	 * Creates the topic.
	 * 
	 * @param intentionOffset The intention responsible for the create.
	 * @param consequenceOffset The offset of this consequence.
	 */
	void create(long intentionOffset, long consequenceOffset);

	/**
	 * Destroys the topic.
	 * 
	 * @param intentionOffset The intention responsible for the destroy.
	 * @param consequenceOffset The offset of this consequence.
	 */
	void destroy(long intentionOffset, long consequenceOffset);
}
