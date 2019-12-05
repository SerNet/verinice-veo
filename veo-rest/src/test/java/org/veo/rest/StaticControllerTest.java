/*******************************************************************************
 * Copyright (c) 2018 Alexander Ben Nasrallah.
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
package org.veo.rest;

import java.io.File;
import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;

import org.veo.service.VeoConfigurationService;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class StaticControllerTest {
    @Autowired
    private StaticController staticController;

    @Autowired
    private VeoConfigurationService veoConfigurationService;

    private MockMvc mockMvc;

    @Before
    public void setup() throws IOException {
        // copy the test schemas into the veo basedir
        File schemaDir = new File(veoConfigurationService.getBaseDir(), "schemas");
        schemaDir.mkdirs();
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] schemaResources = resourcePatternResolver.getResources("classpath:schemas/*.json");
        for (Resource resource : schemaResources) {
            File target = new File(schemaDir, resource.getFilename());
            FileCopyUtils.copy(resource.getFile(), target);
        }
        this.mockMvc = MockMvcBuilders.standaloneSetup(staticController)
                                      .build();

    }

    @Test
    public void getSchema() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/schemas/elements/test.json")
                                                   .accept("application/schema+json"))
                    .andExpect(MockMvcResultMatchers.status()
                                                    .isOk())
                    .andExpect(MockMvcResultMatchers.content()
                                                    .contentType("application/schema+json; charset=utf-8"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.description")
                                                    .value("Test JSON with non-ascii chars: äüöÄÜÖ€"));
    }

    @Test
    public void getElementTypes() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/schemas/elements/")
                                                   .accept("application/json"))
                    .andExpect(MockMvcResultMatchers.status()
                                                    .isOk())
                    .andExpect(MockMvcResultMatchers.content()
                                                    .contentType("application/json;charset=UTF-8"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$")
                                                    .value(CoreMatchers.hasItems("test.json")));
    }
}
