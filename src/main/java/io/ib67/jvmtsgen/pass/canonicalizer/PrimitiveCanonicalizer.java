package io.ib67.jvmtsgen.pass.canonicalizer;

import io.ib67.jvmtsgen.JavaTypes;
import io.ib67.jvmtsgen.pass.TransformerContext;
import io.ib67.jvmtsgen.pass.TypeCanonicalizePass;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSType;

public enum PrimitiveCanonicalizer implements TypeCanonicalizePass.TypeCanonicalizer {
    INSTANCE;
    @Override
    public TSType transformFromBottom(TransformerContext context, TSElement element, TSType type) {
        if (type instanceof TSType.TSClass claz) {
            switch (claz.name()) {
                case JavaTypes.J_OBJECT:
                    return TSType.TSPrimitive.ANY;
                case JavaTypes.J_STRING:
                    return TSType.TSPrimitive.STRING;
                case JavaTypes.J_DOUBLE, JavaTypes.J_FLOAT, JavaTypes.J_BYTE, JavaTypes.J_SHORT,
                     JavaTypes.J_CHAR, JavaTypes.J_INTEGER, JavaTypes.J_LONG:
                    return TSType.TSPrimitive.NUMBER;
                case JavaTypes.J_VOID:
                    return TSType.TSPrimitive.VOID;
                case JavaTypes.J_BOOLEAN:
                    return TSType.TSPrimitive.BOOLEAN;
            }
        }
        return type;
    }
}
