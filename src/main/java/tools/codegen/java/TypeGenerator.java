package tools.codegen.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public class TypeGenerator {

    private CompilationUnit createCU() {
        return new CompilationUnit();
    }

    @Tool("Generates a standard Java Class")
    public String generateClass(
            @P("The name of the class") String name,
            @P(value = "Superclass to extend", required = false) @Nullable String extendsClass,
            @P(value = "Interfaces to implement", required = false) @Nullable List<String> implementsList
    ) {
        var cu = createCU();
        var type = cu.addClass(name, Modifier.Keyword.PUBLIC);
        if (extendsClass != null) {
            type.addExtendedType(extendsClass);
        }
        if (implementsList != null) {
            implementsList.forEach(type::addImplementedType);
        }
        return cu.toString();
    }

    @Tool("Generates a Java Interface")
    public String generateInterface(
            @P("The name of the interface") String name,
            @P(value = "Interfaces to extend", required = false) @Nullable List<String> extendsList
    ) {
        var cu = createCU();
        var type = cu.addInterface(name, Modifier.Keyword.PUBLIC);
        if (extendsList != null) {
            extendsList.forEach(type::addExtendedType);
        }
        return cu.toString();
    }

    @Tool("Generates a Java Record")
    public String generateRecord(
            @P("The name of the record") String name,
            @P(value = "Interfaces to implement", required = false) @Nullable List<String> implementsList
    ) {
        var cu = createCU();
        var record = new RecordDeclaration();
        record.setName(name);
        record.setPublic(true);
        cu.addType(record);

        if (implementsList != null) {
            implementsList.forEach(record::addImplementedType);
        }
        return cu.toString();
    }

    @Tool("Generates a Java Enum")
    public String generateEnum(
            @P("The name of the enum") String name,
            @P(value = "Interfaces to implement", required = false) @Nullable List<String> implementsList
    ) {
        var cu = createCU();
        var type = cu.addEnum(name, Modifier.Keyword.PUBLIC);
        if (implementsList != null) {
            implementsList.forEach(type::addImplementedType);
        }
        return cu.toString();
    }

    @Tool("Generates a Java Annotation type")
    public String generateAnnotation(
            @P("The name of the annotation") String name
    ) {
        var cu = createCU();
        var annotation = new AnnotationDeclaration();
        annotation.setName(name);
        annotation.setPublic(true);
        cu.addType(annotation);
        return cu.toString();
    }

    @Tool("Generates a Java Exception class")
    public String generateException(
            @P("The name of the exception") String name,
            @P(value = "Base exception to extend (defaults to Exception)", required = false) @Nullable String baseException
    ) {
        var cu = createCU();
        var type = cu.addClass(name, Modifier.Keyword.PUBLIC);
        type.addExtendedType(Objects.requireNonNullElse(baseException, "Exception"));
        return cu.toString();
    }

}