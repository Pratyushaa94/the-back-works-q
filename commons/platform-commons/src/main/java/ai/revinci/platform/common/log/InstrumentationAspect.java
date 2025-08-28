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
package ai.revinci.platform.common.log;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Aspect that deals with the "Logging / Instrumentation" cross-cutting concern.
 * <p>
 * This aspect provides the functionality of logging the time spent in the execution of methods that are annotated with
 * {@link Instrumentation} annotation.
 *
 * @author Subbu
 */
@Slf4j
@Aspect
@Component
public class InstrumentationAspect {
    /** Logging message format for method entry. */
    private static final String METHOD_ENTRY_LOG_MSG = "Entering method [{}], class [{}], at [{}]";

    /** Logging message format for method exit. */
    private static final String METHOD_EXIT_LOG_MSG = "Exiting method [{}], class [{}], at [{}], time-spent [{} ms]";

    /**
     * Around advice which gets triggered whenever the control enters a method that is annotated with the annotation
     * {@link Instrumentation}.
     *
     * @param proceedingJoinPoint Proceeding join point.
     *
     * @return Object which is the return data from the respective join point.
     *
     * @throws Throwable Exception that might be thrown from the actual join point.
     */
    @Around("@annotation(ai.revinci.platform.common.log.Instrumentation)")
    public Object instrumentMethodInvocation(final ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        final String className = Objects.nonNull(proceedingJoinPoint.getTarget()) ?
                                 proceedingJoinPoint.getTarget()
                                                    .getClass()
                                                    .getSimpleName() :
                                 StringUtils.EMPTY;
        final String methodName = proceedingJoinPoint.getSignature()
                                                     .getName();

        final long startTime = System.currentTimeMillis();
        InstrumentationAspect.LOGGER.debug(InstrumentationAspect.METHOD_ENTRY_LOG_MSG, methodName, className, startTime);

        // Proceed to the actual join point.
        final Object returnData = proceedingJoinPoint.proceed();

        final long endTime = System.currentTimeMillis();
        InstrumentationAspect.LOGGER.debug(InstrumentationAspect.METHOD_EXIT_LOG_MSG, methodName, className, endTime,
                                    (endTime - startTime));

        return returnData;
    }
}