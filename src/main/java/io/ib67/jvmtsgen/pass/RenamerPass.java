package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
public class RenamerPass implements ElementPass {
    protected final boolean asis;

    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        if (asis) {
            traverse(element)
                    .onItem(this::renameAsis);
        } else throw new UnsupportedOperationException("Renamer does not support other methods yet");
        return element;
    }

    private void renameAsis(TSElement element) {
        switch (element) {
            case TSClassDecl cdl -> cdl.setType(renameTypeAsis(cdl.getType()));
            case TSConstructor tcon -> tcon.setParameters(mapTypeMap(tcon.getParameters()));
            case TSFieldDecl tf -> tf.getVariableDecl().setType(renameTypeAsis(tf.getVariableDecl().getType()));
            case TSMethod tm -> tm.setType(renameTypeAsis(tm.getType()));
            case TSVarDecl tdcl -> tdcl.setType(renameTypeAsis(tdcl.getType()));
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends TSType> T renameTypeAsis(T type) {
        return (T) switch (type) {
            case TSType.TSClass claz -> claz.withName(fromDescriptor(claz.name()))
                    .withTypeParam(mapTypeMap(claz.typeParam()));
            case TSType.TSFunction tf ->
                    tf.withReturnType(renameTypeAsis(tf.returnType())).withParameters(mapTypeMap(tf.parameters()))
                            .withTypeParam(mapTypeMap(tf.typeParam()));
            case TSType.TSIntersection intersection -> intersection.withLeft(renameTypeAsis(intersection.left()))
                    .withRight(renameTypeAsis(intersection.right()));
            case TSType.TSBounded bounded -> bounded.withBound(renameTypeAsis(bounded.bound()));
            default -> type;
        };
    }

    protected Uni<TSElement> traverse(TSElement element) {
        return c -> {
            if (Objects.requireNonNull(element) instanceof TSElement.TSCompoundElement compoundElement) {
                c.onValue(compoundElement);
                for (TSElement tsElement : compoundElement.elements()) {
                    traverse(tsElement).onItem(c);
                }
            } else {
                c.onValue(element);
            }
        };
    }

    protected String fromDescriptor(String descriptor) {
        if (!descriptor.startsWith("L") || !descriptor.endsWith(";")) {
            return descriptor;
        }
        return descriptor.substring(Math.max(descriptor.lastIndexOf('/') + 1, 1), descriptor.length() - 1);
    }

    private Map<String, TSType> mapTypeMap(Map<String, TSType> map) {
        return map.entrySet()
                .stream().map(it -> Map.entry(it.getKey(), renameTypeAsis(it.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
