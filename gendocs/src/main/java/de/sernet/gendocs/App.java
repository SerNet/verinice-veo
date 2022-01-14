/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Finn Westendorf
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.sernet.gendocs;

import java.io.File;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class App {

    static CommandLineParser cliParser = new DefaultParser();
    final static String CLI_PARAM_DIRECTORY = "d";

    public static void main(String[] args) {
        var options = defineOptions();
        try {
            var commandLine = cliParser.parse(options, args);
            var directory = commandLine.getOptionValue(CLI_PARAM_DIRECTORY);
            if (directory == null) {
                printHelp(options);
                return;
            }
            Renderer.print(buildModel(directory));
        } catch (Exception e) {
            printHelp(options);
        }
    }

    // Definition of CLI options like
    private static Options defineOptions() {
        var options = new Options();
        options.addOption(Option.builder(CLI_PARAM_DIRECTORY)//
                                .desc("e.g. ~/IdeaProjects/verinice-veo/domaintemplates/dsgvo/")//
                                .longOpt("domain-template")//
                                .argName("Domain Template Directory")//
                                .required()
                                .hasArg(true)
                                .build());
        return options;
    }

    // Method for printing the help/usage page
    private static void printHelp(Options options) {
        var formatter = new HelpFormatter();
        var header = "gendocs - tool to generate CustomAspect & -Links documentation\n"
                + "┌─┐┌─┐┌┐┌┌┬┐┌─┐┌─┐┌─┐\n"//
                + "│ ┬├┤ │││ │││ ││  └─┐\n"//
                + "└─┘└─┘┘└┘─┴┘└─┘└─┘└─┘\n";
        formatter.printHelp(80, "gendocs", header, options, "", true);
    }

    // The method that parses the data and build a TemplateModel of it
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    protected static TemplateModel buildModel(String customschemadir) {
        var model = new TemplateModel();
        model.setTypes(TypeParser.parseAllTypes(new File(customschemadir)));
        return model;
    }
}
