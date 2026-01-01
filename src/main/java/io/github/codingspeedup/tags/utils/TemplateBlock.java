package io.github.codingspeedup.tags.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
public class TemplateBlock extends TagsBlock {

    private String template;
    private Properties arguments = new Properties();
    private String gateway;
    private String plus;

}
