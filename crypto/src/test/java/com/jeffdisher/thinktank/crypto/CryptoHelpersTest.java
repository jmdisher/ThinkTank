package com.jeffdisher.thinktank.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.Assert;
import org.junit.Test;


public class CryptoHelpersTest {
	@Test
	public void testKeySerialization() throws Throwable {
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		PublicKey publicKey = pair.getPublic();
		PrivateKey privateKey = pair.getPrivate();
		
		byte[] serializedPublic = CryptoHelpers.serializePublic(publicKey);
		PublicKey newPublic = CryptoHelpers.deserializePublic(serializedPublic);
		
		byte[] serializedPrivate = CryptoHelpers.serializePrivate(privateKey);
		PrivateKey newPrivate = CryptoHelpers.deserializePrivate(serializedPrivate);
		
		Assert.assertEquals(52, serializedPublic.length);
		Assert.assertEquals(46, serializedPrivate.length);
		Assert.assertEquals(publicKey, newPublic);
		Assert.assertEquals(privateKey, newPrivate);
	}

	@Test
	public void testSignVerify() throws Throwable {
		KeyPair pair = CryptoHelpers.generateRandomKeyPair();
		PublicKey publicKey = pair.getPublic();
		PrivateKey privateKey = pair.getPrivate();
		
		byte[] data1 = {0};
		byte[] data2 = "abcdefghijklmnopqrstuvwxyz0123456789".getBytes(StandardCharsets.UTF_8);
		
		byte[] signature1 = CryptoHelpers.sign(privateKey, data1);
		byte[] signature2 = CryptoHelpers.sign(privateKey, data2);
		
		byte[] brokenSig2 = signature2.clone();
		brokenSig2[0] += 1;
		byte[] invalidSig2 = signature2.clone();
		invalidSig2[invalidSig2.length - 1] += 1;
		
		boolean verify1 = CryptoHelpers.verify(publicKey, data1, signature1);
		boolean verify2 = CryptoHelpers.verify(publicKey, data2, signature2);
		boolean brokenVerify2 = CryptoHelpers.verify(publicKey, data2, brokenSig2);
		boolean invalidVerify2 = CryptoHelpers.verify(publicKey, data2, invalidSig2);
		
		// Signatures vary in length but are never less than 34 bytes or more than 36 bytes.
		Assert.assertTrue(signature1.length >= 34);
		Assert.assertTrue(signature1.length <= 36);
		Assert.assertTrue(signature2.length >= 34);
		Assert.assertTrue(signature2.length <= 36);
		Assert.assertTrue(verify1);
		Assert.assertTrue(verify2);
		Assert.assertFalse(brokenVerify2);
		Assert.assertFalse(invalidVerify2);
	}
}
