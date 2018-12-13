/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
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
package org.veo.ie;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Command line optiosn for running an import of a VNA file.
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public final class CommandLineOptions {

    public static final String FILE = "file";
    public static final String THREADS = "threads";
    public static final String THREADS_DEFAULT = "1";

    private static Options options;

    private CommandLineOptions() {
    }

    public static Options get() {
        if (options == null) {
            createOptions();
        }
        return options;
    }

    private static void createOptions() {
        options = new Options();
        options.addOption(CommandLineOptions.createHelpOption());
        options.addOption(CommandLineOptions.createFileOption());
        options.addOption(CommandLineOptions.createNumberOfThreadsOption());
    }

    private static Option createFileOption() {
        return Option.builder("f").hasArg().required().desc("use given VNA file (required)")
                .longOpt(FILE).build();
    }

    private static Option createNumberOfThreadsOption() {
        return Option.builder("t").hasArg()
                .desc("number of threads (default: " + THREADS_DEFAULT + ")").longOpt(THREADS)
                .build();
    }

    private static Option createHelpOption() {
        return new Option("h", "help", false, "print this message");
    }
}
