package io.github.codingspeedup.tags.utils;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.codingspeedup.tags.utils.PromptDesc.VAR_PLACEHOLDER;

public class PromptApiSpecBuilder {

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

    private PromptApiSpecBuilder(Set<? extends Class<?>> tools) {
        this.tools = tools;
    }

    public static Optional<String> of(String... toolkit) {
        if (ArrayUtils.isEmpty(toolkit)) {
            return Optional.empty();
        }
        return of(List.of(toolkit));
    }

    public static Optional<String> of(List<String> toolkit) {
        if (CollectionUtils.isEmpty(toolkit)) {
            return Optional.empty();
        }
        var tools = toolkit.stream()
                .map(StringUtils::trimToNull)
                .map(toolFQN -> VAR_PLACEHOLDER.equals(toolFQN) ? null : toolFQN)
                .filter(Objects::nonNull)
                .map(toolFQN -> {
                    try {
                        return Class.forName(toolFQN);
                    } catch (ClassNotFoundException e) {
                        try {
                            return Class.forName(TOOLS_PACKAGE_PREFIX + toolFQN);
                        } catch (ClassNotFoundException ex) {
                            // Ignore
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(tools)) {
            return Optional.empty();
        }
        return Optional.of(new PromptApiSpecBuilder(tools).build().toString());
    }

    private StringBuilder build() {
        tools.forEach(this::collectMethods);

        md.append("\n\n### Required Imports:\n```groovy\n");
        importPackages.stream()
                .sorted()
                .forEach(pName -> md.append("import ").append(pName).append(".*\n"));
        md.append("```");

        return md;
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
            return javaType.getSimpleName();
        }
        return ERASABLE_PREFIXES.contains(javaType.getPackageName())
                ? javaType.getSimpleName()
                : javaType.getName();
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
