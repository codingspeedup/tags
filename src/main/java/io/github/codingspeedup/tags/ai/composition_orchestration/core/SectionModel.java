package io.github.codingspeedup.tags.ai.composition_orchestration.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionModel extends BufferRange {

    private String name;

    public String getContent(String fileContent) {
        var sectionStart = BufferModel.indexOfEol(fileContent, getFromOffset());
        var sectionEnd = BufferModel.indexOfBol(fileContent, getToOffset());
        return fileContent.substring(sectionStart + 1, sectionEnd);
    }

}
