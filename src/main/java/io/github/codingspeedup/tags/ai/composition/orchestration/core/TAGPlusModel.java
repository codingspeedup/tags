package io.github.codingspeedup.tags.ai.composition.orchestration.core;

import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@Getter
@Setter
public class TAGPlusModel extends BufferRange{

    private String template;
    private Properties arguments = new Properties();
    private String gateway;
    private String plus;

}
