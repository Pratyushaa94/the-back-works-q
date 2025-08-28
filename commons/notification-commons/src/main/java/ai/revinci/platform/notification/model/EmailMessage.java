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

package ai.revinci.platform.notification.model;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;

@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmailMessage {
    /** From email address */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String from;

    /** List of To recipients */
    @ToString.Include
    @EqualsAndHashCode.Include
    @Singular("to")
    private Set<String> to;

    /** List of Cc recipients */
    @ToString.Include
    @EqualsAndHashCode.Include
    @Singular("cc")
    private Set<String> cc;

    /** List of Bcc recipients */
    @ToString.Include
    @EqualsAndHashCode.Include
    @Singular("bcc")
    private Set<String> bcc;

    /** Email subject */
    @ToString.Include
    @EqualsAndHashCode.Include
    private String subject;

    /** Email body */
    private String body;

    /** Boolean indicating if the message needs to be sent as html or plain-text */
    @Builder.Default
    private boolean isHtml = true;

    /** Email attachments */
    @Singular
    private Map<String, File> attachments;

    @JsonIgnore
    public boolean hasBody() {
        return StringUtils.isNotBlank(body);
    }

    @JsonIgnore
    public boolean hasAttachments() {
        return Objects.nonNull(attachments) && !attachments.isEmpty();
    }

    @JsonIgnore
    public boolean hasToRecipients() {
        return Objects.nonNull(to) && !to.isEmpty();
    }

    @JsonIgnore
    public boolean hasCcRecipients() {
        return Objects.nonNull(cc) && !cc.isEmpty();
    }

    @JsonIgnore
    public boolean hasBccRecipients() {
        return Objects.nonNull(bcc) && !bcc.isEmpty();
    }
}
