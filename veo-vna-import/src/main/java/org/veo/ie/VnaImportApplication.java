/*******************************************************************************
 * Copyright (c) 2018 Daniel Murygin.
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
package org.veo.ie;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import org.veo.core.entity.util.TimeFormatter;

public class VnaImportApplication {

    private static final Logger log = LoggerFactory.getLogger(VnaImportApplication.class);

    private static final String JAR_NAME = "veo-vna-import-<VERSION>.jar";
    private static final String THREADS_DEFAULT = "1";

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = newCommandLineOptions();

        try {
            long start = System.currentTimeMillis();
            CommandLine line = parser.parse(options, args);
            final String filePath = line.getOptionValue("file");
            final int numberOfThreads = Integer.parseInt(line.getOptionValue("threads",
                                                                             THREADS_DEFAULT));
            ConfigurableApplicationContext context = SpringApplication.run(VnaImportConfiguration.class,
                                                                           args);
            log.info("Importing: {}...", filePath);
            logNumberOfThreads(numberOfThreads);
            try (InputStream rawZipStream = Files.newInputStream(Paths.get(filePath))) {
                VnaImport vnaImport = context.getBean(VnaImport.class);
                vnaImport.setNumberOfThreads(numberOfThreads);
                vnaImport.importVna(rawZipStream);
                long ms = System.currentTimeMillis() - start;
                logRuntime(ms);
            }
        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + JAR_NAME, options);
        }
    }

    private static Options newCommandLineOptions() {
        Options options = new Options();
        options.addOption(new Option("h", "help", false, "print this message"));
        options.addOption(Option.builder("f")
                                .hasArg()
                                .required()
                                .desc("use given VNA file (required)")
                                .longOpt("file")
                                .build());
        options.addOption(Option.builder("t")
                                .hasArg()
                                .desc("number of threads (default: " + THREADS_DEFAULT + ")")
                                .longOpt("threads")
                                .build());
        return options;
    }

    private static void logNumberOfThreads(int numberOfThreads) {
        if (numberOfThreads != 1) {
            log.info("Number of parallel threads: {}", numberOfThreads);
        }
    }

    private static void logRuntime(long ms) {
        if (log.isInfoEnabled()) {
            log.info("Import finished, runtime: {}", TimeFormatter.getHumanRedableTime(ms));
        }
    }
}
