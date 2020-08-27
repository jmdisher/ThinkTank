package com.jeffdisher.thinktank.chat.support;

import java.nio.charset.StandardCharsets;


public class StringCodec implements ICodec<String> {
	@Override
	public String deserialize(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public byte[] serialize(String object) {
		return object.getBytes(StandardCharsets.UTF_8);
	}
}
