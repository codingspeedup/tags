package tools.codegen.java

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.AnnotationDeclaration
import com.github.javaparser.ast.body.RecordDeclaration
import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import org.jetbrains.annotations.Nullable

@SuppressWarnings("unused")
class TypeGenerator {

    private static CompilationUnit createCU() {
        new CompilationUnit()
    }

    @Tool("Generates a standard Java Class")
    static String generateClass(
            @P("className") String className,
            @P(value = "extendsClass -- - the parent class name", required = false) @Nullable String extendsClass = null,
            @P(value = "implementsList", required = false) @Nullable List<String> implementsList = null
    ) {
        var cu = createCU()
        var type = cu.addClass(className, Modifier.Keyword.PUBLIC)
        if (extendsClass) {
            type.addExtendedType(extendsClass)
        }
        implementsList?.each { type.addImplementedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Interface")
    static String generateInterface(
            @P("interfaceName") String interfaceName,
            @P(value = "extendsList", required = false) @Nullable List<String> extendsList = null
    ) {
        var cu = createCU()
        var type = cu.addInterface(interfaceName, Modifier.Keyword.PUBLIC)
        extendsList?.each { type.addExtendedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Record")
    static String generateRecord(
            @P("recordName") String recordName,
            @P(value = "implementsList", required = false) @Nullable List<String> implementsList = null
    ) {
        var cu = createCU()
        var record = new RecordDeclaration()
        record.setName(recordName)
        record.setPublic(true)
        cu.addType(record)

        implementsList?.each { record.addImplementedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Enum")
    static String generateEnum(
            @P("enumName") String enumName,
            @P(value = "implementsList", required = false) @Nullable List<String> implementsList = null
    ) {
        var cu = createCU()
        var type = cu.addEnum(enumName, Modifier.Keyword.PUBLIC)
        implementsList?.each { type.addImplementedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Annotation type")
    static String generateAnnotation(
            @P("annotationName") String annotationName
    ) {
        var cu = createCU()
        var annotation = new AnnotationDeclaration()
        annotation.setName(annotationName)
        annotation.setPublic(true)
        cu.addType(annotation)
        cu.toString()
    }

    @Tool("Generates a Java Exception class")
    static String generateException(
            @P("exceptionName") String exceptionName = null,
            @P(value = "baseException", required = false) @Nullable String baseException = null
    ) {
        var cu = createCU()
        var type = cu.addClass(exceptionName, Modifier.Keyword.PUBLIC)
        // Using Elvis operator instead of Objects.requireNonNullElse
        type.addExtendedType(baseException ?: "Exception")
        cu.toString()
    }
}
