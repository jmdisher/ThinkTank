package com.jeffdisher.thinktank.chat.support;


/**
 * Common interface used by TopicListener to serialize/deserialize keys and values in communication with Laminar.
 * 
 * @param <T> The high-level type being serialized/deserialized.
 */
public interface ICodec <T> {
	/**
	 * Creates a new instance of type T from the given bytes.
	 * 
	 * @param bytes The bytes to deserialized.
	 * @return The new instance of T.
	 */
	T deserialize(byte[] bytes);

	/**
	 * Serializes the given object to bytes.
	 * 
	 * @param object The object instance to serialize.
	 * @return The bytes of serializing the object.
	 */
	byte[] serialize(T object);
}
