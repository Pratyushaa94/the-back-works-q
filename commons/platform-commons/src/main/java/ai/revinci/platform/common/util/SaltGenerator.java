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

import java.security.SecureRandom;
import java.util.Base64;

public final class SaltGenerator {
    /** A cryptographically secure random number generator. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** The length of the salt in bytes. */
    private static final int SALT_LENGTH = 96;

    /**
     * Private constructor.
     */
    private SaltGenerator() {
        // Prevent instantiation
        throw new IllegalStateException("Cannot create instances of this class");
    }

    /**
     * Generates a random salt of length {@value SALT_LENGTH} bytes, and encodes it in Base64.
     *
     * @return A random salt encoded in Base64
     */
    public static String generateSalt() {
        final byte[] salt = new byte[SALT_LENGTH];
        // Generate a random salt
        SaltGenerator.RANDOM.nextBytes(salt);

        // Encode the salt in Base64
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(salt);
    }
}
