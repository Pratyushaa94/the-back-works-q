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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.DateFormat;

@Slf4j
public final class DateUtils {
    public static final String YYYY_MMM_DD = "yyyy-MMM-dd";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DD_MMM_YYYY = "dd-MMM-yyyy";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD);
    private DateUtils() {
        throw new IllegalStateException("Cannot create instances of this class");
    }
    public static ZonedDateTime epochToZonedDateTime(final long epoch) {
        return DateUtils.epochToZonedDateTime(epoch, ZoneId.systemDefault());
    }
    public static Long getCurrentEpoch() {
        return ZonedDateTime.now(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    public static ZonedDateTime epochToZonedDateTime(final long epoch, final ZoneId zoneId) {
        return Instant.ofEpochSecond(epoch / 1000)
                .atZone(Optional.ofNullable(zoneId)
                                .orElse(ZoneId.systemDefault()));
    }
    public static String convertEpochToDateTimeFormat(final Long epochTime, final String dateFormat) {
        final LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochTime),
                                                               LocaleContextHolder.getTimeZone()
                                                                       .toZoneId());
        return dateTime.format(DateTimeFormatter.ofPattern(StringUtils.isNotBlank(dateFormat) ?
                                                                   dateFormat :
                DateFormat.ISO_DATE_TIME.getFormat()));
    }
    public static String currentDay() {
        return LocalDate.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(DateUtils.DD_MMM_YYYY));
    }

    public static int currentHour() {
        return LocalDateTime.now(ZoneOffset.UTC)
                .getHour();
    }
    public static long addDaysToEpoch(final long epochMillis, final Integer days) {
        if (days == null) {
            return epochMillis;
        }
        return Instant.ofEpochMilli(epochMillis)
                .plus(days, ChronoUnit.DAYS)
                .toEpochMilli();
    }
    public static boolean isAfterCurrentTime(final long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
                .isAfter(ZonedDateTime.now(ZoneId.systemDefault())
                                 .toInstant());
    }
    public static boolean isBeforeCurrentTime(final long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
                .isBefore(ZonedDateTime.now(ZoneId.systemDefault())
                                  .toInstant());
    }
    public static boolean isStartDateGreaterThanEndDate(final Long startDate, final Long endDate) {
        return Objects.nonNull(startDate) && Objects.nonNull(endDate) && (startDate < endDate);
    }
    public static void validateDateParamsOrThrow(final ChronoUnit unit, final Integer interval, final Long startDate,
                                                 final Long endDate) {
        if ((interval != null && unit == null) || (interval == null && unit != null)) {
            throw new IllegalArgumentException("Provide both interval and unit or neither.");
        }
        if (startDate == null ^ endDate == null) {
            throw new IllegalArgumentException("Provide both startDate and endDate or neither.");
        }
    }

    public static String formatDate(final LocalDate date, final String pattern) {
        if (date == null) {
            return null;
        }
        final String finalPattern = StringUtils.isBlank(pattern) ?
                DateUtils.DD_MMM_YYYY :
                pattern;
        return date.format(DateTimeFormatter.ofPattern(finalPattern));
    }
    public static String formatDate(final Date date, final String pattern) {
        if (date == null) {
            return null;
        }
        final String finalPattern = StringUtils.isBlank(pattern) ?
                DateUtils.DD_MMM_YYYY :
                pattern;
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(finalPattern));
    }
    public static int getHourFromDate(final Date date) {
        if (date == null) {
            return 0;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
                .getHour();
    }
}
