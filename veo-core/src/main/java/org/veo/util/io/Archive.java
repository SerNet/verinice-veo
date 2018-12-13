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
package org.veo.util.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public final class Archive {

    private static final Logger LOG = LoggerFactory.getLogger(Archive.class);

    static final int BUFFER_SIZE = 1024;

    private Archive() {
        // don not instantiate this class
    }

    /**
     * Extracts all entries of a Zip-Archive
     *
     * @param zipFileData
     *            Data of a zip archive
     * @throws IOException
     */
    public static void extractZipArchive(byte[] zipFileData, String directory) throws IOException {
        new File(directory).mkdirs();
        // get the zip file content
        try (ZipInputStream inputStream = new ZipInputStream(
                new ByteArrayInputStream(zipFileData))) {
            // get the zipped file list entry
            ZipEntry zipEntry = inputStream.getNextEntry();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String fileName = zipEntry.getName();
                    File newFile = new File(directory + File.separator + fileName);
                    new File(newFile.getParent()).mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {

                        int len;
                        while ((len = inputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("File unzipped: " + newFile.getAbsoluteFile());
                    }
                }
                zipEntry = inputStream.getNextEntry();
            }
            inputStream.closeEntry();
        }
    }
}
