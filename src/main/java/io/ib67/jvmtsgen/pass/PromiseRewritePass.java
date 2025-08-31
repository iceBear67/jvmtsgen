package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSMethod;
import io.ib67.jvmtsgen.tsdef.TSType;
import io.ib67.kiwi.routine.Uni;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

@Builder
public class PromiseRewritePass implements ElementPass {
    protected final Set<String> thenables;

    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        traverse(element)
                .filter(it -> it instanceof TSMethod)
                .map(it -> (TSMethod) it)
                .onItem(this::processMethod);
        return element;
    }

    private void processMethod(TSMethod method) {
        if (method.isAsync()) return;
        var type = method.getType();
        if (!(type.returnType() instanceof TSType.TSClass claz)) return;
        if (thenables.contains(claz.name())) {
            var param = claz.typeParam();
            if (param.size() > 1) {
                throw new IllegalStateException("PromiseLike can only have one parameter at most");
            }
            method.setAsync(true);
            method.setType(type.withReturnType(
                    new TSType.TSClass("Promise", Map.of("T", Uni.from(claz.typeParam().values()::forEach)
                            .first().orElse(TSType.TSPrimitive.UNKNOWN)))
            ));
        }
    }
}
