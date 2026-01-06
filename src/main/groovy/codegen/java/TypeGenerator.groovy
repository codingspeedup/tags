package codegen.java

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
            @P("The name of the class") String name,
            @P(value = "Superclass to extend", required = false) @Nullable String extendsClass,
            @P(value = "Interfaces to implement", required = false) @Nullable List<String> implementsList
    ) {
        var cu = createCU()
        var type = cu.addClass(name, Modifier.Keyword.PUBLIC)
        if (extendsClass) {
            type.addExtendedType(extendsClass)
        }
        implementsList?.each { type.addImplementedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Interface")
    static String generateInterface(
            @P("The name of the interface") String name,
            @P(value = "Interfaces to extend", required = false) @Nullable List<String> extendsList
    ) {
        var cu = createCU()
        var type = cu.addInterface(name, Modifier.Keyword.PUBLIC)
        extendsList?.each { type.addExtendedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Record")
    static String generateRecord(
            @P("The name of the record") String name,
            @P(value = "Interfaces to implement", required = false) @Nullable List<String> implementsList
    ) {
        var cu = createCU()
        var record = new RecordDeclaration()
        record.setName(name)
        record.setPublic(true)
        cu.addType(record)

        implementsList?.each { record.addImplementedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Enum")
    static String generateEnum(
            @P("The name of the enum") String name,
            @P(value = "Interfaces to implement", required = false) @Nullable List<String> implementsList
    ) {
        var cu = createCU()
        var type = cu.addEnum(name, Modifier.Keyword.PUBLIC)
        implementsList?.each { type.addImplementedType(it) }
        cu.toString()
    }

    @Tool("Generates a Java Annotation type")
    static String generateAnnotation(
            @P("The name of the annotation") String name
    ) {
        var cu = createCU()
        var annotation = new AnnotationDeclaration()
        annotation.setName(name)
        annotation.setPublic(true)
        cu.addType(annotation)
        cu.toString()
    }

    @Tool("Generates a Java Exception class")
    static String generateException(
            @P("The name of the exception") String name,
            @P(value = "Base exception to extend (defaults to Exception)", required = false) @Nullable String baseException
    ) {
        var cu = createCU()
        var type = cu.addClass(name, Modifier.Keyword.PUBLIC)
        // Using Elvis operator instead of Objects.requireNonNullElse
        type.addExtendedType(baseException ?: "Exception")
        cu.toString()
    }
}
