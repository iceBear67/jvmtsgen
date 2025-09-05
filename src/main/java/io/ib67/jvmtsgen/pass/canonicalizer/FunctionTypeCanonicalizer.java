package io.ib67.jvmtsgen.pass.canonicalizer;

import io.ib67.jvmtsgen.pass.TypeCanonicalizePass;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSType;

import java.util.List;
import java.util.Map;

public enum FunctionTypeCanonicalizer implements TypeCanonicalizePass.TypeCanonicalizer {
    INSTANCE;

    @Override
    public TSType transformFromBottom(TSElement element, TSType type) {
        if (!(type instanceof TSType.TSClass claz)) return type;
        var typeParams = List.copyOf(claz.typeParam().entrySet());
        switch (claz.name()){
            case "Ljava/util/function/Function;"://todo
                return new TSType.TSFunction(typeParams.get(1).getValue(), Map.ofEntries(typeParams.get(0)), claz.typeParam());
        }
        return type;
    }
}
