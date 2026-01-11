package io.github.codingspeedup.tags.ai.composition.reactive;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolboxUtl {

    public static final String TOOLBOX_EXTENSION = ".groovy";

    public static String buildToolboxFileName(int version) {
        return String.format("Toolbox%d%s", version, TOOLBOX_EXTENSION);
    }

    public static String buildSampleToolbox(String packageName, String toolboxName) {
        return "package " + packageName + "\n" +
                "\n" +
                "import groovy.transform.Canonical\n" +
                "import " + F.class.getName() + "\n" +
                "import " + P.class.getName() + "\n" +
                "import " + Tool.class.getName() + "\n" +
                "\n" +
                "@Canonical\n" +
                "class " + toolboxName + "Value {\n" +
                "\n" +
                "    @F(value = \"name field description\", required = true)\n" +
                "    String name\n" +
                "\n" +
                "    @F(value = \"value field description\", required = false)\n" +
                "    String value\n" +
                "\n" +
                "}\n" +
                "\n" +
                "class " + toolboxName + " {\n" +
                "\n" +
                "    @Tool(\"tool description\")\n" +
                "    static String sampleTool(\n" +
                "        @P(value = \"mandatoryArg - parameter description\", required = true) String mandatoryArg,\n" +
                "        @P(value = \"optionalArg - parameter description\", required = false) String optionalArg\n" +
                "    ) {\n" +
                "        return \"${mandatoryArg}${optionalArg ?: ''}\"\n" +
                "    }\n" +
                "\n" +
                "}\n";
    }

}
