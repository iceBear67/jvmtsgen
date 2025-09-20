package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
public class RenamerPass implements ElementPass {
    protected final boolean asis;
    // internal name -> simple name
    protected final Map<String, String> collectedTypes = new HashMap<>();
    protected final Set<String> sameFileType = new HashSet<>();

    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        if (asis) {
            traverse(element).onItem(this::renameAsis);
        } else throw new UnsupportedOperationException("Renamer does not support other methods yet");
        if (element instanceof TSSourceFile srf) {
            for (var entry : collectedTypes.entrySet()) {
                var descriptor = entry.getKey();
                var simpleName = entry.getValue();
                if (!descriptor.startsWith("L") || !descriptor.endsWith(";")) continue;
                var javaInternalName = descriptor.substring(1, descriptor.length() - 1);
                if (sameFileType.contains(javaInternalName)) continue;
                var parent = srf.getPath().getParent();
                // check common prefix
                if (parent == null) {
                    parent = Path.of("/"); //root
                }
                var _parent = parent.toString();
                var index = _parent.indexOf('/');
                index = index < 0 ? _parent.length() : index;
                if (!javaInternalName.startsWith(_parent.substring(0, index))) continue;
                var relative = parent.toAbsolutePath().relativize(Path.of(javaInternalName).toAbsolutePath());
                srf.getImportTable().put(simpleName, "./" + relative);
            }
        }
        return element;
    }

    private void renameAsis(TSElement element) {
        switch (element) {
            case TSClassDecl cdl -> {
                sameFileType.add(cdl.getJavaInternalName());
                cdl.setType(renameTypeAsis(cdl.getType()));
            }
            case TSConstructor tcon -> tcon.setParameters(mapTypeMap(tcon.getParameters()));
            case TSFieldDecl tf -> tf.getVariableDecl().setType(renameTypeAsis(tf.getVariableDecl().getType()));
            case TSMethod tm -> tm.setType(renameTypeAsis(tm.getType()));
            case TSVarDecl tdcl -> tdcl.setType(renameTypeAsis(tdcl.getType()));
            case TSTypeDecl type -> type.setType(renameTypeAsis(type.getType()));
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends TSType> T renameTypeAsis(T type) {
        return (T) switch (type) {
            case TSType.TSClass claz -> {
                var simple = fromDescriptor(claz.name());
                collectedTypes.put(claz.name(), simple);
                yield claz.withName(simple)
                        .withTypeParam(mapTypeMap(claz.typeParam()));
            }
            case TSType.TSFunction tf ->
                    tf.withReturnType(renameTypeAsis(tf.returnType())).withParameters(mapTypeMap(tf.parameters()))
                            .withTypeParam(mapTypeMap(tf.typeParam()));
            case TSType.TSIntersection intersection -> intersection.withLeft(renameTypeAsis(intersection.left()))
                    .withRight(renameTypeAsis(intersection.right()));
            case TSType.TSUnion union -> union.withLeft(renameTypeAsis(union.left()))
                    .withRight(renameTypeAsis(union.right()));
            case TSType.TSBounded bounded -> bounded.withBound(renameTypeAsis(bounded.bound()));
            case TSType.TSArray tarr -> tarr.withElement(renameTypeAsis(tarr.element()));
            default -> type;
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
