package io.github.codingspeedup.tags.ai.composition.reactive;

import com.intellij.openapi.project.Project;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.github.codingspeedup.tags.plugin.console.TagsConsoleService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.ai.composition.orchestration.core.BufferModel.ARG_PLACEHOLDER;

public class ToolboxApiSpecBuilder {

    private static final String TOOLS_PACKAGE_NAME = "tools";
    private static final String TOOLS_PACKAGE_PREFIX = TOOLS_PACKAGE_NAME + ".";

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "continue", "for", "new", "switch", "assert", "default", "if", "package", "synchronized",
            "boolean", "do", "goto", "private", "this", "break", "double", "implements", "protected", "throw",
            "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
            "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void",
            "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while",
            "null", "true", "false"
    );

    private static final List<String> ERASABLE_PREFIXES = List.of(
            String.class.getPackageName(),
            List.class.getPackageName(),
            BigDecimal.class.getPackageName()
    );

    private final Set<? extends Class<?>> tools;
    private final StringBuilder md = new StringBuilder();
    private final Set<String> importPackages = new HashSet<>();
    private final Set<Class<?>> discoveredPojos = new HashSet<>();
    private final Set<Class<?>> collectedPojos = new HashSet<>();

    private ToolboxApiSpecBuilder(Set<? extends Class<?>> tools) {
        this.tools = tools;
    }

    public static Optional<String> of(Project project, String... toolkit) {
        if (ArrayUtils.isEmpty(toolkit)) {
            return Optional.empty();
        }
        return of(project, List.of(toolkit));
    }

    public static Optional<String> of(Project project, List<String> toolkit) {
        if (CollectionUtils.isEmpty(toolkit)) {
            return Optional.empty();
        }

        ClassLoader classLoader;
        if (project == null) {
            classLoader = ToolboxApiSpecBuilder.class.getClassLoader();
        } else {
            var toolbox = ToolboxManagerService.getInstance(project);
            toolbox.reloadIfChanged();
            classLoader = toolbox.getActiveLoader();
        }

        var tools = toolkit.stream()
                .map(StringUtils::trimToNull)
                .map(toolFQN -> ARG_PLACEHOLDER.equals(toolFQN) ? null : toolFQN)
                .filter(Objects::nonNull)
                .map(toolFQN -> {
                    try {
                        return classLoader.loadClass(toolFQN);
                    } catch (ClassNotFoundException e) {
                        try {
                            return classLoader.loadClass(TOOLS_PACKAGE_PREFIX + toolFQN);
                        } catch (ClassNotFoundException ex) {
                            if (project != null) {
                                TagsConsoleService.getInstance(project).warn("Could not load `" + toolFQN + "'");
                            }
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(tools)) {
            return Optional.empty();
        }
        return Optional.of(new ToolboxApiSpecBuilder(tools).build().toString());
    }

    private StringBuilder build() {
        tools.forEach(this::collectMethods);

        if (!discoveredPojos.isEmpty()) {
            while (true) {
                var diff = new HashSet<>(discoveredPojos);
                diff.removeAll(collectedPojos);
                if (diff.isEmpty()) {
                    break;
                }
                var pojo = diff.iterator().next();
                collectDataStructure(pojo);
                collectedPojos.add(pojo);
            }
        }

        md.append("\n\n### Required Imports:\n```groovy\n");
        importPackages.stream()
                .sorted()
                .forEach(pName -> md.append("import ").append(pName).append(".*\n"));
        md.append("```");

        return md;
    }

    private void collectDataStructure(Class<?> pojoClass) {
        md.append("\n\n### POJO: ").append(pojoClass.getSimpleName());

        var superclass = pojoClass.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            md.append(" extends ").append(collectType(pojoClass.getGenericSuperclass()));
        }

        Arrays.stream(pojoClass.getDeclaredFields())
                .filter(field -> field.getAnnotation(F.class) != null)
                .sorted(Comparator.comparing(Field::getName))
                .forEach(field -> {
                    var f = field.getAnnotation(F.class);
                    md.append("\n- ").append(field.getName());
                    if (!f.required()) {
                        md.append("?");
                    }
                    md.append(" : ").append(collectType(field.getGenericType()));
                    var doc = StringUtils.trimToEmpty(f.value()).replaceAll("\\s+", " ");
                    if (!doc.isEmpty()) {
                        md.append(" // ").append(doc);
                    }
                });
    }

    private void collectMethods(Class<?> toolClass) {
        importPackages.add(toolClass.getPackageName());
        md.append("\n\n## API: ").append(toolClass.getSimpleName());
        Arrays.stream(toolClass.getMethods())
                .filter(method -> {
                    var modifiers = method.getModifiers();
                    return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)
                            && method.getAnnotation(Tool.class) != null;
                })
                .sorted(Comparator.comparing(Method::getName))
                .forEach(method -> {
                    var paramDocs = new ArrayList<String>();
                    var paramsPart = Arrays.stream(method.getParameters())
                            .map(param -> collectParameter(param, paramDocs))
                            .collect(Collectors.joining(", "));
                    var doc = collectToolDoc(method, paramDocs);

                    md.append("\n- ");
                    if (StringUtils.isNotBlank(doc)) {
                        md.append(doc).append("\n  "); // Doc on first line, signature indented on second
                    }
                    md.append(collectType(method.getGenericReturnType()))
                            .append(" ").append(method.getName())
                            .append("(").append(paramsPart).append(")");
                });
    }

    private String collectType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            var rawType = (Class<?>) parameterized.getRawType();
            var typeArgs = parameterized.getActualTypeArguments();

            var argsJoined = Arrays.stream(typeArgs)
                    .map(this::collectType) // Recursive call for nested generics like List<Map<String, Int>>
                    .collect(Collectors.joining(", "));

            return collectClassType(rawType) + "<" + argsJoined + ">";
        }

        if (type instanceof Class<?> clazz) {
            return collectClassType(clazz);
        }

        return type.getTypeName();
    }

    private String collectClassType(Class<?> javaType) {
        if (javaType.isArray()) {
            return collectClassType(javaType.getComponentType()) + "[]";
        }
        if (javaType.isPrimitive()) {
            return javaType.getSimpleName();
        }
        var packageName = javaType.getPackageName();
        if (StringUtils.isEmpty(packageName)) {
            return javaType.getSimpleName();
        }
        var isTool = packageName.equals(TOOLS_PACKAGE_NAME) || packageName.startsWith(TOOLS_PACKAGE_PREFIX);
        if (isTool) {
            discoveredPojos.add(javaType);
            importPackages.add(packageName);
            return javaType.getSimpleName();
        }
        if (ERASABLE_PREFIXES.contains(javaType.getPackageName())) {
            return javaType.getSimpleName();
        } else {
            importPackages.add(packageName);
            return javaType.getName();
        }
    }

    private String collectParameter(Parameter param, List<String> paramDocs) {
        var pDeclaration = new StringBuilder(collectType(param.getParameterizedType())).append(" ");
        var pMeta = param.getAnnotation(P.class);
        if (pMeta == null) {
            pDeclaration.append(param.getName());
        } else {
            var pNameDoc = collectParameterNameAndDocstring(param.getName(), pMeta.value());
            pDeclaration.append(pNameDoc.getLeft());
            if (StringUtils.isNotBlank(pNameDoc.getRight())) {
                paramDocs.add(pNameDoc.getLeft() + " - " + pNameDoc.getRight());
            }
            if (!pMeta.required()) {
                pDeclaration.append("?");
            }
        }
        return pDeclaration.toString();
    }

    private Pair<String, String> collectParameterNameAndDocstring(String paramName, String paramDoc) {
        paramDoc = StringUtils.trimToEmpty(paramDoc).replaceAll("\\s+", " ");
        if (paramDoc.isEmpty() || !Character.isJavaIdentifierStart(paramDoc.charAt(0))) {
            return Pair.of(paramName, paramDoc);
        }
        var i = 1;
        while (i < paramDoc.length() && Character.isJavaIdentifierPart(paramDoc.charAt(i))) {
            i++;
        }
        var candidate = paramDoc.substring(0, i);
        if (!KEYWORDS.contains(candidate)) {
            paramName = candidate;
            paramDoc = paramDoc.substring(i).trim();
            while (paramDoc.startsWith("-")) {
                paramDoc = paramDoc.substring(1).trim();
            }
        }
        return Pair.of(paramName, paramDoc);
    }

    private static String collectToolDoc(Method method, ArrayList<String> paramsDocs) {
        var summary = new ArrayList<String>();
        var tool = method.getAnnotation(Tool.class);
        if (tool != null && tool.value().length > 0) {
            Arrays.stream(tool.value())
                    .map(s -> StringUtils.trimToEmpty(s).replaceAll("\\s+", " "))
                    .filter(StringUtils::isNotEmpty)
                    .forEach(summary::add);
        }
        summary.addAll(paramsDocs);
        return summary.isEmpty() ? "" : "// " + String.join("; ", summary);
    }

}
