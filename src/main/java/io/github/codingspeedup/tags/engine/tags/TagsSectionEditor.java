package io.github.codingspeedup.tags.engine.tags;

import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.core.TagsResult;
import io.github.codingspeedup.tags.engine.core.ActionResultGateway;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TagsSectionEditor {

    private final FileTypeModel ftModel;
    private final String fileContent;

    public TagsResult insertNewSection(int fromOffset, int toOffset) {
        var sPrefix = ftModel.getSPrefix();
        var existingSections = ftModel.getSections(fileContent).keySet();

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

        var tagsResult = new TagsResult();
        tagsResult.setGateway(ActionResultGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(startOffset);
        tagsResult.setEndOffset(endOffset);
        return tagsResult;
    }

}
