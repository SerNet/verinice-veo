/*******************************************************************************
 * Copyright (c) 2018 Jochen Kemnade.
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
 ******************************************************************************/
package org.veo;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableFileFilter;

/** Prefixes a license header before the package statement. */
public final class LicenseHeaderStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String NAME = "veoLicenseHeader";
    private static final String DEFAULT_YEAR_DELIMITER = "-";
    private static final String LICENSE_HEADER_DELIMITER = "package ";

    private static final SerializableFileFilter UNSUPPORTED_JVM_FILES_FILTER = SerializableFileFilter.skipFilesNamed("package-info.java",
                                                                                                                     "package-info.groovy",
                                                                                                                     "module-info.java");

    private static final String licenseHeaderTemplate = "/*******************************************************************************\n"
            + " * Copyright (c) $YEAR $AUTHOR.\n" + " *\n"
            + " * This program is free software: you can redistribute it and/or\n"
            + " * modify it under the terms of the GNU Lesser General Public License\n"
            + " * as published by the Free Software Foundation, either version 3\n"
            + " * of the License, or (at your option) any later version.\n"
            + " * This program is distributed in the hope that it will be useful,\n"
            + " * but WITHOUT ANY WARRANTY; without even the implied warranty\n"
            + " * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.\n"
            + " * See the GNU Lesser General Public License for more details.\n" + " *\n"
            + " * You should have received a copy of the GNU Lesser General Public License\n"
            + " * along with this program.\n" + " * If not, see <http://www.gnu.org/licenses/>.\n"
            + " ******************************************************************************/\n";

    private final String author;
    private final Pattern delimiterPattern;

    /**
     * Creates a FormatterStep which forces the start of each file to match a
     * license header.
     */
    public static FormatterStep create(String author) {
        Objects.requireNonNull(author, "author");
        return FormatterStep.create(LicenseHeaderStep.NAME, new LicenseHeaderStep(author,
                LICENSE_HEADER_DELIMITER, DEFAULT_YEAR_DELIMITER), step -> step::format);
    }

    public static String name() {
        return NAME;
    }

    public static String defaultYearDelimiter() {
        return DEFAULT_YEAR_DELIMITER;
    }

    public static SerializableFileFilter unsupportedJvmFilesFilter() {
        return UNSUPPORTED_JVM_FILES_FILTER;
    }

    /** The license that we'd like enforced. */
    private LicenseHeaderStep(String author, String delimiter, String yearSeparator) {
        if (delimiter.contains("\n")) {
            throw new IllegalArgumentException("The delimiter must not contain any newlines.");
        }

        this.author = author;
        this.delimiterPattern = Pattern.compile('^' + delimiter,
                                                Pattern.UNIX_LINES | Pattern.MULTILINE);
    }

    /** Formats the given string. */
    public String format(String raw) {
        Matcher matcher = delimiterPattern.matcher(raw);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Unable to find delimiter regex " + delimiterPattern);
        } else {
            String existingLicense = raw.substring(0, matcher.start());
            if (existingLicense.contains("Apache Software License")) {
                // don't change files which have a different license
                return raw;
            }
            String licenseTemplateWithTokensReplaced = licenseHeaderTemplate.replace("$YEAR",
                                                                                     "\\E\\d{4}\\Q")
                                                                            .replace("$AUTHOR",
                                                                                     "\\E[\\p{IsAlphabetic}' -]+\\Q");
            Pattern p = Pattern.compile("^\\Q" + licenseTemplateWithTokensReplaced + "\\E",
                                        Pattern.UNIX_LINES | Pattern.MULTILINE);
            Matcher m = p.matcher(existingLicense);
            if (m.find()) {
                // if no change is required, return the raw string without
                // creating any other new strings for maximum performance
                return raw;
            } else {
                // otherwise we'll have to add the header
                Pattern existingInfo = Pattern.compile("Copyright \\(c\\) (\\d{4}) (.*)\\.");
                Matcher existingInfoMatcher = existingInfo.matcher(existingLicense);
                String yearToUse;
                String authorToUse;
                if (existingInfoMatcher.find()) {
                    yearToUse = existingInfoMatcher.group(1);
                    authorToUse = existingInfoMatcher.group(2);
                } else {
                    yearToUse = String.valueOf(YearMonth.now()
                                                        .getYear());
                    authorToUse = author;
                }
                String licenseHeaderExtrapolated = licenseHeaderTemplate.replace("$YEAR", yearToUse)
                                                                        .replace("$AUTHOR",
                                                                                 authorToUse);
                return licenseHeaderExtrapolated + raw.substring(matcher.start());
            }
        }
    }
}