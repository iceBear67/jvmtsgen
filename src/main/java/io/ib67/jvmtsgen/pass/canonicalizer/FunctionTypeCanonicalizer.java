package io.ib67.jvmtsgen.pass.canonicalizer;

import io.ib67.jvmtsgen.JavaTypes;
import io.ib67.jvmtsgen.pass.TransformerContext;
import io.ib67.jvmtsgen.pass.TypeCanonicalizePass;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSType;
import io.ib67.kiwi.Diag;
import io.ib67.kiwi.routine.Uni;
import lombok.RequiredArgsConstructor;

import java.lang.classfile.AttributeMapper;
import java.lang.classfile.Attributes;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.reflect.AccessFlag;
import java.util.*;

@RequiredArgsConstructor
public class FunctionTypeCanonicalizer implements TypeCanonicalizePass.TypeCanonicalizer {
    protected final boolean expandUserFunctionalInterface;

    @Override
    public TSType transformFromBottom(TransformerContext context, TSElement element, TSType type) { //todo support for any functional interfaces.
        if (!(type instanceof TSType.TSClass claz)) return type;
        var typeParams = List.copyOf(claz.typeParam().entrySet());
        // The type parameters can be stripped safely since they are mostly provided by the belonging method signature.
        switch (claz.name()) {
            case JavaTypes.JUF_FUNCTION:
                return new TSType.TSFunction(typeParams.get(1).getValue(), Map.ofEntries(typeParams.get(0)), Map.of());
            case JavaTypes.JUF_PREDICATE:
                return new TSType.TSFunction(TSType.TSPrimitive.BOOLEAN, Map.ofEntries(typeParams.get(0)), Map.of());
            case JavaTypes.JUF_CONSUMER:
                return new TSType.TSFunction(TSType.TSPrimitive.VOID, Map.ofEntries(typeParams.getFirst()), Map.of());
            case JavaTypes.JUF_SUPPLIER:
                return new TSType.TSFunction(typeParams.getFirst().getValue(), Map.of(), Map.of());
            case JavaTypes.JUF_UNARY_OPERATOR:
                return new TSType.TSFunction(typeParams.getFirst().getValue(), Map.ofEntries(typeParams.getFirst()), Map.of());
            default:
                break;
        }
        if (!expandUserFunctionalInterface) return type;
        var _clazMod = context.getClassModels().getByDescriptor(claz.name());
        if (_clazMod.isEmpty()) return type;
        var clasMod = _clazMod.get();
        if (!clasMod.flags().has(AccessFlag.INTERFACE)) return type;
        // check if it is functional interface
        var nonDefaultMethods = Uni.from(clasMod.methods()::forEach)
                .filter(it -> !it.flags().has(AccessFlag.STATIC) && it.flags().has(AccessFlag.ABSTRACT))
                .toList();
        if (nonDefaultMethods.size() != 1) {
            return type;
        }
        var nonDefaultMethod = nonDefaultMethods.getFirst();
        // 1. typevar -> (order -> actual type)
        // 2. expand
        var typeMap = new HashMap<String, TSType>();
        clasMod.findAttribute(Attributes.signature()).map(SignatureAttribute::asClassSignature)
                .ifPresent(classSignature -> {
                    var params = classSignature.typeParameters();
                    var actual = new ArrayList<>(claz.typeParam().values());
                    for (int i = 0; i < params.size(); i++) {
                        typeMap.put(params.get(i).identifier(), actual.get(i));
                    }
                });

        var fnType = context.getModelBuilder().writeMethod(nonDefaultMethod).getType();
        if(!typeMap.isEmpty()){
            for (Map.Entry<String, TSType> entry : fnType.parameters().entrySet()) {
                if(entry.getValue() instanceof TSType.TSTypeVar(var id)){
                    entry.setValue(typeMap.get(id));
                }
            }
        }
        return fnType;
    }
}
