/*******************************************************************************
 * Copyright (c) 2012 Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package org.veo.util.time;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides public static utility functions to format dates and times. Do not
 * instantiate this class.
 *
 * @author Daniel Murygin
 */
public final class TimeFormatter {

    public static final DateTimeFormatter DATE_TIME_FORMATTER_ISO_8601 = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private static final int MILLIS_PER_SECOND = 1000;

    private static final int MINUTES_PER_HOUR = 60;

    private static final int HOURS_PER_DAY = 24;

    private static final int SECONDS_PER_MINUTE = 60;

    private static final int MILLIS_PER_MINUTE = MILLIS_PER_SECOND * SECONDS_PER_MINUTE;

    private static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;

    private static final int MILLIS_PER_HOUR = MILLIS_PER_SECOND * SECONDS_PER_HOUR;

    private static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;

    private static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;

    private TimeFormatter() {
        super();
        // do not instantiate this class
    }

    public static String getIso8601FromEpochMillis(long epochMillis, ZoneId zoneId) {
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId);
        return zonedDateTime.format(DATE_TIME_FORMATTER_ISO_8601);
    }

    public static ZonedDateTime getDateFromIso8601(String dateIso8601) {
        return ZonedDateTime.parse(dateIso8601, DATE_TIME_FORMATTER_ISO_8601);
    }

    public static String getHumanRedableTime(final long ms) {
        Interval interval = new Interval(ms);
        boolean outputDays;
        boolean outputHours;
        boolean outputMinutes;
        boolean outputSeconds;
        boolean outputMilliseconds;

        if (interval.days > 0) {
            outputDays = true;
            outputMinutes = false;
            outputSeconds = false;
            outputMilliseconds = false;
            interval = interval.roundToHours();
            outputHours = interval.hours > 0;
        } else if (interval.hours > 0) {
            outputDays = false;
            outputHours = true;
            outputSeconds = false;
            outputMilliseconds = false;
            interval = interval.roundToMinutes();
            outputMinutes = interval.minutes > 0;
        } else if (interval.minutes > 0) {
            outputDays = false;
            outputHours = false;
            outputMinutes = true;
            outputMilliseconds = false;
            interval = interval.roundToSeconds();
            outputSeconds = interval.seconds > 0;
        } else {
            outputDays = false;
            outputHours = false;
            outputMinutes = false;
            outputSeconds = interval.seconds > 0;
            outputMilliseconds = !outputSeconds;
            if (!outputMilliseconds) {
                interval = interval.roundToSeconds();
            }
        }

        return formatInterval(interval, outputDays, outputHours, outputMinutes, outputSeconds,
                outputMilliseconds);
    }

    private static String formatInterval(Interval interval, boolean outputDays, boolean outputHours,
            boolean outputMinutes, boolean outputSeconds, boolean outputMilliseconds) {
        StringBuilder sb = new StringBuilder();

        if (outputDays) {
            sb.append(interval.days).append(" d");
        }
        if (outputHours) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(interval.hours).append(" h");
        }
        if (outputMinutes) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(interval.minutes).append(" m");
        }
        if (outputSeconds) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(interval.seconds).append(" s");
        }
        if (outputMilliseconds) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(interval.milliseconds).append(" ms");
        }
        return sb.toString();
    }

    private static final class Interval {
        final int milliseconds;
        final int seconds;
        final int minutes;
        final int hours;
        final int days;

        Interval(long milliseconds) {
            this.milliseconds = (int) milliseconds % 1000;
            seconds = (int) ((milliseconds / MILLIS_PER_SECOND) % SECONDS_PER_MINUTE);
            minutes = (int) ((milliseconds / MILLIS_PER_MINUTE) % MINUTES_PER_HOUR);
            hours = (int) ((milliseconds / MILLIS_PER_HOUR) % HOURS_PER_DAY);
            days = (int) (milliseconds / MILLIS_PER_DAY);
        }

        Interval(int days, int hours, int minutes, int seconds, int milliseconds) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.milliseconds = milliseconds;
        }

        Interval roundToSeconds() {
            int newSeconds = seconds;
            int newMinutes = minutes;
            if (milliseconds >= 500) {
                newSeconds += 1;

                if (newSeconds == SECONDS_PER_MINUTE) {
                    newMinutes = newMinutes + 1;
                    newSeconds = 0;
                }
            }

            return new Interval(days, hours, newMinutes, newSeconds, 0);
        }

        public Interval roundToMinutes() {
            int newMinutes = minutes;
            int newHours = hours;
            if (seconds >= 30) {
                newMinutes += 1;

                if (newMinutes == MINUTES_PER_HOUR) {
                    newHours = newHours + 1;
                    newMinutes = 0;
                }
            }

            return new Interval(days, newHours, newMinutes, 0, 0);
        }

        public Interval roundToHours() {
            int newDays = days;
            int newHours = hours;
            if (minutes >= 30) {
                newHours += 1;

                if (newHours == HOURS_PER_DAY) {
                    newDays = newDays + 1;
                    newHours = 0;
                }
            }

            return new Interval(newDays, newHours, 0, 0, 0);
        }
    }

}