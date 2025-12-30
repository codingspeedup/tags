package io.github.codingspeedup.tags.engine.core;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class TemplateBlock extends TagsBlock {

    private String template;
    private Map<String, String> arguments = new LinkedHashMap<>();
    private String generation;
    private String plus;

}
