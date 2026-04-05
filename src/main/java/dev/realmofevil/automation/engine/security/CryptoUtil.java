package dev.realmofevil.automation.engine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Single source of truth for Cryptographic operations.
 * Implements hashing, signing, defensive decoding and encoding.
 */
public final class CryptoUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CryptoUtil.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private CryptoUtil() {}

    /**
     * Generates an HMAC-SHA256 signature converted to a Hex string.
     * Useful for generating secure API signatures.
     *
     * @param secret The secret key.
     * @param data   The data to sign.
     * @return Hex-encoded signature.
     */
    public static String hmacSha256Hex(String secret, String data) {
        Objects.requireNonNull(secret, "HMAC secret must not be null");
        Objects.requireNonNull(data, "HMAC data must not be null");
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Critical failure in HMAC-SHA256 calculation.", e);
        }
    }

    /**
     * Generates a SHA-1 hash converted to a Hex string.
     *
     * @param data The input string to hash.
     * @return Hex-encoded SHA-1 hash.
     */
    public static String sha1Hex(String data) {
        Objects.requireNonNull(data, "SHA-1 data must not be null");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Critical failure: SHA-1 algorithm not available", e);
        }
    }

    /**
     * Generates an MD5 hash converted to a Hex string.
     */
    public static String md5Hex(String data) {
        Objects.requireNonNull(data, "MD5 data must not be null");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    public static String decodeBase64(String input) {
        return decodeBase64(input, false);
    }

    /**
     * Decodes Base64 with an optional graceful fallback.
     * 
     * @param input             The string to decode.
     * @param fallbackIfInvalid If true, returns original string on failure. Used for dev/local overrides.
     * @return Decoded plain text or original input.
     */
    public static String decodeBase64(String input, boolean fallbackIfInvalid) {
        if (input == null || input.isBlank())
            return input;

        try {
            return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            if (fallbackIfInvalid) {
                LOG.warn("Input is not valid Base64; returning raw value. (Value: {}...)",
                        input.substring(0, Math.min(input.length(), 4)));
                return input;
            }
            throw new SecurityException("Base64 decoding failed and fallback is disabled.", e);
        }
    }

    /**
     * Encodes a string to Base64 (UTF-8).
     *
     * @param plainText The plain text.
     * @return Base64 encoded string.
     */
    public static String encodeBase64(String plainText) {
        if (plainText == null || plainText.isEmpty())
            return plainText;
        return Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts a Base64 encoded payload using AES/CBC/NoPadding.
     *
     * @param encryptedBase64 The Base64 encoded ciphertext.
     * @param secretKey       The AES secret key (must match required byte length).
     * @param iv              The Initialization Vector (must be exactly 16 bytes).
     * @return The decrypted plain text string.
     */
    public static String decryptAesCbc(String encryptedBase64, String secretKey, String iv) {
        if (encryptedBase64 == null || encryptedBase64.isBlank())
            return encryptedBase64;

        try {
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decodedCiphertext = Base64.getDecoder().decode(encryptedBase64);
            byte[] rawPlaintext = cipher.doFinal(decodedCiphertext);

            String result = new String(rawPlaintext, StandardCharsets.UTF_8);
            return result.trim();

        } catch (GeneralSecurityException e) {
            throw new RuntimeException("AES Decryption failed: Cryptographic configuration error", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("AES Decryption failed: Invalid Base64 input", e);
        }
    }
}