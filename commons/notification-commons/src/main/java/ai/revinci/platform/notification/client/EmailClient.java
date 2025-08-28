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

package ai.revinci.platform.notification.client;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.notification.model.EmailMessage;
import ai.revinci.platform.notification.parser.ITemplateContentParser;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailClient implements IEmailClient {
    /** Template content parser. */
    private final ITemplateContentParser templateContentParser;

    /** Java Mail Sender. */
    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(final EmailMessage emailMessage) {
        final MimeMessage message = createMimeMessage(emailMessage);
        if (Objects.isNull(message)) {
            EmailClient.LOGGER.warn("Message is null. Skipping the process of sending email.");
            return;
        }
        mailSender.send(message);
    }

    @Override
    public void sendEmail(final EmailMessage emailMessage, final String templateName) {
        sendEmail(emailMessage, templateName, null);
    }

    @Override
    public void sendEmail(final EmailMessage emailMessage, final String templateName,
                          final Map<String, Object> placeholders) {
        final String trimmedTemplate = StringUtils.isBlank(templateName) ?
                null :
                templateName.trim();
        if (StringUtils.isBlank(trimmedTemplate)) {
            // Do we have any content in the body of the message?
            if (!emailMessage.hasBody()) {
                EmailClient.LOGGER.info("No content in the body of the email message. Not sending the email.");
                return;
            }
            EmailClient.LOGGER.info("No template was provided. Attempting to send as plain text message.");
            emailMessage.setHtml(false);
        } else {
            // Template specified. Need to send as html content. Parse the template which becomes the body of the email
            emailMessage.setBody(templateContentParser.parse(trimmedTemplate, placeholders));
            emailMessage.setHtml(true);
        }

        sendEmail(emailMessage);
    }

    /**
     * This method creates a mime-message (of type {@link MimeMessage}) using the details from the {@link EmailMessage}
     * that is provided as a parameter to this function.
     *
     * @param emailMessage Email message that holds the data.
     *
     * @return Mime message of type {@link MimeMessage}.
     */
    private MimeMessage createMimeMessage(final EmailMessage emailMessage) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            final MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage, true);
            messageHelper.setFrom(emailMessage.getFrom());
            if (emailMessage.hasToRecipients()) {
                messageHelper.setTo(toInternetAddresses(emailMessage.getTo()));
            }
            if (emailMessage.hasCcRecipients()) {
                messageHelper.setCc(toInternetAddresses(emailMessage.getCc()));
            }
            if (emailMessage.hasBccRecipients()) {
                messageHelper.setBcc(toInternetAddresses(emailMessage.getBcc()));
            }
            messageHelper.setSubject(emailMessage.getSubject());
            messageHelper.setText(emailMessage.getBody(), emailMessage.isHtml());
            // Support attachments
            if (emailMessage.hasAttachments()) {
                for (final Map.Entry<String, File> attachmentEntry : emailMessage.getAttachments()
                        .entrySet()) {
                    messageHelper.addAttachment(attachmentEntry.getKey(), attachmentEntry.getValue());
                }
            }
        } catch (final MessagingException e) {
            EmailClient.LOGGER.error(e.getMessage(), e);
            mimeMessage = null;
        }
        return mimeMessage;
    }

    /**
     * The purpose of this method is to convert the provided set of email addresses (in string form) to an array of
     * internet addresses of type {@link InternetAddress}
     *
     * @param emailAddresses List of email addresses that are to be converted to {@link InternetAddress}.
     *
     * @return Array of internet addresses where every element is of type {@link InternetAddress}.
     */
    private InternetAddress[] toInternetAddresses(final Set<String> emailAddresses) {
        return emailAddresses.stream()
                .map(this::transform)
                .filter(Objects::nonNull)
                .toArray(InternetAddress[]::new);
    }

    /**
     * This method attempts to convert / transform the provided {@code email} to an instance of type
     * {@link InternetAddress}.
     *
     * @param email Email address that needs to be converted to {@link InternetAddress}.
     *
     * @return Instance of {@link InternetAddress} if the conversion was successful, null otherwise.
     */
    private InternetAddress transform(final String email) {
        InternetAddress ia = null;
        try {
            ia = new InternetAddress(email);
        } catch (final Exception ex) {
            EmailClient.LOGGER.error("Ignoring error ({}) while converting email address", ex.getMessage());
        }
        return ia;
    }
}

