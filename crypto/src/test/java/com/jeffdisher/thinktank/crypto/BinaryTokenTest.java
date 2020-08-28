package com.jeffdisher.thinktank.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;


public class BinaryTokenTest {
	@Test
	public void testValidToken() throws Throwable {
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		PublicKey publicKey = pair.getPublic();
		PrivateKey privateKey = pair.getPrivate();
		
		UUID uuid = UUID.randomUUID();
		long now = 1_000L;
		long future = 2_000L;
		String token = BinaryToken.createToken(privateKey, uuid, future);
		UUID valid = BinaryToken.validateToken(publicKey, now, token);
		Assert.assertEquals(uuid, valid);
	}

	@Test
	public void testExpiredToken() throws Throwable {
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		PublicKey publicKey = pair.getPublic();
		PrivateKey privateKey = pair.getPrivate();
		
		UUID uuid = UUID.randomUUID();
		long now = 1_000L;
		String token = BinaryToken.createToken(privateKey, uuid, now);
		UUID valid = BinaryToken.validateToken(publicKey, now, token);
		Assert.assertNull(valid);
	}

	@Test
	public void testCorruptToken() throws Throwable {
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		PublicKey publicKey = pair.getPublic();
		
		long now = 1_000L;
		String token = "AM21RNO2I0OFqxayuGoXbfA8AAAAAAAH0DAhAg8AxBzjTxhOkGX8HStqbB8CDjG70bn2sGNgpftjwsmL";
		UUID valid = BinaryToken.validateToken(publicKey, now, token);
		Assert.assertNull(valid);
	}
	@Test
	public void testMaliciousToken() throws Throwable {
		PublicKey publicKey = CryptoHelpers.generateRandomKeyPair().getPublic();
		PrivateKey privateKey = CryptoHelpers.generateRandomKeyPair().getPrivate();
		
		UUID uuid = UUID.randomUUID();
		long now = 1_000L;
		long future = 2_000L;
		String token = BinaryToken.createToken(privateKey, uuid, future);
		UUID valid = BinaryToken.validateToken(publicKey, now, token);
		Assert.assertNull(valid);
	}
}
