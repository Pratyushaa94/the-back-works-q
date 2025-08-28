/*
 * Copyright (c) 2025 Revinci AI.
 *
 * All rights reserved. This software is proprietary to and embodies the
 * confidential technology of Revinci AI. Possession,
 * use, duplication, or dissemination of the software and media is
 * authorized only pursuant to a valid written license from
 * Revinci AI Solutions Pvt. Ltd.
 *
 * Unauthorized use of this software is strictly prohibited.
 *
 * THIS SOFTWARE IS PROVIDED BY Revinci AI "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Revinci AI BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ai.revinci.platform.common.password;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.RepeatCharacterRegexRule;
import org.passay.Rule;

/**
 * Implementation class that provides the capability to generate password policies.
 *
 * @author Subbu
 */
public final class PasswordPolicy {
    /** Minimum password length. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** Maximum password length. */
    private static final int MAX_PASSWORD_LENGTH = 64;

    /** Minimum number of special characters. */
    private static final int MIN_SPECIAL_CHARACTERS_COUNT = 1;

    /** Minimum number of upper case characters. */
    private static final int MIN_UPPER_CASE_CHARACTERS_COUNT = 1;

    /** Minimum number of lower case characters. */
    private static final int MIN_LOWER_CASE_CHARACTERS_COUNT = 1;

    /** Minimum number of digits. */
    private static final int MIN_DIGITS_COUNT = 1;

    /** Number of days before the password expires. */
    private static final int PASSWORD_EXPIRATION_DAYS = 30;

    /** Number of previous passwords that cannot be used during password change process. */
    private static final int PASSWORD_HISTORY = 3;

    /** Random instance. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Private constructor to prevent instantiation.
     */
    private PasswordPolicy() {
        throw new IllegalStateException("Cannot create instance of this class");
    }

    /**
     * This method returns a random password length.
     *
     * @return A random password length.
     */
    public static int getRandomPasswordLength() {
        return PasswordPolicy.RANDOM.nextInt(PasswordPolicy.MIN_PASSWORD_LENGTH, PasswordPolicy.MAX_PASSWORD_LENGTH);
    }

    /**
     * This method returns the minimum password length.
     *
     * @return The minimum password length.
     */
    public static int getMinimumPasswordLength() {
        return PasswordPolicy.MIN_PASSWORD_LENGTH;
    }

    /**
     * This method returns the maximum password length.
     *
     * @return The maximum password length.
     */
    public static int getMaximumPasswordLength() {
        return PasswordPolicy.MAX_PASSWORD_LENGTH;
    }

    /**
     * This method returns the minimum number of special characters that must be present in the password.
     *
     * @return Minimum number of special characters that must be present in the password.
     */
    public static int getMinimumSpecialCharactersCount() {
        return PasswordPolicy.MIN_SPECIAL_CHARACTERS_COUNT;
    }

    /**
     * This method returns the minimum number of upper case characters that must be present in the password.
     *
     * @return Minimum number of upper case characters that must be present in the password.
     */
    public static int getMinimumUpperCaseCharactersCount() {
        return PasswordPolicy.MIN_UPPER_CASE_CHARACTERS_COUNT;
    }

    /**
     * This method returns the minimum number of lower case characters that must be present in the password.
     *
     * @return Minimum number of lower case characters that must be present in the password.
     */
    public static int getMinimumLowerCaseCharactersCount() {
        return PasswordPolicy.MIN_LOWER_CASE_CHARACTERS_COUNT;
    }

    /**
     * This method returns the minimum number of digits that must be present in the password.
     *
     * @return Minimum number of digits that must be present in the password.
     */
    public static int getMinimumDigitsCount() {
        return PasswordPolicy.MIN_DIGITS_COUNT;
    }

    /**
     * This method returns the number of days before the password expires.
     *
     * @return Number of days before the password expires.
     */
    public static int getPasswordExpirationDays() {
        return PasswordPolicy.PASSWORD_EXPIRATION_DAYS;
    }

    /**
     * This method returns the number of previous passwords that cannot be used during password change process.
     *
     * @return Number of previous passwords that cannot be used during password change process.
     */
    public static int getPasswordHistory() {
        return PasswordPolicy.PASSWORD_HISTORY;
    }

    /**
     * This method returns the default set of rules to be considered for password generation.
     *
     * @return A list of rules to be considered for password generation.
     */
    public static List<Rule> usingDefaults() {
        // Define the rules
        final CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase,
                                                              PasswordPolicy.MIN_UPPER_CASE_CHARACTERS_COUNT);
        final CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase,
                                                              PasswordPolicy.MIN_LOWER_CASE_CHARACTERS_COUNT);
        final CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit, PasswordPolicy.MIN_DIGITS_COUNT);
        final CharacterRule specialCharRule = new CharacterRule(new CharacterData() {
            public String getErrorCode() {
                return "INSUFFICIENT_SPECIAL";
            }

            public String getCharacters() {
                return "@$!%*?&";
            }
        }, PasswordPolicy.MIN_SPECIAL_CHARACTERS_COUNT);

        // Ensure the password length is at least 10 characters
        final LengthRule lengthRule = new LengthRule(PasswordPolicy.MIN_PASSWORD_LENGTH,
                                                     PasswordPolicy.MAX_PASSWORD_LENGTH);

        // Ensure no repeated characters
        final RepeatCharacterRegexRule repeatRule = new RepeatCharacterRegexRule(3);

        return Arrays.asList(upperCaseRule, lowerCaseRule, digitRule, specialCharRule, lengthRule, repeatRule);
    }
}
