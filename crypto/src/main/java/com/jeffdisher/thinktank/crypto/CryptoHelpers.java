package com.jeffdisher.thinktank.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;


/**
 * Helpers related to cryptographic signing and verification.
 * Further reading for broader context or understanding (much of this class was created by looking at these sources):
 * -Java signatures and verification:  http://tutorials.jenkov.com/java-cryptography/signature.html
 * -Standard crypto algorithm names:  https://docs.oracle.com/javase/9/docs/specs/security/standard-names.html
 * -deserializing keys:  https://exceptionshub.com/how-to-recover-a-rsa-public-key-from-a-byte-array.html
 */
public class CryptoHelpers {
	private static final String KEY_ALGORITHM = "EC";
	private static final String SIGNATURE_ALGORITHM = "SHA512withECDSA";

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final KeyFactory KEY_FACTORY;

	static {
		try {
			KEY_FACTORY = KeyFactory.getInstance(KEY_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// This is a static config error.
			throw new AssertionError(e);
		}
	}

	/**
	 * @return A randomly generated key pair in the default EC curve.
	 */
	public static KeyPair generateRandomKeyPair() {
		// We just use the default EC spec with 112-bit keys (just since we want them small).
		KeyPairGenerator keyPairGenerator;
		try {
			keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// This is a static config error.
			throw _noSuchAlgorithm(KEY_ALGORITHM, e);
		}
		keyPairGenerator.initialize(112, RANDOM);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * @param key The public key to serialize.
	 * @return The key, serialized using X509 encoding.
	 */
	public static byte[] serializePublic(PublicKey key) {
		if (null == key) {
			throw new NullPointerException();
		}
		
		return key.getEncoded();
	}

	/**
	 * @param bytes An EC public key encoded as X509.
	 * @return The public key (null if there was an error decoding).
	 */
	public static PublicKey deserializePublic(byte[] bytes) {
		if (null == bytes) {
			throw new NullPointerException();
		}
		
		PublicKey key;
		try {
			key = KEY_FACTORY.generatePublic(new X509EncodedKeySpec(bytes));
		} catch (InvalidKeySpecException e) {
			// This is an error in the key.
			key = null;
		}
		return key;
	}

	/**
	 * @param key The private key to serialize.
	 * @return The key, serialized using PKCS8 encoding.
	 */
	public static byte[] serializePrivate(PrivateKey key) {
		if (null == key) {
			throw new NullPointerException();
		}
		
		return key.getEncoded();
	}

	/**
	 * @param bytes An EC private key encoded as PKCS8.
	 * @return The private key (null if there was an error decoding).
	 */
	public static PrivateKey deserializePrivate(byte[] bytes) {
		if (null == bytes) {
			throw new NullPointerException();
		}
		
		PrivateKey key;
		try {
			key = KEY_FACTORY.generatePrivate(new PKCS8EncodedKeySpec(bytes));
		} catch (InvalidKeySpecException e) {
			// This is an error in the key.
			key = null;
		}
		return key;
	}

	/**
	 * Generates a cryptographic signature of the given message signed with the given private key.
	 * 
	 * @param key An EC private key.
	 * @param message The message to sign.
	 * @return The cryptographic signature, in ASN.1 DER encoding.
	 */
	public static byte[] sign(PrivateKey key, byte[] message) {
		if ((null == key) || (null == message)) {
			throw new NullPointerException();
		}
		
		Signature signature;
		try {
			signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// This is a static config error.
			throw _noSuchAlgorithm(SIGNATURE_ALGORITHM, e);
		}
		byte[] serialized;
		try {
			signature.initSign(key, RANDOM);
			signature.update(message);
			serialized = signature.sign();
		} catch (InvalidKeyException e) {
			// This is an error in the key.
			serialized = null;
		} catch (SignatureException e) {
			// This can't happen since we are initializing the signature right here.
			throw new AssertionError("Unexpected exception", e);
		}
		return serialized;
	}

	/**
	 * Verifies that the signature provided was generated from the given message by the private key associated with the
	 * given public key.
	 * 
	 * @param key An EC public key.
	 * @param message The message presumably signed.
	 * @param signature The signature derived from the message (encoded as ASN.1 DER).
	 * @return True if the signature was generated from this message using the private key associated with the given
	 * public key.
	 */
	public static boolean verify(PublicKey key, byte[] message, byte[] signature) {
		if ((null == key) || (null == message) || (null == signature)) {
			throw new NullPointerException();
		}
		
		Signature verify;
		try {
			verify = Signature.getInstance(SIGNATURE_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			// This is a static config error.
			throw _noSuchAlgorithm(SIGNATURE_ALGORITHM, e);
		}
		boolean verified;
		try {
			verify.initVerify(key);
			verify.update(message);
			verified = verify.verify(signature);
		} catch (InvalidKeyException e) {
			// This is an error in the key.
			verified = false;
		} catch (SignatureException e) {
			// This is how a failure appears if the signature was just corrupted.
			verified = false;
		}
		return verified;
	}


	private static RuntimeException _noSuchAlgorithm(String name, NoSuchAlgorithmException e) {
		throw new AssertionError("Missing required crypto algorith: " + name, e);
	}
}
