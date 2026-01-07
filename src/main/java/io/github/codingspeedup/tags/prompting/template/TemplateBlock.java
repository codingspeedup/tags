package io.github.codingspeedup.tags.prompting.template;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
public class TemplateBlock extends PromptBlock {

    private String template;
    private Properties arguments = new Properties();
    private String gateway;
    private String plus;

}
