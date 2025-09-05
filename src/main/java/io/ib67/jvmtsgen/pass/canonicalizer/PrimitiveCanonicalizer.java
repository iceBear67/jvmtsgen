package io.ib67.jvmtsgen.pass.canonicalizer;

import io.ib67.jvmtsgen.pass.TypeCanonicalizePass;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSType;

public enum PrimitiveCanonicalizer implements TypeCanonicalizePass.TypeCanonicalizer {
    INSTANCE;
    @Override
    public TSType transformFromBottom(TSElement element, TSType type) {
        if (type instanceof TSType.TSClass claz) {
            switch (claz.name()) {
                case "Ljava/lang/Object;":
                    return TSType.TSPrimitive.ANY;
                case "Ljava/lang/String;":
                    return TSType.TSPrimitive.STRING;
                case "Ljava/lang/Integer;", "Ljava/lang/Long;", "Ljava/lang/Double;", "Ljava/lang/Float;",
                     "Ljava/lang/Byte;", "Ljava/lang/Short;", "Ljava/lang/Character;":
                    return TSType.TSPrimitive.NUMBER;
                case "Ljava/lang/Void;":
                    return TSType.TSPrimitive.VOID;
                case "Ljava/lang/Boolean;":
                    return TSType.TSPrimitive.BOOLEAN;
            }
        }
        return type;
    }
}
