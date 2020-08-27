package com.jeffdisher.thinktank.chat.support;


public interface ICodec <T> {
	T deserialize(byte[] bytes);
	byte[] serialize(T object);
}
