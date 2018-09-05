package org.veo.rest

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.veo.service.LinkMapService

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class LinksControllerTest {
    private LinksController controller
    private MockMvc mockMvc

    @Before
    void setup() {
        def mockMapService = new MockMapService();
        controller = new LinksController(mockMapService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }


    @Test
    void getAll() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/links").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath('$').isArray())
    }

    @Test
    void get() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/links/deadbeef").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath('$.source').value("111111"))
                .andExpect(MockMvcResultMatchers.jsonPath('$.target').value("222222"))
    }

    @Test
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/links/abad1dea").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
    }

    @Test
    void post() throws Exception {
        def postBody = '''{
            "source": 555555,
            "target": 777777
        }'''

        mockMvc.perform(MockMvcRequestBuilders.post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.header().stringValues("Location", "/links/444444"))
    }

    @Test
    void put() throws Exception {
        def postBody = '''{
            "id": "deadbeef",
            "source": 111111,
            "target": 333333,
            "new": "value"
        }'''

        mockMvc.perform(MockMvcRequestBuilders.put("/links/deadbeef")
                .contentType(MediaType.APPLICATION_JSON)
                .content(postBody.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
    }

    @Test
    void getByElement() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/elements/333333/links").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(MockMvcResultMatchers.jsonPath('$').isArray())
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].id').value("abad1dea"))
                .andExpect(MockMvcResultMatchers.jsonPath('$[1].id').value("110011"))
    }

    private class MockMapService implements LinkMapService {

        @Override
        List<Map<String, Object>> findAll() {
            return [
                    [
                            id   : 'deadbeef',
                            source: "111111",
                            target: "222222",
                    ],
                    [
                            id   : 'abad1dea',
                            source: "222222",
                            target: '333333',
                    ],
                    [
                            id   : '110011',
                            source: '333333',
                            target: "111111",
                    ]
            ]
        }

        @Override
        Map<String, Object> find(String id) {
            return [
                    id   : 'deadbeef',
                    source: "111111",
                    target: "222222"
            ]
        }

        @Override
        List<Map<String, Object>> findByElement(String elementId) {
            return [
                    [
                            id    : 'abad1dea',
                            source: '222222',
                            target: '333333',
                    ],
                    [
                            id    : '110011',
                            source: '333333',
                            target: '111111'
                    ]
            ]
        }

        @Override
        void save(String id, Map<String, Object> content) {

        }

        @Override
        String saveNew(Map<String, Object> content) {
            return "444444"
        }

        @Override
        void delete(String id) {

        }
    }
}
