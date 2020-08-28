package com.jeffdisher.thinktank.crypto;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;


/**
 * Inspired by JWT (JSON Web Token), but far simpler and specific to ThinkTank uses.  May later be replaced by JWT but
 * there is no compelling reason to use that, at this time (it would add a dependency, requires a more complex
 * interpretation in ThinkTank code, requires more error handling/hardening around the JSON parser, and is larger).
 * This token has the following format:
 * -[0] - version byte - always 0.
 * -[1-16] - user UUID bytes in big-endian.
 * -[17-24] - expiry time in milliseconds since epoch.
 * -[25-n] - SHA512withECDSA signature in ASN.1 DER encoding (34-36 bytes long).
 * 
 * Note that the token is always serialized as a Base64 string.
 */
public class BinaryToken {
	private static final int UUID_SIZE = Long.BYTES + Long.BYTES;
	private static final byte VERSION = 0;

	public static String createToken(PrivateKey key, UUID uuid, long expiryMillis) {
		ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES + UUID_SIZE + Long.BYTES);
		buffer.put(VERSION);
		buffer.putLong(uuid.getMostSignificantBits());
		buffer.putLong(uuid.getLeastSignificantBits());
		buffer.putLong(expiryMillis);
		byte[] data = buffer.array();
		byte[] signature = CryptoHelpers.sign(key, data);
		byte[] token = new byte[data.length + signature.length];
		System.arraycopy(data, 0, token, 0, data.length);
		System.arraycopy(signature, 0, token, data.length, signature.length);
		return Base64.getEncoder().encodeToString(token);
	}

	public static UUID validateToken(PublicKey key, long nowMillis, String encodedToken) {
		byte[] raw = Base64.getDecoder().decode(encodedToken);
		byte[] data = new byte[Byte.BYTES + UUID_SIZE + Long.BYTES];
		byte[] signature = new byte[raw.length - data.length];
		System.arraycopy(raw, 0, data, 0, data.length);
		System.arraycopy(raw, data.length, signature, 0, signature.length);
		
		UUID valid = null;
		boolean isValid = CryptoHelpers.verify(key, data, signature);
		if (isValid) {
			ByteBuffer buffer = ByteBuffer.wrap(data);
			byte version = buffer.get();
			if (VERSION == version) {
				long most = buffer.getLong();
				long least = buffer.getLong();
				long expiryMillis = buffer.getLong();
				if (expiryMillis > nowMillis) {
					valid = new UUID(most, least);
				}
			}
		}
		return valid;
	}
}
