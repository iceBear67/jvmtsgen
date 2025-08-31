package io.ib67.jvmtsgen.tsdef;

import io.ib67.jvmtsgen.TypeUtil;
import lombok.AllArgsConstructor;
import lombok.With;

import java.util.*;
import java.util.stream.Collectors;

public interface TSType {
    interface ParameterizedTSType extends TSType {
        Map<String, TSType> typeParam();
    }

    @With
    record TSUnion(
            TSType left,
            TSType right
    ) implements TSType {
        public TSUnion {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
        }

        @Override
        public String toString() {
            return left + " | " + right;
        }
    }

    @With
    record TSIntersection(
            TSType left,
            TSType right
    ) implements TSType {
        public TSIntersection {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
        }

        @Override
        public String toString() {
            return left + " & " + right;
        }
    }

    @With
    record TSFunction(
            boolean async,
            TSType returnType,
            Map<String, TSType> parameters,
            Map<String, TSType> typeParam
    ) implements ParameterizedTSType {
        public TSFunction {
            Objects.requireNonNull(returnType);
            if (parameters == null) parameters = new HashMap<>();
            if (typeParam == null) typeParam = new HashMap<>();
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            if (async) sb.append("async ");
            sb.append(TypeUtil.typeParamString(typeParam)).append("(").append(paramSet(parameters)).append(") => ").append(returnType);
            return sb.toString();
        }
    }

    /**
     * @param rawLiteral boolean, int, string, double, etc.
     */
    @With
    record TSLiteral(Object rawLiteral) implements TSType {
        @Override
        public String toString() {
            return rawLiteral.toString();
        }
    }

    @With
    record TSClass(String name, Map<String, TSType> typeParam) implements ParameterizedTSType {
        public TSClass {
            Objects.requireNonNull(name);
            if (typeParam == null) typeParam = new HashMap<>();
        }

        @Override
        public String toString() {
            if (typeParam.isEmpty()) return name;
            return name + "<" + typeParam.values().stream().map(TSType::toString).collect(Collectors.joining(", ")) + ">";
        }
    }

    @With
    record TSObject(
            Map<String, ObjectProperty> properties
    ) implements TSType {
        public record ObjectProperty(
                boolean readOnly,
                TSType type
        ) {
        }

        public TSObject() {
            this(null);
        }

        public TSObject {
            if (properties == null) properties = new HashMap<>();
        }

        public void addProperty(String key, TSType type, boolean readOnly) {
            properties.put(key, new ObjectProperty(readOnly, type));
        }

        @Override
        public String toString() {
            return "{" + properties.entrySet().stream()
                    .map(this::toEntryString)
                    .collect(Collectors.joining(", "))
                    + "}";
        }

        private String toEntryString(Map.Entry<String, ObjectProperty> it) {
            var prop = it.getValue();
            return (prop.readOnly() ? "readonly " : "") + it.getKey() + ": " + prop.type;
        }
    }

    static TSType nullable(TSType type) {
        return new TSUnion(type, TSPrimitive.NULL);
    }

    @With
    record TSArray(TSType element) implements TSType {
        public TSArray {
            Objects.requireNonNull(element);
        }

        @Override
        public String toString() {
            return element + "[]";
        }
    }

    @With
    record TSTypeVar(String identifier) implements TSType {
        public static final TSTypeVar UNKNOWN = new TSTypeVar("?");

        public TSTypeVar {
            Objects.requireNonNull(identifier);
        }

        @Override
        public String toString() {
            return identifier;
        }
    }

    @With
    record TSBounded(
            TSTypeVar typeVar,
            Indicator indicator,
            TSType bound
    ) implements TSType {
        public enum Indicator {
            EXTENDS
        }

        public TSBounded {
            Objects.requireNonNull(bound);
            Objects.requireNonNull(indicator);
            if (typeVar == null) typeVar = TSTypeVar.UNKNOWN;
        }

        @Override
        public String toString() {
            return typeVar + " extends " + bound; //todo fix
        }
    }

    @AllArgsConstructor
    enum TSPrimitive implements TSType {
        ANY("any"),
        UNKNOWN("unknown"),
        VOID("void"),
        NEVER("never"),
        NULL("null"),
        NUMBER("number"),
        BIGINT("bigint"),
        STRING("string"),
        BOOLEAN("boolean");

        public final String keyword;


        @Override
        public String toString() {
            return keyword;
        }
    }


    static String paramSet(Map<String, TSType> types) {
        return types.entrySet().stream()
                .map(it -> it.getKey() + ": " + it.getValue())
                .collect(Collectors.joining(", "));
    }
}
