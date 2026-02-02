package dev.realmofevil.automation.engine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

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
        if (secret == null || data == null) {
            throw new IllegalArgumentException("Secret and data must not be null for HMAC generation.");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Critical failure in HMAC-SHA256 calculation.", e);
        }
    }

    /**
     * Generates an MD5 hash converted to a Hex string.
     * Required for legacy Odin API checksums.
     */
    public static String md5Hex(String data) {
        if (data == null)
            return null;
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
        if (plainText == null)
            return null;
        return Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
    }
}