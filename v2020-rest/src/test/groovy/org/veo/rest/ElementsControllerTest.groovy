package org.veo.rest

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.veo.service.ElementMapService

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest("veo.basedir=/tmp/veo")
public class ElementsControllerTest {
    private ElementsController elementsController
    private MockMvc mockMvc

    @Before
    void setup() {
        def mockMapService = new MockMapService();
        this.elementsController = new ElementsController(mockMapService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(elementsController).build();
    }


    @Test
    void getAll() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/elements").accept("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath('$').isArray())
    }

    @Test
    void get() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/elements/deadbeef").accept("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath('$.title').value("I'm test data…"))
    }

    @Test
    void getChildren() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.get("/elements/abad1dea/children").accept("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath('$').isArray())
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].title').value("I'm a child"))
                .andExpect(MockMvcResultMatchers.jsonPath('$[1].title').value("I'm a child, too"))
    }

    @Test
    void post() throws Exception {
        def postBody = '''{
            "title": "I am new"
        }'''

        mockMvc.perform(MockMvcRequestBuilders.post("/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().stringValues("Location", "/elements/444444"))
    }

    @Test
    void put() throws Exception {
        def postBody = '''{
            "id": "deadbeef",
            "title": "I am modified",
            "new": "value"
        }'''

        mockMvc.perform(MockMvcRequestBuilders.put("/elements/deadbeef")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
    }

    @Test
    void delete() throws Exception {
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/elements/abad1dea").accept("application/json"))
                .andExpect(MockMvcResultMatchers.status().isOk())
    }

    private class MockMapService implements ElementMapService {

        @Override
        List<Map<String, Object>> findAll() throws IOException {
            return [
                    [
                            id   : 'deadbeef',
                            title: "I'm test data…"
                    ],
                    [
                            id   : 'abad1dea',
                            title: "I'm test data, too"
                    ],
                    [
                            id   : '813ad81',
                            title: "I'm a child",
                            parent: "abad1dea"
                    ],
                    [
                            id   : '813ad81',
                            title: "I'm a child, too",
                            parent: "abad1dea"
                    ],
            ]
        }

        @Override
        Map<String, Object> find(String id) throws IOException {
            return [
                    id   : 'deadbeef',
                    title: "I'm test data…"
            ]
        }

        @Override
        List<Map<String, Object>> findChildren(String parentId) throws IOException {
            return [
                    [
                            id   : '813ad81',
                            title: "I'm a child",
                            parent: "abad1dea"
                    ],
                    [
                            id   : '813ad81',
                            title: "I'm a child, too",
                            parent: "abad1dea"
                    ],
            ]
        }

        @Override
        void save(String id, Map<String, Object> content) throws IOException {

        }

        @Override
        String saveNew(Map<String, Object> content) throws IOException {
            return "444444"
        }

        @Override
        void delete(String id) throws IOException {

        }
    }
}
