package tools.builtin.codegen.java

import groovy.transform.Canonical
import io.github.codingspeedup.tags.ai.composition.reactive.F

@Canonical
class Parameter {

    @F(value = "The variable name used in the generated code", required = true)
    String name

    @F(value = "The fully qualified class name or primitive type", required = true)
    String type

}
