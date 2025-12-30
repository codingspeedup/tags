package io.github.codingspeedup.tags.engine.tags;

import io.github.codingspeedup.tags.engine.core.FileTypeModel;
import io.github.codingspeedup.tags.engine.core.GenerationResponse;
import io.github.codingspeedup.tags.engine.core.GenerationSink;
import io.github.codingspeedup.tags.engine.core.TagsBlock;
import lombok.AllArgsConstructor;

import java.util.Locale;

@AllArgsConstructor
public class TagsTemplateEditor {

    private final FileTypeModel fileModel;
    private final String fileContent;

    public GenerationResponse insertNewTemplate(int offset) {
        int tOffset = fileModel.parseSections(fileContent).values().stream()
                .filter(section -> section.contains(offset))
                .findFirst()
                .map(TagsBlock::getFromOffset)
                .orElse(FileTypeModel.indexOfBol(fileContent, offset));

        @SuppressWarnings("all")
        var newContent = new StringBuilder(fileContent.length() + 1024);
        newContent.append(fileContent, 0, tOffset);
        newContent.append(fileModel.getTPrefix()).append("Explain {{concept}}").append("\n");
        newContent.append(fileModel.getAPrefix()).append("concept=abstraction").append("\n");
        newContent.append(fileModel.getGPrefix()).append(GenerationSink.CLIPBOARD.name().toLowerCase(Locale.ROOT)).append("\n");
        newContent.append(fileContent, tOffset, fileContent.length());

        var response = new GenerationResponse();
        response.setOutputChannel(GenerationSink.REPLACE_CONTENT);
        response.setGeneratedContent(newContent.toString());
        response.setStartOffset(tOffset);
        response.setEndOffset(tOffset);
        return response;
    }

}
