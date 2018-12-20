/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
package org.veo.service.ie;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.sernet.sync.sync.SyncRequest;

/**
 * Wrapper class to access the content of a verinice archive (VNA) file.
 *
 * A verinice archive is an ordinary ZIP-Archive with file extension '.vna'. You
 * can open/import a verinice archive with verinice. Verinice is an Open Source
 * information security management system (ISMS), see http://verinice.org for
 * more information.
 *
 * Content of a verinice archive:
 *
 * @formatter:off
 * files                 directory containing all attached files
 *   |
 *   -<EXT_ID>_<FILE_NAME>.doc file 1
 *   |
 *   -<EXT_ID>_<FILE_NAME>.pdf file 2
 *   |
 *   - ...
 *   |
 * verinice.xml          XML file containing data of all verinice elements
 *   |
 * sync.xsd              XML Schema / XML Schema Definition (XSD) for verinice.xml
 *   |
 * data.xsd              XSD for verinice.xml
 *   |
 * mapping.xsd           XSD for verinice.xml
 * @formatter:on
 *
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class Vna {

    private static final Logger LOG = LoggerFactory.getLogger(Vna.class);
    private static final String VERINICE_XML = "verinice.xml";

    private Vna() {
    }

    /**
     * Unmarshals verinice.xml out of a verinice archive input stream.
     *
     * @param inpuStream
     *            The input stream on an open verinice archive (VNA, zip archive)
     * @throws VnaNotValidException
     *             In case of a missing entry in a VNA
     */
    public static SyncRequest getXml(InputStream inputStream) throws VnaNotValidException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating new Vna...");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            for (ZipEntry zipEntry = zipInputStream.getNextEntry(); zipEntry != null; zipInputStream
                    .getNextEntry()) {
                if (VERINICE_XML.equals(zipEntry.getName())) {
                    return javax.xml.bind.JAXB.unmarshal(zipInputStream, SyncRequest.class);
                }
            }
        } catch (Exception e) {
            LOG.error("Error while reading verinice archive", e);
            throw new VnaNotValidException(e);
        }
        throw new VnaNotValidException(String.format("vna does not contain %s", VERINICE_XML));
    }
}
