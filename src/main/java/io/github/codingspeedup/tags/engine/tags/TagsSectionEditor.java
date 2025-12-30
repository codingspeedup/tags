package io.github.codingspeedup.tags.engine.tags;

import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import io.github.codingspeedup.tags.engine.core.GenerationSink;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TagsSectionEditor {

    private final FileTypeModel fileModel;
    private final String fileContent;

    public GenerationResponse insertNewSection(int fromOffset, int toOffset) {
        var sPrefix = fileModel.getSPrefix();
        var existingSections = fileModel.parseSections(fileContent).keySet();

        var sectionIndex = 2;
        var newSectionName = "section-1";
        while (existingSections.contains(newSectionName)) {
            newSectionName = "section-" + sectionIndex++;
        }

        fromOffset = FileTypeModel.indexOfBol(fileContent, fromOffset);
        toOffset = FileTypeModel.indexOfEol(fileContent, toOffset);

        var newContent = new StringBuilder(fileContent.length() + 64);
        newContent.append(fileContent, 0, fromOffset);
        newContent.append(sPrefix).append("<").append(newSectionName).append(">\n");
        var startOffset = newContent.length();
        newContent.append(fileContent, fromOffset, toOffset);
        var endOffset = newContent.length();
        newContent.append("\n").append(sPrefix).append("</").append(newSectionName).append(">\n");
        newContent.append(fileContent,  toOffset, fileContent.length());

        var response = new GenerationResponse();
        response.setOutputChannel(GenerationSink.REPLACE_CONTENT);
        response.setGeneratedContent(newContent.toString());
        response.setStartOffset(startOffset);
        response.setEndOffset(endOffset);
        return response;
    }

}
