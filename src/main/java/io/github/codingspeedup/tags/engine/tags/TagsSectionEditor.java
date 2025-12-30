package io.github.codingspeedup.tags.engine.tags;

import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import io.github.codingspeedup.tags.engine.core.GenerationSink;
import lombok.AllArgsConstructor;

import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class TagsSectionEditor {

    private final FileTypeModel fileModel;
    private final String fileContent;

    public GenerationResponse insertNewSection(int fromOffset, int toOffset) {
        var prefix = fileModel.getSPrefix();
        var existingSections = fileContent.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && line.startsWith(prefix))
                .map(line -> fileModel.s(line.substring(prefix.length())))
                .filter(Objects::nonNull)
                .map(SModel::name) // Assuming SModel has a name() method
                .collect(Collectors.toSet());

        var sectionIndex = 2;
        var newSectionName = "section-1";
        while (existingSections.contains(newSectionName)) {
            newSectionName = "section-" + sectionIndex++;
        }

        var lfPos = fileContent.lastIndexOf('\n', fromOffset - 1);
        fromOffset = (lfPos < 0) ? 0 : lfPos + 1;

        lfPos = fileContent.indexOf('\n', toOffset);
        toOffset = (lfPos < 0) ? fileContent.length() : lfPos;

        var newContent = new StringBuilder(fileContent.length() + 64);
        newContent.append(fileContent, 0, fromOffset);
        newContent.append(prefix).append("<").append(newSectionName).append(">\n");
        var startOffset = newContent.length();
        newContent.append(fileContent, fromOffset, toOffset);
        var endOffset = newContent.length();
        newContent.append("\n").append(prefix).append("</").append(newSectionName).append(">\n");
        var restOffset = (toOffset < fileContent.length() && fileContent.charAt(toOffset) == '\n')
                ? toOffset + 1
                : toOffset;
        newContent.append(fileContent.substring(restOffset));

        var response = new GenerationResponse();
        response.setOutputChannel(GenerationSink.REPLACE_CONTENT);
        response.setGeneratedContent(newContent.toString());
        response.setStartOffset(startOffset);
        response.setEndOffset(endOffset);
        return response;
    }

}
