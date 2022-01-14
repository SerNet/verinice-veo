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

package de.sernet.gendocs

import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor

@TupleConstructor
class TypeParser {

    def typedir
    def translations
    protected TypeParser(File typedir) {
        this.typedir = typedir
        translations = new JsonSlurper().parse(new File(typedir, "lang.json"))
    }

    static List<VeoType> parseAllTypes(File domaintemplatedir) {
        new File(domaintemplatedir, "types").listFiles().toSorted().collect {new TypeParser(it).parseType()}
    }

    VeoType parseType() {
        new VeoType().tap {
            name = makeNameFromFile(typedir)
            aspects = sortedFiles("customAspects").collect {parseAspect(it)}
            links = sortedFiles("links").collect {parseLink(it)}
            subTypes = sortedFiles("subTypes").collect {it.name.split("\\.json")[0]}
        }
    }

    //The method that parses a specific custom aspect
    CustomAspect parseAspect(File file) {
        def schema = new JsonSlurper().parse(file)
        def name = file.name[0..-6]//strip .json
        new CustomAspect().tap {
            key = name
            name = getEnglishTranslation(name)
            translation = getGermanTranslation(name)
            attributes = schema.attributeSchemas.collect { parseAttribute(it) }
        }
    }

    //The method that parses a specific custom link
    CustomLink parseLink(File file) {
        def schema = new JsonSlurper().parse(file)
        def name = stripjson(file)
        new CustomLink().tap {
            key = name
            name = getEnglishTranslation(name)
            translation = getGermanTranslation(name)
            attributes = schema.attributeSchemas.collect { parseAttribute(it) }
            targetType = schema.targetType
            targetSubType = schema.targetSubType
        }
    }

    //Method that grabs a single Attribute from the parsed data
    private Attribute parseAttribute(Map.Entry attribute) {
        new Attribute().tap {
            key = attribute.key
            def prop = attribute.value
            title = getEnglishTranslation(attribute.key as String)
            translation = getGermanTranslation(attribute.key as String)
            type = prop.type
            format = prop.format
            if (type == "array") {
                oneOf = prop.items.enum.collect{new AttributeType(it, getEnglishTranslation(it), getGermanTranslation(it))}
            }
        }
    }

    private static String makeNameFromFile(file) {
        def name = stripjson(file)
        def first = name.charAt(0).toUpperCase().toString()
        first + name.substring(1)
    }

    private static String stripjson(File file) {
        file.name.split(/\.json/)[0]
    }

    //Method to retrieve the title or description from the lang.json
    private String getEnglishTranslation(String key) {
        translations.en.get(key)
    }

    //Method to retrieve the german title or description from the lang.json
    private String getGermanTranslation(String key) {
        translations.de.get(key)
    }

    //Method to prevent NPE in parseAspect/Link
    private File[] sortedFiles(String folder) {
        def files = new File(typedir, folder).listFiles()
        if (files != null)
            files.toSorted()
    }
}