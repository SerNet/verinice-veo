/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

class CustomPrettyPrinter extends DefaultPrettyPrinter {

    private static final long serialVersionUID = -838025542289234422L;
    private int depth = 0;

    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator g) throws IOException {
        g.writeRaw(": ");
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
        CustomPrettyPrinter instance = new CustomPrettyPrinter();
        instance.indentArraysWith(new DefaultIndenter());
        return instance;
    }

    @Override
    public void writeStartObject(JsonGenerator jg) throws IOException {
        super.writeStartObject(jg);
        ++depth;
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws IOException {
        if (nrOfEntries == 0) {
            g.writeRaw("\n");
            for (int i = 0; i < depth; i++) {
                g.writeRaw("  ");
            }
            g.writeRaw("\n");
            for (int i = 0; i < depth - 2; i++) {
                g.writeRaw("  ");
            }
            g.writeRaw(" ");
        }
        super.writeEndObject(g, nrOfEntries);
        if (--depth == 0) {
            g.writeRaw('\n');
        }
    }
}