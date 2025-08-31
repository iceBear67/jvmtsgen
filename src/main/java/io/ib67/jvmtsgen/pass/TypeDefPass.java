package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.jvmtsgen.tsdef.special.TSCompound;
import io.ib67.kiwi.routine.Uni;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.*;

@Builder
@AllArgsConstructor
public class TypeDefPass implements ElementPass {
    @Builder.Default
    protected final boolean transformInterface = false;
    @Builder.Default
    protected final Set<TSModifier> allowAccess = EnumSet.of(TSModifier.PUBLIC);

    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        TSElement.TSCompoundElement top = switch (element) {
            case TSSourceFile tsf -> tsf;
            case TSElement.TSCompoundElement ele when !(ele instanceof TSClassDecl) -> ele;
            default -> new TSCompound(null, new ArrayList<>());
        };
        var uni = select(element);
        var newElements = new ArrayList<TSElement>();
        uni.filter(c -> transformInterface == (c.getKind() == TSClassDecl.Kind.INTERFACE))
                .map(c -> new TSTypeDecl(
                        c.getType().name(),
                        classToObjectLiteral(c, false),
                        EnumSet.of(TSModifier.EXPORT),
                        c.getType().typeParam()
                )).peek(it -> it.setParent(top))
                .onItem(newElements::add); // type Type = { instance members }
        uni.map(c -> new TSVarDecl( // const Type: { static members } = stub
                c.getType().name(),
                classToObjectLiteral(c, true),
                EnumSet.of(TSModifier.EXPORT),
                context.stubNameOf(c.getJavaInternalName())
        )).peek(it -> it.setParent(top)).onItem(newElements::add);
        uni.onItem(this::removeFromParent);
        newElements.forEach(top.elements()::add);
        return top;
    }

    private void removeFromParent(TSClassDecl classDecl) {
        if (classDecl.getParent() instanceof TSElement.TSCompoundElement element) {
            element.elements().remove(classDecl);
        }
    }

    private TSType.TSObject classToObjectLiteral(TSClassDecl classDecl, boolean isStatic) {
        var obj = new TSType.TSObject();
        for (TSElement element : classDecl.elements()) {
            switch (element) {
                case TSMethod method
                        when (isStatic == method.getModifiers().contains(TSModifier.STATIC))
                        && intersectionAny(method.getModifiers()) ->
                        obj.addProperty(
                                method.getName(),
                                method.getType(),
                                true
                        );
                case TSFieldDecl field when (isStatic == field.getModifiers().contains(TSModifier.STATIC))
                        && intersectionAny(field.getModifiers())
                        && field.getVariableDecl() instanceof TSVarDecl varDecl ->
                        obj.addProperty(
                                varDecl.getName(),
                                varDecl.getType(),
                                field.getModifiers().contains(TSModifier.READ_ONLY)
                        );
                default -> {
                }
            }
        }
        return obj;
    }

    protected boolean intersectionAny(Set<TSModifier> modifiers) {
        for (TSModifier modifier : modifiers) {
            if (allowAccess.contains(modifier)) {
                return true;
            }
        }
        return false;
    }

    protected Uni<TSClassDecl> select(TSElement element) {
        return c -> traverse(element)
                .filter(it -> it instanceof TSClassDecl)
                .onItem(it -> c.onValue((TSClassDecl) it));
    }
}
