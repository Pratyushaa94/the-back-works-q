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

package ai.revinci.platform.services.platform.configuration;

import java.util.function.Consumer;

import org.passay.PasswordGenerator;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.Message;

import lombok.RequiredArgsConstructor;

import ai.revinci.platform.notification.annotation.EnableAsyncNotification;
import ai.revinci.platform.security.data.model.persistence.PermissionEntity;
import ai.revinci.platform.security.data.repository.PermissionRepository;
import ai.revinci.platform.security.util.AuthenticationUtils;
import ai.revinci.platform.services.platform.PlatformService;
import ai.revinci.platform.services.platform.data.repository.TenantRepository;
import ai.revinci.platform.services.platform.handler.PlatformMessageHandler;
import ai.revinci.platform.web.annotation.EnableWebConfiguration;

@PropertySource("classpath:/l10n/ValidationMessages.properties")
@EnableAsyncNotification
@EnableWebConfiguration
@EnableJpaRepositories(basePackageClasses = {TenantRepository.class, PermissionRepository.class})
@EntityScan(basePackageClasses = {PlatformService.class, PermissionEntity.class})
@Configuration
@RequiredArgsConstructor
public class PlatformServiceConfiguration {

    private final PlatformMessageHandler platformMessageHandler;

    @Bean
    public PasswordGenerator passwordGenerator() {
        return new PasswordGenerator();
    }

    @Bean
    public Consumer<Message<String>> rvcPlatformOnRealmProvisionedEvent() {
        return platformMessageHandler::handleRealmProvisionedEvent;
    }
}
