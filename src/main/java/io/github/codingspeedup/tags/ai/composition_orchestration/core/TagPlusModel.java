package io.github.codingspeedup.tags.ai.composition_orchestration.core;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
public class TagPlusModel extends BufferRange{

    private String template;
    private Properties arguments = new Properties();
    private String gateway;
    private String plus;

}
