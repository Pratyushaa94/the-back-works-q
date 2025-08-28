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

package ai.revinci.platform.data.jpa.utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Token;

@Slf4j
public final class PageUtils {
    /** Default page number. */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    /** Default page number. */
    public static final String REQUEST_PARAM_DEFAULT_PAGE_NUMBER = String.valueOf(PageUtils.DEFAULT_PAGE_NUMBER);

    /** Default page size i.e. number of records to return per page. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Default batch size that will enable hibernate to load data in batches. */
    public static final int DEFAULT_BATCH_SIZE = 30;

    /** Default page size i.e. number of records to return per page. */
    public static final String REQUEST_PARAM_DEFAULT_PAGE_SIZE = String.valueOf(PageUtils.DEFAULT_PAGE_SIZE);

    /** Maximum page size i.e. maximum number of records to return per page. */
    public static final int MAX_PAGE_SIZE = 100;

    /** Default maximum page size i.e. maximum number of records to return per page. */
    public static final String REQUEST_PARAM_MAX_PAGE_SIZE = String.valueOf(PageUtils.MAX_PAGE_SIZE);

    /**
     * Private constructor.
     */
    private PageUtils() {
        // Prevent creation of an instance of this class.
        throw new IllegalStateException("Cannot create an instance of this class");
    }

    /**
     * Creates and returns an empty page preserving the pagination settings that was provided.
     *
     * @param pageRequest Page request object containing the pagination settings.
     *
     * @return Page object of type {@link Page}.
     */
    public static <X> Page<X> emptyPage(final Pageable pageRequest) {
        return PageUtils.createPage(Collections.emptyList(), pageRequest, 0);
    }

    /**
     * Creates and returns a page of records using the provided data.
     *
     * @param records      Collection of records that are wrapped in the {@link Page} object.
     * @param pageRequest  Page request object containing the pagination settings.
     * @param totalRecords Total number of records.
     *
     * @return Page object of type {@link Page}.
     */
    public static <X> Page<X> createPage(final List<X> records, final Pageable pageRequest, final long totalRecords) {
        return new PageImpl<>(records, pageRequest, totalRecords);
    }

    /**
     * This method attempts to create a pagination configuration object of type {@link Pageable}.
     *
     * @param pageNumber Page number and defaulted to {@code PageUtils.DEFAULT_PAGE_NUMBER} if the value is invalid.
     * @param pageSize   Page size and defaulted to {@code PageUtils.DEFAULT_PAGE_SIZE} if the value is invalid.
     *
     * @return Pagination configuration instance of type {@link Pageable}.
     */
    public static Pageable createPaginationConfiguration(final int pageNumber, final int pageSize) {
        final int pageNumberToUse = pageNumber < 0 ?
                PageUtils.defaultPageNumber() :
                pageNumber;

        final int pageSizeToUse = pageSize <= 0 || pageSize > PageUtils.maximumPageSize() ?
                PageUtils.defaultPageSize() :
                pageSize;

        return PageRequest.of(pageNumberToUse, pageSizeToUse);
    }

    /**
     * This method attempts to create a pagination configuration object of type {@link Pageable}.
     *
     * @param pageNumber Page number and defaulted to {@code PageUtils.DEFAULT_PAGE_NUMBER} if the value is invalid.
     * @param pageSize   Page size and defaulted to {@code PageUtils.DEFAULT_PAGE_SIZE} if the value is invalid.
     * @param sortBy     Comma separated list of property names on which the data has to be sorted.
     * @param direction  Direction of the sorting (i.e. ASC or DESC).
     *
     * @return Pagination configuration instance of type {@link Pageable}.
     */
    public static Pageable createPaginationConfiguration(final int pageNumber, final int pageSize, final String sortBy,
                                                         final String direction) {
        final int pageNumberToUse = pageNumber < 0 ?
                PageUtils.defaultPageNumber() :
                pageNumber;

        final int pageSizeToUse = pageSize <= 0 || pageSize > PageUtils.maximumPageSize() ?
                PageUtils.defaultPageSize() :
                pageSize;

        Sort sort = null;
        if (StringUtils.isNotBlank(sortBy)) {
            final Sort.Direction sortDirection = StringUtils.isBlank(direction) ?
                    Sort.Direction.ASC :
                    Sort.Direction.fromString(direction);

            final String[] sortProperties = sortBy.trim()
                    .split(Token.COMMA.value());
            sort = Sort.by(sortDirection, Stream.of(sortProperties)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toArray(String[]::new));
        }

        return Objects.isNull(sort) ?
                PageRequest.of(pageNumberToUse, pageSizeToUse) :
                PageRequest.of(pageNumberToUse, pageSizeToUse, sort);
    }

    /**
     * This method attempts to validate the provided page definition. If the validation passes, the same page definition
     * is returned else a new page definition with the default settings is returned i.e. page-number is defaulted to
     * {@code PageUtils.DEFAULT_PAGE_NUMBER} and page-size to {@code PageUtils.DEFAULT_PAGE_SIZE}.
     *
     * @param page Page definition of type {@link Pageable} that needs to be validated.
     *
     * @return Validated page definition instance of type {@link Pageable}.
     */
    public static Pageable validateAndUpdatePaginationConfiguration(final Pageable page) {
        if (Objects.isNull(page)) {
            PageUtils.LOGGER.warn("Provided page settings is null. Using the defaults.");
            return PageRequest.of(PageUtils.defaultPageNumber(), PageUtils.defaultPageSize());
        } else if (page.getPageNumber() < 0 || page.getPageSize() <= 0 || page.getPageSize() > PageUtils.maximumPageSize()) {
            PageUtils.LOGGER.warn("Invalid page settings provided. Using the defaults.");
            return PageRequest.of(PageUtils.defaultPageNumber(), PageUtils.defaultPageSize(), page.getSort());
        }
        return page;
    }

    /**
     * This method attempts to calculate the number of pages to iterate over the provided total records using the
     * default page size ({@code PageUtils.DEFAULT_PAGE_SIZE}).
     *
     * @param totalRecords Total number of records.
     *
     * @return Number of pages.
     */
    public static int pageCount(final long totalRecords) {
        return PageUtils.pageCount(totalRecords, PageUtils.DEFAULT_PAGE_SIZE);
    }

    /**
     * This method attempts to calculate the number of pages to iterate over the provided total records using the
     * provided page size ({@code pageSize}).
     *
     * @param totalRecords Total number of records.
     * @param pageSize     Page size.
     *
     * @return Number of pages.
     */
    public static int pageCount(final long totalRecords, final int pageSize) {
        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    /**
     * This method attempts to calculate the offset for the provided page number using the default page size
     * ({@code PageUtils.DEFAULT_PAGE_SIZE}).
     *
     * @param pageNumber Page number.
     *
     * @return Offset.
     */
    public static int offsetForPage(final int pageNumber) {
        return PageUtils.offsetForPage(pageNumber, PageUtils.DEFAULT_PAGE_SIZE);
    }

    /**
     * This method attempts to calculate the offset for the provided page number using the provided page size
     * ({@code pageSize}).
     *
     * @param pageNumber Page number, which starts from 0.
     *
     * @return Offset.
     */
    public static int offsetForPage(final int pageNumber, final int pageSize) {
        return pageNumber * pageSize;
    }

    /**
     * This method returns the default page number ({@code PageUtils.DEFAULT_PAGE_NUMBER}) and is used in situations
     * where the pagination settings are invalid.
     *
     * @return Default page number ({@code PageUtils.DEFAULT_PAGE_NUMBER}).
     */
    public static int defaultPageNumber() {
        return PageUtils.DEFAULT_PAGE_NUMBER;
    }

    /**
     * This method returns the default page size ({@code PageUtils.DEFAULT_PAGE_SIZE}) and is used in situations where
     * the pagination settings are invalid.
     *
     * @return Default page size ({@code PageUtils.DEFAULT_PAGE_SIZE}).
     */
    public static int defaultPageSize() {
        return PageUtils.DEFAULT_PAGE_SIZE;
    }

    /**
     * This method returns the permitted maximum page size({@code PageUtils.MAX_PAGE_SIZE}) and is used in situations
     * where the pagination settings are invalid.
     *
     * @return Maximum page size ({@code PageUtils.MAX_PAGE_SIZE}).
     */
    public static int maximumPageSize() {
        return PageUtils.MAX_PAGE_SIZE;
    }
}

