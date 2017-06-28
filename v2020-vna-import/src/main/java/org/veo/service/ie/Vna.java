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
 * 
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package org.veo.service.ie;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veo.util.io.Archive;
import org.veo.util.io.XmlIO;

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
 * files directory containing all attached files | +-<EXT_ID>_<FILE_NAME>.doc
 * file 1 | +-<EXT_ID>_<FILE_NAME>.pdf file 2 | +- ... | verinice.xml XML file
 * containing data of all verinice elements | sync.xsd XML Schema / XML Schema
 * Definition (XSD) for verinice.xml | data.xsd XSD for verinice.xml |
 * mapping.xsd XSD for verinice.xml
 * 
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class Vna implements Serializable {

    private static final long serialVersionUID = 4563763042849838627L;

    private static final Logger LOG = LoggerFactory.getLogger(Vna.class);

    public static final String EXTENSION_VERINICE_ARCHIVE = ".vna"; //$NON-NLS-1$
    public static final String VERINICE_XML = "verinice.xml"; //$NON-NLS-1$
    public static final String DATA_XSD = "data.xsd"; //$NON-NLS-1$
    public static final String MAPPING_XSD = "mapping.xsd"; //$NON-NLS-1$
    public static final String SYNC_XSD = "sync.xsd"; //$NON-NLS-1$
    public static final String README_TXT = "readme.txt"; //$NON-NLS-1$

    private static final String[] ALL_STATIC_FILES = new String[] { VERINICE_XML, DATA_XSD, MAPPING_XSD, SYNC_XSD,
            README_TXT, };

    static {
        Arrays.sort(ALL_STATIC_FILES);
    }

    private String uuid;
    private String tempFileName = null;
    public static final String FILES = "files"; //$NON-NLS-1$

    /**
     * Creates a Vna instance out of <code>byte[] data</code>.
     * 
     * @param data
     *            Data of a verinice archive (VNA, zip archive)
     * @throws VnaNotValidException
     *             In case of a missing entry in a VNA
     */
    public Vna(byte[] data) throws VnaNotValidException {
        super();
        uuid = UUID.randomUUID().toString();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating new Vna...");
        }
        try {
            Archive.extractZipArchive(data, getTempFileName());
        } catch (Exception e) {
            LOG.error("Error while reading verinice archive", e);
            throw new VnaNotValidException(e);
        }
    }

    public byte[] getFileData(String fileName) {
        String fullPath = getPathToTempFile(fileName);
        try {
            return FileUtils.readFileToByteArray(new File(fullPath));
        } catch (Exception e) {
            LOG.error("Error while loading file data: " + fullPath, e);
            return null;
        }
    }

    private String getPathToTempFile(String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTempFileName()).append(File.separator).append(fileName);
        return sb.toString();
    }

    /**
     * Returns file verinice.xml from the archive. If there is no verinice.xml
     * in the archive null is returned.
     * 
     * @return verinice.xml from the archive
     */
    public byte[] getXmlFileData() {
        return getFileData(VERINICE_XML);
    }

    public String getXmlFilePath() {
        return getPathToTempFile(VERINICE_XML);
    }

    public SyncRequest getXml() {
        return XmlIO.read(getXmlFilePath(), SyncRequest.class);
    }

    public void clear() {
        try {
            FileUtils.deleteDirectory(new File(getTempFileName()));
        } catch (IOException e) {
            LOG.error("Error while deleting zipfile content.", e);
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getTempFileName() {
        if (tempFileName == null) {
            tempFileName = createTempFileName(getUuid());
        }
        return tempFileName;
    }

    private static String createTempFileName(String uuid) {
        String tempDir = System.getProperty("java.io.tmpdir");
        StringBuilder sb = new StringBuilder().append(tempDir);
        if (!tempDir.endsWith(String.valueOf(File.separatorChar))) {
            sb.append(File.separatorChar);
        }
        return sb.append(uuid).toString();
    }
}
