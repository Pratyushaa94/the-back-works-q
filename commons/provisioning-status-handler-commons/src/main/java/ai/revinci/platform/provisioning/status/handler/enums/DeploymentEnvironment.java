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

package ai.revinci.platform.provisioning.status.handler.enums;

import java.util.List;

import lombok.AllArgsConstructor;

import ai.revinci.platform.common.enums.IEnumValueProvider;

@AllArgsConstructor
public enum DeploymentEnvironment implements IEnumValueProvider {
    LOCAL("local"),
    DEV("dev"),
    QA("qa"),
    DEMO("demo"),
    STAGING("staging"),
    PROD("prod");


    /** Environment. */
    private final String value;

    /**
     * Find the matching "lowest" deployment environment based on the active profiles.
     * <p>
     * If the active profiles contains both "local" and "dev", we will treat it as "local".
     *
     * @param activeRuntimeProfiles An array of active profiles.
     *
     * @return Matching "lowest" deployment environment.
     */
    public static DeploymentEnvironment findMatchingLowestEnvironment(final String[] activeRuntimeProfiles) {
        // Get the active profiles.
        final List<String> activeProfiles = List.of(activeRuntimeProfiles);
        if (activeProfiles.isEmpty()) {
            // If no active profiles, we will treat it as a local environment.
            return DeploymentEnvironment.LOCAL;
        }

        DeploymentEnvironment env = DeploymentEnvironment.LOCAL;
        if (activeProfiles.contains(DeploymentEnvironment.DEV.value())) {
            env = DeploymentEnvironment.DEV;
        } else if (activeProfiles.contains(DeploymentEnvironment.QA.value())) {
            env = DeploymentEnvironment.QA;
        } else if (activeProfiles.contains(DeploymentEnvironment.STAGING.value())) {
            env = DeploymentEnvironment.STAGING;
        } else if (activeProfiles.contains(DeploymentEnvironment.DEMO.value())) {
            env = DeploymentEnvironment.DEMO;
        } else if (activeProfiles.contains(DeploymentEnvironment.PROD.value())) {
            env = DeploymentEnvironment.PROD;
        }

        return env;
    }

    @Override
    public String value() {
        return value;
    }

}
