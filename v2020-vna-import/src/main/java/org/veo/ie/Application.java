package org.veo.ie;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
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

@SpringBootApplication
@ComponentScan(basePackages = { "org.veo" })
@EntityScan("org.veo.model")
public class Application implements CommandLineRunner {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String JAR_NAME = "v2020-vna-import-<VERSION>.jar";

    public Application() {
    	// Empty constructor
    }

    @Autowired
    VnaImport vnaImport;

    @Override
    public void run(String... args) throws Exception {
        CommandLineParser parser = new BasicParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(CommandLineOptions.get(), args);
            String filePath = line.getOptionValue(CommandLineOptions.FILE);
            String numberOfThreads = line.getOptionValue(CommandLineOptions.THREADS, CommandLineOptions.THREADS_DEFAULT);
            long start = System.currentTimeMillis();
            logFilePath(filePath);
            logNumberOfThreads(numberOfThreads);
            byte[] vnaFileData = Files.readAllBytes(Paths.get(filePath));
            vnaImport.setNumberOfThreads(Integer.valueOf(numberOfThreads));
            vnaImport.importVna(vnaFileData);
            long ms = System.currentTimeMillis() - start;
            logRuntime(ms);

        } catch (ParseException exp) {
            // oops, something went wrong
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar " + JAR_NAME, CommandLineOptions.get());
        }
    }

    private void logFilePath(String filePath) {
        String message = "Importing: " + filePath + "...";
        logMessage(message);
    }

    private void logNumberOfThreads(String numberOfThreads) {
        if (!"1".equals(numberOfThreads)) {
            String message = "Number of parallel threads: " + numberOfThreads;
            logMessage(message);
        }
    }

    private void logRuntime(long ms) {
        String message = "Import finished, runtime: " + TimeFormatter.getHumanRedableTime(ms);
        logMessage(message);
    }

    private void logMessage(String message) {
        if (LOG.isInfoEnabled()) {
            LOG.info(message);
        }
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

}
