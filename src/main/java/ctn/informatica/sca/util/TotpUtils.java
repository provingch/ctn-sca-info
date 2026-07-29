package ctn.informatica.sca.util;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpUtils {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int DEFAULT_SECRET_BYTES = 20;
    private static final int DEFAULT_DIGITS = 6;
    private static final int DEFAULT_PERIOD_SECONDS = 30;
    private static final int DEFAULT_WINDOW = 2;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpUtils() {
    }

    public static String generateSecret() {
        return generateSecret(DEFAULT_SECRET_BYTES);
    }

    public static String generateSecret(int numBytes) {
        byte[] bytes = new byte[numBytes];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public static String getOtpAuthUrl(String issuer, String accountName, String secret) {
        String encodedIssuer = urlEncode(issuer);
        String encodedAccount = urlEncode(accountName);
        String encodedSecret = urlEncode(secret);
        return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
                + "?secret=" + encodedSecret
                + "&issuer=" + encodedIssuer
                + "&algorithm=SHA1"
                + "&digits=" + DEFAULT_DIGITS
                + "&period=" + DEFAULT_PERIOD_SECONDS;
    }

    public static boolean verifyCode(String base32Secret, String code) {
        return verifyCode(base32Secret, code, DEFAULT_WINDOW);
    }

    public static boolean verifyCode(String base32Secret, String code, int window) {
        if (base32Secret == null || base32Secret.isBlank() || code == null || code.isBlank()) {
            return false;
        }
        byte[] key = base32Decode(base32Secret);
        if (key == null || key.length == 0) {
            return false;
        }
        long currentStep = Instant.now().getEpochSecond() / DEFAULT_PERIOD_SECONDS;
        for (long delta = -window; delta <= window; delta++) {
            String candidate = generateTotpCode(key, currentStep + delta, DEFAULT_DIGITS);
            if (candidate.equals(code.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String generateTotpCode(String base32Secret) {
        if (base32Secret == null || base32Secret.isBlank()) {
            throw new IllegalArgumentException("Secret must not be blank");
        }
        byte[] key = base32Decode(base32Secret);
        long currentStep = Instant.now().getEpochSecond() / DEFAULT_PERIOD_SECONDS;
        return generateTotpCode(key, currentStep, DEFAULT_DIGITS);
    }

    static String generateTotpCode(byte[] key, long counter, int digits) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(counter);
            byte[] hash = mac.doFinal(buffer.array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, digits);
            return String.format("%0" + digits + "d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to generate TOTP code", ex);
        }
    }

    private static String base32Encode(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        int index = 0;
        int digit = 0;
        int currByte;
        int nextByte;
        for (int i = 0; i < bytes.length; ) {
            currByte = bytes[i] & 0xFF;
            if (index > 3) {
                if (i + 1 < bytes.length) {
                    nextByte = bytes[i + 1] & 0xFF;
                } else {
                    nextByte = 0;
                }
                digit = currByte & (0xFF >> index);
                index = (index + 5) % 8;
                digit <<= index;
                digit |= nextByte >> (8 - index);
                i++;
            } else {
                digit = (currByte >> (8 - (index + 5))) & 0x1F;
                index = (index + 5) % 8;
                if (index == 0) {
                    i++;
                }
            }
            builder.append(BASE32_CHARS.charAt(digit));
        }
        return builder.toString();
    }

    static byte[] base32Decode(String base32) {
        if (base32 == null) {
            return new byte[0];
        }
        String normalized = base32.trim().replaceAll("[=\s]", "").toUpperCase();
        int numBytes = normalized.length() * 5 / 8;
        byte[] result = new byte[numBytes];

        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;
        for (char c : normalized.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) {
                return new byte[0];
            }
            buffer <<= 5;
            buffer |= val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }

    private static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
