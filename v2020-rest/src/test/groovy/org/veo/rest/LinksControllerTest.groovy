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
import org.veo.service.LinkMapService

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
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
                .andExpect(MockMvcResultMatchers.jsonPath('$.source.$ref').value("/elements/111111"))
    }

    @Test
    void delete() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/links/abad1dea").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
    }

    @Test
    void post() throws Exception {
        def postBody = '''{
            "source": {
                "$ref": "/elements/555555"
            },
            "target": {
                "$ref": "/elements/777777"
            }
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
            "source": {
                "$ref": "/elements/111111"
            },
            "target": {
                "$ref": "/elements/333333"
            },
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
        List<Map<String, Object>> findAll() throws IOException {
            return [
                    [
                            id   : 'deadbeef',
                            source: [
                                    $ref: "/elements/111111"
                            ],
                            target: [
                                    $ref: "/elements/222222"
                            ],
                    ],
                    [
                            id   : 'abad1dea',
                            source: [
                                    $ref: "/elements/222222"
                            ],
                            target: [
                                    $ref: "/elements/333333"
                            ],
                    ],
                    [
                            id   : '110011',
                            source: [
                                    $ref: "/elements/333333"
                            ],
                            target: [
                                    $ref: "/elements/111111"
                            ],
                    ]
            ]
        }

        @Override
        Map<String, Object> find(String id) throws IOException {
            return [
                    id   : 'deadbeef',
                    source: [
                            $ref: "/elements/111111"
                    ],
                    target: [
                            $ref: "/elements/222222"
                    ],
            ]
        }

        @Override
        List<Map<String, Object>> findByElement(String elementId) throws IOException {
            return [
                    [
                            id    : 'abad1dea',
                            source: [
                                    $ref: "/elements/222222"
                            ],
                            target: [
                                    $ref: "/elements/333333"
                            ],
                    ],
                    [
                            id    : '110011',
                            source: [
                                    $ref: "/elements/333333"
                            ],
                            target: [
                                    $ref: "/elements/111111"
                            ],
                    ]
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