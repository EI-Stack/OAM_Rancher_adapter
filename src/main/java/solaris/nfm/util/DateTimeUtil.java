package solaris.nfm.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class DateTimeUtil
{
	/**
	 * 將 millisecond (Long) [隱含的時區為 UTC] 轉成 LocalDateTime
	 */
	public static LocalDateTime millsToLocalDateTime(final long millis)
	{
		final Instant instant = Instant.ofEpochMilli(millis);
		return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	/**
	 * 將 LocalDateTime 轉成 millisecond (Long) [隱含的時區為 UTC]
	 */
	public static Long LocalDateTimeToMills(final LocalDateTime localDateTime)
	{
		final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
		return zonedDateTime.toInstant().toEpochMilli();
	}

	/**
	 * 將 millisecond (Long) [隱含的時區為 UTC] 轉成 LocalDateTime [隱含的時區為 UTC]
	 */
	public static LocalDateTime castMillsToUtcLocalDateTime(final long millis)
	{
		final Instant instant = Instant.ofEpochMilli(millis);
		return instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
	}

	/**
	 * 將 millisecond (Long) [隱含的時區為 UTC] 轉成 ZonedDateTime [時區為 UTC]
	 */
	public static ZonedDateTime castMillsToUtcZonedDateTime(final long millis)
	{
		final Instant instant = Instant.ofEpochMilli(millis);
		return instant.atZone(ZoneId.of("UTC"));
	}

	/**
	 * Cast ISO string (2016-12-27T08:15:05.674+01:00) to UTC localDateTime
	 */
	public static LocalDateTime castIsoToUtcLocalDateTime(final String isoString)
	{
		final ZonedDateTime zdtUtc = castIsoToUtcZonedDateTime(isoString);
		// 將時區去除，回傳 LocalDateTime
		return zdtUtc.toLocalDateTime();
	}

	/**
	 * Cast ISO string (2016-12-27T08:15:05.674+01:00) to ZonedDateTime
	 */
	public static ZonedDateTime castIsoToUtcZonedDateTime(final String isoString)
	{
		// 取得附有時區的時間
		final ZonedDateTime zdt = ZonedDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		// 將時區轉換成 UTC
		final ZonedDateTime zdtUtc = zdt.withZoneSameInstant(ZoneId.of("UTC"));

		return zdtUtc;
	}

	/**
	 * Cast LocalDateTime to ISO string (2016-12-27T08:15:05.674)
	 */
	public static String localDateTimeToIsoString(final LocalDateTime localDateTime)
	{
		return localDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
	}

	/**
	 * Cast LocalDateTime to zoned ISO string (2016-12-27T08:15:05.674+01:00)
	 */
	public static String castLocalDateTimeToZonedIsoString(final LocalDateTime localDateTime)
	{
		final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
		return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	/**
	 * Cast LocalDateTime to formated string (2016-12-27 08:15:05)
	 */
	public static String castLocalDateTimeToString(final LocalDateTime localDateTime)
	{
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		return localDateTime.format(formatter);
	}

	/**
	 * Cast ZonedDateTime to formated string (2016-12-27 08:15:05+0800)
	 */
	public static String castZonedDateTimeToString(final ZonedDateTime zonedDateTime)
	{
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");
		return zonedDateTime.format(formatter);
	}

	/**
	 * Covert ISO string (2016-12-27T08:15:05.674) to millisecond (Long)
	 */
	public static Long isoStringToMills(final String isoString)
	{
		final LocalDateTime dateTime = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME);
		return LocalDateTimeToMills(dateTime);
	}

	/**
	 * Cast ISO string (2016-12-27T08:15:05.674+01:00) to formated string (2016-12-27 08:15:05)
	 */
	public static String castIsoToUtcString(final String isoString)
	{
		final LocalDateTime utcTime = DateTimeUtil.castIsoToUtcLocalDateTime(isoString);
		final String utcTimeString = DateTimeUtil.castLocalDateTimeToString(utcTime);

		return utcTimeString;
	}

	public static void sleepForSeconds(final Integer second)
	{
		try
		{
			TimeUnit.SECONDS.sleep(second);
		} catch (final InterruptedException e)
		{}
	}
}