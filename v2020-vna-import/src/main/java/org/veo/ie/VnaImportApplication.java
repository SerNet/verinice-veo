package org.veo.ie;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.veo.service.ie.VnaImport;
import org.veo.util.time.TimeFormatter;

/**
 * Spring Boot application class to run the import of a VNA file. You should
 * start this application with property spring.main.web-environment=false in
 * your application.properties file to turn the starting of a web environment
 * off.
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
@SpringBootApplication
@ComponentScan(basePackages = { "org.veo" })
@EntityScan("org.veo.model")
public class VnaImportApplication implements CommandLineRunner {

    private static Logger log = LoggerFactory.getLogger(VnaImportApplication.class);

    private static final String JAR_NAME = "v2020-vna-import-<VERSION>.jar";

    public VnaImportApplication() {
        // Empty constructor
    }

    @Autowired
    VnaImport vnaImport;

    @Override
    public void run(String... args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(CommandLineOptions.get(), args);
            String filePath = line.getOptionValue(CommandLineOptions.FILE);
            String numberOfThreads = line.getOptionValue(CommandLineOptions.THREADS, CommandLineOptions.THREADS_DEFAULT);
            long start = System.currentTimeMillis();
            log.info("Importing: {}...", filePath);
            logNumberOfThreads(numberOfThreads);
            byte[] vnaFileData = Files.readAllBytes(Paths.get(filePath));
            vnaImport.setNumberOfThreads(Integer.parseInt(numberOfThreads));
            vnaImport.importVna(vnaFileData);
            long ms = System.currentTimeMillis() - start;
            logRuntime(ms);

        } catch (ParseException exp) {
            // oops, something went wrong
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + JAR_NAME, CommandLineOptions.get());
        }
    }

    private void logNumberOfThreads(String numberOfThreads) {
        if (!"1".equals(numberOfThreads)) {
            log.info("Number of parallel threads: {}", numberOfThreads);
        }
    }

    private void logRuntime(long ms) {
        if (log.isInfoEnabled()) {
            log.info("Import finished, runtime: {}", TimeFormatter.getHumanRedableTime(ms));
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(VnaImportApplication.class, args);
    }

}
