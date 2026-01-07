package io.github.codingspeedup.tags.prompting.template;

import io.github.codingspeedup.tags.utils.FileTypeModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionBlock extends PromptBlock {

    private String name;

    public String getContent(String fileContent) {
        var sectionStart = FileTypeModel.indexOfEol(fileContent, getFromOffset());
        var sectionEnd = FileTypeModel.indexOfBol(fileContent, getToOffset());
        return fileContent.substring(sectionStart + 1, sectionEnd);
    }

}
