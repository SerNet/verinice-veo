# gendocs

tool to generate CustomAspect & -Links documentation
generates AsciiDoc from veo domain templates using freemarker

┌─┐┌─┐┌┐┌┌┬┐┌─┐┌─┐┌─┐
│ ┬├┤ │││ │││ ││  └─┐
└─┘└─┘┘└┘─┴┘└─┘└─┘└─┘

## Usage

```sh
java -jar gendocs.jar -d ~/IdeaProjects/verinice-veo/domaintemplates/dsgvo/ | asciidoctor --doctype book - > customschemadocs.html
```
