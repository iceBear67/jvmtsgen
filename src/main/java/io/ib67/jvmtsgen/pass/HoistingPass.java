package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.jvmtsgen.tsdef.special.TSCompound;
import io.ib67.kiwi.routine.Uni;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Builder
public class HoistingPass implements ElementPass {
    protected final boolean hoistStaticMethod;
    protected final boolean hoistConstructor;

    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        TSElement.TSCompoundElement top = switch (element) {
            case TSSourceFile tsf -> tsf;
            case TSElement.TSCompoundElement ele when !(ele instanceof TSClassDecl) -> ele;
            default -> new TSCompound(null, new ArrayList<>());
        };
        select(element)
                .map(it -> createStaticFactory(context, it))
                .onItem(t -> {
                    t.setParent(top);
                    top.elements().add(t);
                });
        return top;
    }

    private TSElement createStaticFactory(TransformerContext context, TSElement element) {
        return switch (element) {
            case TSConstructor constructor when constructor.getParent() instanceof TSClassDecl decl -> {
                decl.elements().remove(constructor);
                yield createStaticFactory(context, constructor, decl);
            }
            case TSMethod method when method.getParent() instanceof TSClassDecl decl -> {
                decl.elements().remove(method);
                yield createStaticFactory(context, method, decl);
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + element);
        };
    }

    private TSElement createStaticFactory(TransformerContext context, TSMethod method, TSClassDecl decl) {
        var type = method.getType();
        Objects.requireNonNull(decl.getJavaInternalName());
        var code = context.stubNameOf(decl.getJavaInternalName()) + "." + method.getName() + "(" + String.join(",", type.parameters().keySet()) + ");";
        if (method.isAsync() || type.returnType() != TSType.TSPrimitive.VOID) {
            code = "return " + code;
        }
        method.setCode(code);
        method.getModifiers().add(TSModifier.EXPORT);
        method.getModifiers().remove(TSModifier.PUBLIC);
        method.getModifiers().remove(TSModifier.STATIC);
        method.getModifiers().remove(TSModifier.PRIVATE);
        return method;
    }

    private TSElement createStaticFactory(TransformerContext context, TSConstructor constructor, TSClassDecl decl) {
        var typeParams = decl.getType().typeParam();
        return new TSMethod(
                "new" + decl.getType().name(),
                EnumSet.of(TSModifier.EXPORT),
                new TSType.TSFunction(decl.getType().withTypeParam(typeParams), // todo transform typeParams, <K,V> -> <V, TypeVar of V>
                        constructor.getParameters(), typeParams),
                "return new " + context.stubNameOf(decl.getJavaInternalName())
                        + "(" + String.join(",", constructor.getParameters().keySet()) + ");",
                false
        );
    }

    public Uni<TSElement> select(TSElement root) {
        return c -> traverse(root).onItem(it -> {
            switch (it){
                case TSConstructor con
                        when hoistConstructor -> c.onValue(con);
                case TSMethod meth
                        when hoistStaticMethod && meth.getModifiers().contains(TSModifier.STATIC) -> c.onValue(meth);
                default -> {
                }
            }
        });
    }
}
