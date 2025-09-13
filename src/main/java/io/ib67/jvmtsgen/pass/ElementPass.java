package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.TSConstructor;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSMethod;
import io.ib67.jvmtsgen.tsdef.TSModifier;
import io.ib67.kiwi.routine.Uni;

import java.util.List;
import java.util.Objects;

public interface ElementPass {
    TSElement transform(TransformerContext context, TSElement element);

    default void onPassEnd(){}

    default Uni<TSElement> traverse(TSElement root) {
        return c -> {
            c.onValue(root);
            if (Objects.requireNonNull(root) instanceof TSElement.TSCompoundElement ele) {
                Uni.from(List.copyOf(ele.elements())::forEach).flatMap(this::traverse).onItem(c);
            }
        };
    }
}
