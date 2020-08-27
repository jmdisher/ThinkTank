package com.jeffdisher.thinktank.chat.support;

import java.nio.ByteBuffer;
import java.util.UUID;


/**
 * Serializes/deserializes UUIDs for Laminar.
 */
public class UUIDCodec implements ICodec<UUID> {
	@Override
	public UUID deserialize(byte[] bytes) {
		ByteBuffer wrapper = ByteBuffer.wrap(bytes);
		return new UUID(wrapper.getLong(), wrapper.getLong());
	}
	@Override
	public byte[] serialize(UUID object) {
		return ByteBuffer.allocate(2 * Long.BYTES)
			.putLong(object.getMostSignificantBits())
			.putLong(object.getLeastSignificantBits())
			.array();
	}
}
