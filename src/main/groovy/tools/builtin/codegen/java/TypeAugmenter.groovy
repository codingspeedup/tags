package tools.builtin.codegen.java

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.RecordDeclaration
import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool

@SuppressWarnings("unused")
class TypeAugmenter {

    @Tool("Adds a field to a Java record")
    static String addRecordField(
            @P("code - the source code of the compilation unit") String code,
            @P("param - the parameter specification") Parameter param
    ) {
        StaticJavaParser.getParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
        var cu = StaticJavaParser.parse(code)
        cu.findFirst(RecordDeclaration.class).ifPresent(record -> {
            record.addParameter(param.type, param.name)
        })
        return cu.toString()
    }

}
