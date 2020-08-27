package com.jeffdisher.thinktank.chat.support;


public interface IListenerTopicShim<K, V> {
	void delete(K key, long intentionOffset);

	void put(K key, V value, long intentionOffset);

	void create(long intentionOffset);

	void destroy(long intentionOffset);
}
