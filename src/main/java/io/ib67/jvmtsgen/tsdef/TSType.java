package io.ib67.jvmtsgen.tsdef;

import io.ib67.jvmtsgen.TypeUtil;
import io.ib67.kiwi.routine.Uni;
import lombok.AllArgsConstructor;
import lombok.With;

import java.util.*;
import java.util.stream.Collectors;

public interface TSType {
    interface ParameterizedTSType extends TSType {
        Map<String, TSType> typeParam();
    }

    default Uni<TSType> traverse(){
        return Uni.of(this);
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
        public Uni<TSType> traverse() {
            return c -> {
                left.traverse().onItem(c);
                right.traverse().onItem(c);
            };
        }

        @Override
        public String toString() {
            var _left = (left instanceof TSFunction) ? "("+ left +")" : left.toString();
            var _right = (right instanceof TSFunction) ? "("+ right +")" : right.toString();
            return _left + " | " + _right;
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
        public Uni<TSType> traverse() {
            return c -> {
                left.traverse().onItem(c);
                right.traverse().onItem(c);
            };
        }

        @Override
        public String toString() {
            return left + " & " + right;
        }
    }

    @With
    record TSFunction(
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
        public Uni<TSType> traverse() {
            return c -> {
                typeParam.values().forEach(i -> i.traverse().onItem(c));
                parameters.values().forEach(i -> i.traverse().onItem(c));
            };
        }

        @Override
        public String toString() {
            return TypeUtil.typeParamString(typeParam) + "(" + TypeUtil.paramSetString(parameters) + ") => " + returnType;
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
        public Uni<TSType> traverse() {
            return c -> typeParam.values().forEach(i -> i.traverse().onItem(c));
        }

        public String toRawSignature(){
            if (typeParam.isEmpty()) return name;
            return name + "<" + String.join(", ", typeParam.keySet()) + ">";
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
        @With
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
        public Uni<TSType> traverse() {
            return c -> properties.values().forEach(i -> i.type().traverse().onItem(c));
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
        public Uni<TSType> traverse() {
            return element.traverse();
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
        public Uni<TSType> traverse() {
            return c -> {
                typeVar.traverse().onItem(c);
                bound.traverse().onItem(c);
            };
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


}
