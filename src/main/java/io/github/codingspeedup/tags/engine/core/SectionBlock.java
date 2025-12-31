package io.github.codingspeedup.tags.engine.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionBlock extends TagsBlock {

    private String name;

    public String getContent(String fileContent) {
        var sectionStart = FileTypeModel.indexOfEol(fileContent, getFromOffset());
        var sectionEnd = FileTypeModel.indexOfBol(fileContent, getToOffset());
        return fileContent.substring(sectionStart + 1, sectionEnd);
    }

}
