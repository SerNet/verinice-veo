package org.veo.service

import spock.lang.Specification

class ElementMapServiceImplTest extends Specification {
    private ElementMapServiceImpl elementMapService
    private MockRepo mockStorage

    def elements = [
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
                    id   : '813ad82',
                    title: "I'm a child, too",
                    parent: "abad1dea"
            ],
    ]



    def "find all elements"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.elementMapService = new ElementMapServiceImpl(mockStorage)

        when:
        def result = elementMapService.findAll()

        then:
        result.size() == 4
    }

    def "find element"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.elementMapService = new ElementMapServiceImpl(mockStorage)

        when:
        def result = elementMapService.find("abad1dea")

        then:
        result["title"] == "I'm test data, too"
    }

    def "find children"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.elementMapService = new ElementMapServiceImpl(mockStorage)

        when:
        def result = elementMapService.findChildren("abad1dea")

        then:
        result[0]["title"] == "I'm a child"
        result[1]["title"] == "I'm a child, too"
    }

    def "save new element"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.elementMapService = new ElementMapServiceImpl(mockStorage)

        def newItem = [
                title: "I am new"
        ]
        mockStorage.setNextId("444444")

        when:
        def generatedId = elementMapService.saveNew(newItem)

        then:
        generatedId == "444444"
        elements.size() == 5
        elements[4]["title"] == "I am new"
    }

    def "update element"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.elementMapService = new ElementMapServiceImpl(mockStorage)

        def newItem = [
                id: 'deadbeef',
                title: "I am modified",
                new: 'value'
        ]

        when:
        elementMapService.save('deadbeef', newItem)


        elements.size() == 4

        then:
        def item = elements.find { it['id'] == 'deadbeef' }
        item != null
        item['title'] == "I am modified"
        item['new'] == 'value'
    }

    def "delete element"() {
        setup:
        mockStorage = new MockRepo(elements)
        this.elementMapService = new ElementMapServiceImpl(mockStorage)

        when:
        elementMapService.delete("abad1dea")

        then:
        elements.find { it['id'] == "abad1dea" } == null
        elements.size() == 3
    }
}