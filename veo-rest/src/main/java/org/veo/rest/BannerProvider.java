/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest;

import java.util.List;
import java.util.Random;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BannerProvider {
    public static String getBanner() {
        try {
            return render();
        } catch (Exception ex) {
            log.error("Banner rendering failed", ex);
            return "VEO";
        }
    }

    private static String render() {
        return "<=====================================================================>\n\n"
                + "    __/\\\\\\________/\\\\\\__/\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\_______/\\\\\\\\\\______        \n"
                + "     _\\/\\\\\\_______\\/\\\\\\_\\/\\\\\\///////////______/\\\\\\///\\\\\\____       \n"
                + "      _\\//\\\\\\______/\\\\\\__\\/\\\\\\_______________/\\\\\\/__\\///\\\\\\__      \n"
                + "       __\\//\\\\\\____/\\\\\\___\\/\\\\\\\\\\\\\\\\\\\\\\______/\\\\\\______\\//\\\\\\_     \n"
                + "        ___\\//\\\\\\__/\\\\\\____\\/\\\\\\///////______\\/\\\\\\_______\\/\\\\\\_    \n"
                + "         ____\\//\\\\\\/\\\\\\_____\\/\\\\\\_____________\\//\\\\\\______/\\\\\\__   \n"
                + "          _____\\//\\\\\\\\\\______\\/\\\\\\______________\\///\\\\\\__/\\\\\\____  \n"
                + "           ______\\//\\\\\\_______\\/\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\____\\///\\\\\\\\\\/_____ \n"
                + "            _______\\///________\\///////////////_______\\/////_______    \n\n"
                + getCenteredRandomTitle(72) + "\n\n"
                + "<=====================================================================>";
    }

    @SuppressFBWarnings("SECPR")
    private static String getCenteredRandomTitle(int artWidth) {
        var descriptions = List.of("Very Effective Organizer", "Valuable Entity Office",
                                   "Vinegar & Eggplant Omelet", "Verified Elephant Orphanage",
                                   "Very Elegant Orangutan", "Valued Electric Outlet");
        var description = descriptions.get(new Random().nextInt(descriptions.size()));
        var leftMarginWidth = Math.max(0, artWidth - description.length()) / 2;
        return " ".repeat(leftMarginWidth) + description;
    }
}
