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

package ai.revinci.platform.services.notification.configuration;

import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.multitenancy.annotation.EnableMultiTenancy;
import ai.revinci.platform.notification.annotation.EnableAsyncNotification;
import ai.revinci.platform.security.data.model.persistence.PermissionEntity;
import ai.revinci.platform.security.data.repository.PermissionRepository;
import ai.revinci.platform.security.util.AuthenticationUtils;
import ai.revinci.platform.services.notification.NotificationApplicationService;
import ai.revinci.platform.services.notification.configuration.properties.NotificationSettingsProperties;
import ai.revinci.platform.services.notification.data.repository.TenantRepository;
import ai.revinci.platform.services.notification.handler.NotificationMessageHandler;
import ai.revinci.platform.web.annotation.EnableWebConfiguration;

@Slf4j
@EnableAsyncNotification
@EnableMultiTenancy
@EnableWebConfiguration
@EnableJpaRepositories(basePackageClasses = {TenantRepository.class, PermissionRepository.class})
@EntityScan(basePackageClasses = {NotificationApplicationService.class, PermissionEntity.class})
@EnableConfigurationProperties(value = {NotificationSettingsProperties.class})
@Configuration
@RequiredArgsConstructor
public class NotificationServiceConfiguration {
    /** Handler implementation for handling notification messages. */
    private final NotificationMessageHandler notificationMessageHandler;


    /**
     * A {@link Consumer} which is registered as a bean that consumes the messages on the topic -
     * {environment}-rvc-platform-notification-topic.
     *
     * @return A {@link Consumer} that consumes the messages on the topic -
     *         {environment}-rvc-platform-notification-topic.
     */
    @Bean
    public Consumer<Message<String>> rvcPlatformOnNotificationEvent() {
        return notificationMessageHandler::handleNotificationEvent;
    }

    /**
     * A {@link Consumer} which is registered as a bean that consumes the messages on the topic -
     * {environment}-rvc-platform-db-provisioned-topic.
     *
     * @return A {@link Consumer} that consumes the messages on the topic -
     *         {environment}-rvc-platform-db-provisioned-topic.
     */
    @Bean
    public Consumer<Message<String>> rvcPlatformOnDbProvisionedEvent() {
        return notificationMessageHandler::handleDbProvisionedEvent;
    }
}
