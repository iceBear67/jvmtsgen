package io.ib67.jvmtsgen.pass.nameresolver;

import io.ib67.jvmtsgen.pass.ElementPass;
import io.ib67.jvmtsgen.pass.TransformerContext;
import io.ib67.jvmtsgen.tsdef.TSClassDecl;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSFieldDecl;
import io.ib67.jvmtsgen.tsdef.TSMethod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface OverloadPass extends ElementPass {
    @Override
    default TSElement transform(TransformerContext context, TSElement element) {
        traverse(element).filter(it -> it instanceof TSElement.TSCompoundElement).onItem(this::tryMergeOverload);
        return element;
    }

    private void tryMergeOverload(TSElement _element) {
        var element = (TSElement.TSCompoundElement) _element;
        var methodsByName = element.elements().stream()
                .filter(it -> it instanceof TSMethod)
                .map(it -> (TSMethod) it)
                .collect(Collectors.groupingBy(TSMethod::getName,
                        Collectors.mapping(Function.identity(), Collectors.<TSElement>toList())));
        element.elements().stream()
                .filter(it -> it instanceof TSFieldDecl)
                .map(it -> (TSFieldDecl) it)
                .forEach(it -> {
                    var l = methodsByName.get(it.getVariableDecl().getName());
                    if(l == null) return;
                    l.add(it);
                });
        for (List<TSElement> value : methodsByName.values()) {
            if (value.size() == 1) continue;
            var newMethod = tryMergeOverload(element, value);
            if(newMethod != null){
                element.elements().removeAll(value);
                element.elements().add(newMethod);
            }
        }
    }

    /**
     * @param clazz the parent of methods
     * @param elements elements with same name, to merge.
     * @return a reduction result, which is to replace the original methods, otherwise do nothing.
     */
    @Nullable TSMethod tryMergeOverload(TSElement.TSCompoundElement clazz, List<TSElement> elements);
}
