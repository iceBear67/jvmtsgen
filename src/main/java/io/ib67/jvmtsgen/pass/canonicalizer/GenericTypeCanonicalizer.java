package io.ib67.jvmtsgen.pass.canonicalizer;

import io.ib67.jvmtsgen.pass.TypeCanonicalizePass;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSType;

import java.util.Objects;

public enum GenericTypeCanonicalizer implements TypeCanonicalizePass.TypeCanonicalizer {
    INSTANCE;
    @Override
    public TSType transformFromBottom(TSElement element, TSType type) {
        if (Objects.requireNonNull(type) instanceof TSType.TSBounded(var typeVar, var indicator, var bound)) {
            if (indicator == TSType.TSBounded.Indicator.EXTENDS) {
                if (bound == TSType.TSPrimitive.ANY) {
                    return typeVar;
                } else if (bound == TSType.TSPrimitive.UNKNOWN) {
                    return bound;
                }
            }
        }
        return type;
    }
}
