package org.veo.service

import spock.lang.Specification

class LinkMapServiceImplTest extends Specification {
    private LinkMapServiceImpl linkMapService
    private MockRepo mockStorage

    def elements = [
            [
                    id   : 'deadbeef',
                    source: "111111",
                    target: "222222",
            ],
            [
                    id   : 'abad1dea',
                    source: "222222",
                    target: "333333",
            ],
            [
                    id   : '110011',
                    source: "333333",
                    target: "111111",
            ],
    ]



    def "find all"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.linkMapService = new LinkMapServiceImpl(mockStorage)

        when:
        def result = linkMapService.findAll()

        then:
        result.size() == 3
    }

    def "find"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.linkMapService = new LinkMapServiceImpl(mockStorage)

        when:
        def result = linkMapService.find("abad1dea")

        then:
        result["source"] == "222222"
        result["target"] == "333333"
    }

    def "find by element"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.linkMapService = new LinkMapServiceImpl(mockStorage)

        when:
        def result = linkMapService.findByElement("333333")

        then:
        result[0]["id"] == "abad1dea"
        result[1]["id"] == "110011"
    }

    def "save new"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.linkMapService = new LinkMapServiceImpl(mockStorage)

        def newItem = [
                source: "555555",
                target: "777777"
        ]
        mockStorage.setNextId("444444")

        when:
        def generatedId = linkMapService.saveNew(newItem)

        then:
        generatedId == "444444"
        elements.size() == 4
        elements[3]["id"] == "444444"
        elements[3]["source"] == "555555"
        elements[3]["target"] == "777777"
    }

    def "save"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.linkMapService = new LinkMapServiceImpl(mockStorage)

        def newItem = [
                id   : 'deadbeef',
                source: "111111",
                target: "333333",
                new: "value"
        ]

        when:
        linkMapService.save('deadbeef', newItem)



        then:
        elements.size() == 3
        elements[2]["id"] == "deadbeef"
        elements[2]["source"] == "111111"
        elements[2]["target"] == "333333"
        elements[2]["new"] == "value"
    }

    def "delete"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.linkMapService = new LinkMapServiceImpl(mockStorage)

        when:
        linkMapService.delete("abad1dea")

        then:
        elements.find { it['id'] == "abad1dea" } == null
        elements.size() == 2
    }
}