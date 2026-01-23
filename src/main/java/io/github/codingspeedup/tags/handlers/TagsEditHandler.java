package io.github.codingspeedup.tags.handlers;

import com.intellij.openapi.project.Project;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel;
import io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferRange;
import io.github.codingspeedup.tags.ai.deployment.orchestration.ResponseGateway;
import io.github.codingspeedup.tags.ai.primitives.reactive.PromptRef;
import io.github.codingspeedup.tags.minions.ProjectPromptLibraryProvider;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

import static io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel.*;

@AllArgsConstructor
public class TagsEditHandler {

    private final BufferModel ftModel;
    private final String fileContent;

    public TagsResult insertNewTemplate(Project project, int offset, PromptRef promptRef) {
        int tOffset = ftModel.getSections(fileContent).values().stream()
                .filter(section -> section.contains(offset))
                .findFirst()
                .map(BufferRange::getFromOffset)
                .orElse(indexOfBol(fileContent, offset));

        var pLib = new ProjectPromptLibraryProvider(project).load(promptRef).orElseThrow();
        var pDesc = pLib.getPromptDescription(promptRef.getId()).orElseThrow();

        var newContent = new StringBuilder(fileContent.length() + 1024);

        newContent.append(fileContent, 0, tOffset);

        // T
        newContent.append(ftModel.getTPrefix()).append(promptRef).append("\n");

        // A
        for (var varName : pLib.getVariables(promptRef.getId())) {
            var varValue = ARG_PLACEHOLDER;
            if (pLib.getDefaults().containsKey(varName)) {
                varValue = String.valueOf(pLib.getDefaults().get(varName));
            }
            newContent.append(ftModel.getAPrefix()).append(varName).append(ARG_SEPARATOR).append(varValue).append("\n");
        }

        // G
        var newSections = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(pDesc.gateway())) {
            var existingSections = new HashSet<String>();
            pDesc.gateway().forEach(gateway -> {
                var gatewayType = ResponseGateway.resolveGateway(gateway);
                if (gatewayType == ResponseGateway.CONTENT) {
                    var nextSectionName = gateway.substring(SECTION_REF_MARKER.length()).trim();
                    if (SECTION_ROOT_ID.equals(nextSectionName)) {
                        if (existingSections.isEmpty()) {
                            existingSections.addAll(ftModel.getSections(fileContent).keySet());
                        }
                        nextSectionName = getNextSectionName(existingSections);
                        newSections.add(nextSectionName);
                        existingSections.add(nextSectionName);
                    }
                    newContent.append(ftModel.getGPrefix()).append(SECTION_REF_MARKER).append(nextSectionName).append("\n");
                } else {
                    newContent.append(ftModel.getGPrefix()).append(gatewayType.name().toLowerCase(Locale.ROOT)).append("\n");
                }
            });
        }

        // +
        if (CollectionUtils.isNotEmpty(pDesc.plus())) {
            pDesc.plus().forEach(plus -> newContent.append(ftModel.getPlusPrefix()).append(plus).append("\n"));
        }

        // S
        if (CollectionUtils.isNotEmpty(newSections)) {
            var sPrefix = ftModel.getSPrefix();
            newSections.forEach(newSectionName ->
                    newContent.append(sPrefix).append(buildSectionStartMarker(newSectionName)).append("\n")
                            .append(sPrefix).append(buildSectionEndMarker(newSectionName)).append("\n"));
        }

        newContent.append(fileContent, tOffset, fileContent.length());

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
        tagsResult.setContent(newContent.toString());
        tagsResult.setStartOffset(tOffset);
        tagsResult.setEndOffset(tOffset);
        return tagsResult;
    }

    public TagsResult insertNewSection(int fromOffset, int toOffset) {
        var existingSections = ftModel.getSections(fileContent).keySet();
        var nextSectionName = getNextSectionName(existingSections);
        var section = ftModel.insertSection(nextSectionName, fileContent, fromOffset, toOffset);

        var tagsResult = new TagsResult(ResponseGateway.CONTENT);
        tagsResult.setContent(section.getLeft());
        tagsResult.setStartOffset(section.getMiddle());
        tagsResult.setEndOffset(section.getRight());
        return tagsResult;
    }

    public Optional<TagsResult> stripTags(int offset) {
        var newContent = ftModel.stripTags(fileContent);
        if (newContent.isPresent()) {
            var tagsResult = new TagsResult(ResponseGateway.CONTENT);
            tagsResult.setContent(newContent.get());
            tagsResult.setStartOffset(offset);
            tagsResult.setEndOffset(offset);
            return Optional.of(tagsResult);
        }
        return Optional.empty();
    }

}
