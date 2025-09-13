package io.ib67.jvmtsgen.pass.nameresolver;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OverloadMergePass implements OverloadPass {
    @Nullable
    @Override
    public TSMethod tryMergeOverload(TSElement.TSCompoundElement clazz, List<TSElement> _value) {
        TSType retType = null;
        int parametersCount = -1;
        List<TSType> typeParams = null;
        Set<TSModifier> modifiers = null;
        var value = _value.stream().filter(it -> it instanceof TSMethod).map(it -> (TSMethod) it).toList();
        for (var method : value) {
            if (modifiers == null) modifiers = method.getModifiers();
            if (!modifiers.equals(method.getModifiers())) return null;

            if (retType == null) retType = method.getType().returnType();
            if (parametersCount == -1) parametersCount = Math.max(1, method.getType().parameters().size());
            var _typeParams = method.getType().typeParam().values();
            if (typeParams == null) typeParams = new ArrayList<>(_typeParams);
            if (!method.getType().returnType().equals(retType)) return null;
            if (Math.max(method.getType().parameters().size(), 1) != parametersCount) {
                return null;
            }
            if (typeParams.size() != _typeParams.size()) return null;
            if (!typeParams.containsAll(_typeParams)) return null;
        }
        // generate a new method
        var newFn = value.stream()
                .map(TSMethod::getType)
                .reduce(this::combine).orElseThrow();
        var method = value.getFirst();
        method.setType(newFn);
        return method;
    }

    private TSType.TSFunction combine(TSType.TSFunction a, TSType.TSFunction b) {
        var v1 = a.parameters();
        if (v1.isEmpty()) {
            v1.put("p0", TSType.TSPrimitive.VOID);
        }
        var v2 = new ArrayList<>(b.parameters().values());
        if (v2.isEmpty()) {
            v2.add(TSType.TSPrimitive.VOID);
        }
        int i = 0;
        for (Map.Entry<String, TSType> entry : v1.entrySet()) {
            entry.setValue(new TSType.TSUnion(entry.getValue(), v2.get(i++)));
        }
        return a.withParameters(v1);
    }
}
