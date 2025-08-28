/*
 *  Copyright (c) 2025 Revinci AI.
 *
 *  All rights reserved. This software is proprietary to and embodies the
 *  confidential technology of Revinci AI. Possession,
 *  use, duplication, or dissemination of the software and media is
 *  authorized only pursuant to a valid written license from Revinci AI.
 *
 *  Unauthorized use of this software is strictly prohibited.
 *
 *  THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL REVINCI AI BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 *  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author
 *
 */

package ai.revinci.platform.common.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;

@Slf4j
public final class Strings {


    /** Email masking template. */
    private static final String MASK_EMAIL_ADDRESS = "{0}@xxxx.xxx";

    /** Basic authentication format. */
    private static final String BASIC_AUTHENTICATION_FORMAT = "{0}:{1}";
    /** Secret key. */
    private static final byte[] SECRET = "McynrfdXkd9gobX246datmckdTqykFH27ZGXuexibjsF9hQSbFhWor6wuVhDE".getBytes(
            StandardCharsets.UTF_8);
    /** Encryption algorithm to  be used. */
    private static final byte[] ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding".getBytes(StandardCharsets.UTF_8);
    private static MessageDigest messageDigest;


    static {
        try {
            Strings.messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (final NoSuchAlgorithmException e) {
            Strings.LOGGER.error("Requested algorithm not available. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.ALGORITHM_NOT_FOUND, e.getMessage());
        }
    }

    private Strings() {
        // Do not allow creation of objects of this utility class
        throw new IllegalStateException("Cannot create instances of this class");
    }

    public static boolean same(final String input1, final String input2) {
        return Strings.same(input1, input2, true);
    }

    public static boolean same(final String input1, final String input2, final boolean ignoreCase) {
        // Both are null, we return true
        if (StringUtils.isBlank(input1) && StringUtils.isBlank(input2)) {
            return true;
        }

        // Both are non-null, check based on ignoreCase parameter.
        if (StringUtils.isNoneBlank(input1, input2)) {
            return ignoreCase ?
                    input1.equalsIgnoreCase(input2) :
                    input1.equals(input2);
        }

        return false;
    }


    public static boolean isValidUUID(final String input) {
        try {
            UUID.fromString(input);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    public static String getFullName(final String firstname, final String lastname) {
        if (StringUtils.isNoneBlank(firstname, lastname)) {
            return String.format("%s %s", firstname, lastname);
        } else if (StringUtils.isNotBlank(firstname)) {
            return firstname.trim();
        } else if (StringUtils.isNotBlank(lastname)) {
            return lastname.trim();
        }

        return StringUtils.EMPTY;
    }

    public static synchronized String decrypt(final String input, final byte[] secret) {
        // Credits go to the author of the below article:
        // https://howtodoinjava.com/security/java-aes-encryption-example/

        try {
            final Cipher cipher = Cipher.getInstance(new String(Strings.ENCRYPTION_ALGORITHM));
            cipher.init(Cipher.DECRYPT_MODE, Strings.generateSecretKeySpec(secret), new IvParameterSpec(new byte[16]));
            return new String(cipher.doFinal(Base64.getDecoder()
                                                     .decode(input)));
        } catch (final NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException |
                       IllegalBlockSizeException | InvalidAlgorithmParameterException ex) {
            Strings.LOGGER.error("Failed to decrypt. Error message : {}", ex.getMessage(), ex);
            throw ServiceException.of(CommonErrors.DECRYPTION_FAILED, ex.getMessage());
        }
    }

    private static SecretKeySpec generateSecretKeySpec(final byte[] secret) {
        return new SecretKeySpec(Arrays.copyOf(Strings.messageDigest.digest(secret), 16), "AES");
    }

    public static synchronized String encrypt(final String input, final byte[] secret) {
        // Credits go to the author of the below article:
        // https://howtodoinjava.com/security/java-aes-encryption-example/

        try {
            final Cipher cipher = Cipher.getInstance(new String(Strings.ENCRYPTION_ALGORITHM));
            cipher.init(Cipher.ENCRYPT_MODE, Strings.generateSecretKeySpec(secret), new IvParameterSpec(new byte[16]));
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException |
                       IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            Strings.LOGGER.error("Failed to encrypt. Error message : {}", e.getMessage(), e);
            throw ServiceException.of(CommonErrors.ENCRYPTION_FAILED, e.getMessage());
        }
    }

    public static String maskEmail(final String email) {
        final int index = StringUtils.isBlank(email) ?
                -1 :
                email.indexOf(Token.AT.value());
        if (index <= 0) {
            return email;
        }

        return MessageFormat.format(Strings.MASK_EMAIL_ADDRESS, email.substring(0, index));
    }

    public static synchronized String encryptUsingSalt(final String input, final String salt) {
        return Strings.encrypt(input, Strings.generateSecret(salt));
    }

    public static synchronized byte[] generateSecret(final String value) {
        if (StringUtils.isBlank(value)) {
            return new byte[0];
        }

        // Generate the hash
        final byte[] hashBytes = Strings.messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));

        // Convert the hash to a Base64 string
        return Base64.getEncoder()
                .encodeToString(hashBytes)
                .getBytes(StandardCharsets.UTF_8);
    }
    public static synchronized String generateSaltOrThrow(@NonNull final UUID tenantId, @NonNull final String realmName)
            throws NoSuchAlgorithmException {
        // Concatenate tenantId and realmName
        final String input = tenantId.toString()
                .concat(realmName);

        // Create a SHA-512 digest
        final byte[] hash = Strings.messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));

        // Convert the byte array into a hexadecimal string
        final StringBuilder hexString = new StringBuilder();
        for (final byte b : hash) {
            final String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static synchronized String generateSalt(@NonNull final UUID tenantId, @NonNull final String realmName) {
        try {
            return Strings.generateSaltOrThrow(tenantId, realmName);
        } catch (final NoSuchAlgorithmException e) {
            Strings.LOGGER.error("Tenant: {}, Realm: {}. Salt generation failed: {}", tenantId, realmName,
                                 e.getMessage(), e);
            throw ServiceException.of(CommonErrors.SALT_GENERATION_FAILED, e.getMessage());
        }
    }

    public static synchronized String decryptUsingSalt(final String input, final String salt) {
        return Strings.decrypt(input, Strings.generateSecret(salt));
    }

}
