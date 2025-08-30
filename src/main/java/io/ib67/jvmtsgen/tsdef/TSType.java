package io.ib67.jvmtsgen.tsdef;

import io.ib67.jvmtsgen.TypeUtil;
import lombok.AllArgsConstructor;
import lombok.With;

import java.util.*;
import java.util.stream.Collectors;

public interface TSType {
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
    ) implements TSType {
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
    record TSClass(String name, Map<String, TSType> typeParameters) implements TSType {
        public TSClass {
            Objects.requireNonNull(name);
            if (typeParameters == null) typeParameters = new HashMap<>();
        }

        @Override
        public String toString() {
            if (typeParameters.isEmpty()) return name;
            return name + "<" + typeParameters.values().stream().map(TSType::toString).collect(Collectors.joining(", ")) + ">";
        }
    }

    @With
    record TSObject(Map<String, TSType> properties) implements TSType {
        public TSObject {
            if (properties == null) properties = new HashMap<>();
        }

        @Override
        public String toString() {
            return "{" + paramSet(properties) + "}";
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
