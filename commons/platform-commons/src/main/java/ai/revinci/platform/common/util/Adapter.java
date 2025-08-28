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
package ai.revinci.platform.common.util;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class that provides functionality to adapt/convert objects from one type to another.
 *
 * @author Subbu
 */
@Slf4j
public final class Adapter {
    /**
     * Private constructor.
     */
    private Adapter() {
        // Throw illegal if anyone creates
        throw new IllegalStateException("Cannot create instances of this class");
    }

    /**
     * This method attempts to adapt the provided object (via {@code input} parameter) to the provided target type (via
     * {@code targetType} parameter).
     * <p>
     * If the object can be adapted, it will be cast to the target type and returns it else returns a null.
     *
     * @param input      Input object that needs to be adapted to the target type
     * @param targetType Target type
     * @param <T>        Target type
     *
     * @return Input object adapted to the target type. Returns null if the input object cannot be adapted to the target
     *         type
     */
    public static <T> T adapt(final Object input, final Class<T> targetType) {
        if (Objects.nonNull(input) && (targetType.isAssignableFrom(input.getClass()) || targetType.isInstance(input))) {
            return targetType.cast(input);
        }

        Adapter.LOGGER.warn("Unable to adapt the provided input to target type {}.", targetType.getName());
        return null;
    }
}