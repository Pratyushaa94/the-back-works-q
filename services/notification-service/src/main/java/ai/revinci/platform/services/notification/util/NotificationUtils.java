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

package ai.revinci.platform.services.notification.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.PlatformDefaults;
import ai.revinci.platform.common.enums.Token;
import ai.revinci.platform.common.util.Strings;

@Slf4j
public final class NotificationUtils {
    /**
     * Private constructor.
     */
    private NotificationUtils() {
        throw new IllegalStateException("Cannot create instances of this class");
    }

    /**
     * This method returns the blacklisted email domains as a list.
     *
     * @param blacklistedDomains A comma-separated list of blacklisted email domains.
     *
     * @return A {@link Collection} of blacklisted email domains.
     */
    public static Collection<String> getBlacklistedDomainsAsCollection(@NonNull final String blacklistedDomains) {
        if (StringUtils.isBlank(blacklistedDomains)) {
            return Set.of();
        }

        return Arrays.stream(blacklistedDomains.split(Token.COMMA.value()))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * This method attempts to check if the given email is blacklisted.
     *
     * @param blacklistedDomains A comma-separated list of blacklisted email domains.
     * @param email              Email address to check if its email domain is blacklisted.
     *
     * @return {@code true} if the email domain of the provided {@code email} is blacklisted, {@code false} otherwise.
     */
    public static boolean isEmailDomainBlacklisted(@NonNull final String blacklistedDomains,
                                                   @NonNull final String email) {
        final Collection<String> domains = NotificationUtils.getBlacklistedDomainsAsCollection(blacklistedDomains);
        return NotificationUtils.isEmailDomainBlacklisted(domains, email);
    }

    /**
     * This method attempts to check if the given email is blacklisted.
     *
     * @param blacklistedDomains A comma-separated list of blacklisted email domains.
     * @param email              Email address to check if its email domain is blacklisted.
     *
     * @return {@code true} if the email domain of the provided {@code email} is blacklisted, {@code false} otherwise.
     */
    public static boolean isEmailDomainBlacklisted(final Collection<String> blacklistedDomains,
                                                   @NonNull final String email) {
        if (CollectionUtils.isEmpty(blacklistedDomains)) {
            return false;
        }

        // If the email is blank, no point in returning false.
        if (StringUtils.isBlank(email)) {
            return true;
        }

        return blacklistedDomains.stream()
                .anyMatch(email::endsWith);
    }

    /**
     * This method attempts to remove the email addresses containing blacklisted domains.
     *
     * @param blacklistedDomains A comma-separated list of blacklisted email domains.
     * @param emails             A list of email addresses to check and remove if they contain blacklisted domains.
     *
     * @return A list of email addresses that do not contain blacklisted domains.
     */
    public static List<String> removeBlacklistedEmailDomains(@NonNull final String blacklistedDomains,
                                                             final String[] emails) {
        final Collection<String> domains = NotificationUtils.getBlacklistedDomainsAsCollection(blacklistedDomains);
        return NotificationUtils.removeBlacklistedEmailDomains(domains, emails);
    }

    /**
     * This method attempts to remove the email addresses containing blacklisted domains.
     *
     * @param blacklistedDomains A collection of blacklisted email domains.
     * @param emails             A list of email addresses to check and remove if they contain blacklisted domains.
     *
     * @return A list of email addresses that do not contain blacklisted domains.
     */
    public static List<String> removeBlacklistedEmailDomains(final Collection<String> blacklistedDomains,
                                                             final String[] emails) {
        if (Objects.isNull(emails) || emails.length == 0) {
            return List.of();
        }

        if (CollectionUtils.isEmpty(blacklistedDomains)) {
            return Stream.of(emails)
                    .filter(StringUtils::isNotBlank)
                    .toList();
        }

        return Stream.of(emails)
                .filter(email -> !NotificationUtils.isEmailDomainBlacklisted(blacklistedDomains, email))
                .toList();
    }

    /**
     * This method attempts to replace the email domain in the provided {@code email} with the provided replacement
     * domain in {@code replacementDomain}.
     *
     * @param email             Email address containing the email domain, which needs to be replaced with.
     * @param replacementDomain Replacement domain to replace the email domain in the provided {@code email}.
     *
     * @return A string containing the email address with the email domain replaced with the provided
     *         {@code replacementDomain}.
     */
    public static String replaceEmailDomain(@NonNull final String email, final String replacementDomain) {
        if (StringUtils.isBlank(replacementDomain) || email.endsWith(PlatformDefaults.REVINCI_AI_EMAIL_DOMAIN.value())) {
            return email;
        }

        final int atIndex = email.indexOf(Token.AT.value());
        if (atIndex == -1) {
            return email;
        }

        final String emailDomain = email.substring(atIndex + 1);
        if (StringUtils.isBlank(emailDomain)) {
            return email;
        }

        return email.replace(emailDomain, replacementDomain);
    }

    /**
     * Cleanses the given email by removing the blacklisted domains and replacing the domain with the given replacement
     * domain.
     *
     * @param email              Email addresses to cleanse.
     * @param blacklistedDomains Collection of blacklisted domains.
     * @param replacementDomain  Replacement domain.
     *
     * @return An {@link Optional} wrapping the cleansed email address. If the email domain is blacklisted, then an
     *         empty {@link Optional} is returned.
     */
    public static Optional<String> cleanseEmail(final String email, final Collection<String> blacklistedDomains,
                                                final String replacementDomain) {
        final boolean blacklisted = NotificationUtils.isEmailDomainBlacklisted(blacklistedDomains, email);
        if (blacklisted) {
            NotificationUtils.LOGGER.info("Email: {}. Ignoring as the email domain is blacklisted",
                                          Strings.maskEmail(email));
            return Optional.empty();
        }

        return Optional.of(NotificationUtils.replaceEmailDomain(email.trim(), replacementDomain));
    }

    /**
     * Cleanses the given emails by removing the blacklisted domains and replacing the domain with the given replacement
     * domain.
     *
     * @param emails             Array of email addresses to cleanse.
     * @param blacklistedDomains Collection of blacklisted domains.
     * @param replacementDomain  Replacement domain.
     *
     * @return A list of cleansed email addresses.
     */
    public static List<String> cleanseEmails(final String[] emails, final Collection<String> blacklistedDomains,
                                             final String replacementDomain) {
        final Collection<String> whitelistedEmails = NotificationUtils.removeBlacklistedEmailDomains(blacklistedDomains,
                                                                                                     emails);
        return whitelistedEmails.stream()
                .map(email -> NotificationUtils.replaceEmailDomain(email.trim(), replacementDomain))
                .toList();
    }
}
