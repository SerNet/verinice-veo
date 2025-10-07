/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Jochen Kemnade
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

import java.io.File;
import java.io.IOException;

import org.ini4j.Wini;

public class GitConfigUtil {

  /**
   * Returns the effective Git user.name. Checks repository config first, then falls back to global
   * config.
   */
  public static String getUserName() throws IOException {
    // 1. Try repository config
    File repoConfig = findRepoConfig();
    if (repoConfig != null && repoConfig.exists()) {
      String name = readUserNameFromConfig(repoConfig);
      if (name != null) {
        return name;
      }
    }

    // 2. Fall back to global config (~/.gitconfig or %USERPROFILE%\.gitconfig)
    File globalConfig = new File(System.getProperty("user.home"), ".gitconfig");
    if (globalConfig.exists()) {
      return readUserNameFromConfig(globalConfig);
    }

    return null; // no git user.name found
  }

  private static String readUserNameFromConfig(File configFile) throws IOException {
    return new Wini(configFile).get("user", "name");
  }

  private static File findRepoConfig() {
    File dir = new File(System.getProperty("user.dir"));
    while (dir != null) {
      File gitDir = new File(dir, ".git");
      if (gitDir.exists()) {
        File config = new File(gitDir, "config");
        if (config.exists()) {
          return config;
        }
      }
      dir = dir.getParentFile();
    }
    return null;
  }
}
