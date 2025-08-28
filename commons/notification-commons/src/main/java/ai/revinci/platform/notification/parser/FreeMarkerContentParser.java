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

package ai.revinci.platform.notification.parser;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreeMarkerContentParser implements ITemplateContentParser {
    private final Configuration freeMarkerConfiguration;

    @Override
    public String parse(final String templateFileName, final Map<String, Object> placeholders) {
        FreeMarkerContentParser.LOGGER.debug("Processing template {}", templateFileName);
        String parsedContent = "";
        try {
            final Template template = freeMarkerConfiguration.getTemplate(templateFileName);
            parsedContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, placeholders);
        } catch (final Exception ioe) {
            FreeMarkerContentParser.LOGGER.error("Failed to parse template {}. Error message : {}", templateFileName,
                                                 ioe.getMessage());
        }

        return parsedContent;
    }
}
