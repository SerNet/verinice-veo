package org.veo.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest("veo.basedir=/tmp/veo")
public class StaticControllerTest {
    @Autowired
    private StaticController staticController;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(staticController).build();
    }

    @Test
    public void getSchema() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/schemas/test.json").accept("application/schema+json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/schema+json; charset=utf-8"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Test JSON with non-ascii chars: äüöÄÜÖ€"));
    }
}
