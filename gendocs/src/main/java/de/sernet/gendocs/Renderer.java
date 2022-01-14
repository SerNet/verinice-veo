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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public final class Renderer {
    private Renderer() {
        // Hide useless constructor
    }

    public static void print(TemplateModel model) {
        render(model, System.out);
    }

    @SuppressFBWarnings("TEMPLATE_INJECTION_FREEMARKER")
    public static void render(TemplateModel model, OutputStream os) {
        var config = new Configuration(Configuration.VERSION_2_3_31);
        config.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        try {
            config.setClassForTemplateLoading(Renderer.class, "/");
            Template template = config.getTemplate("SchemaDocTemplate.ftl");
            template.process(model, new OutputStreamWriter(os, StandardCharsets.UTF_8));
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
    }
}
