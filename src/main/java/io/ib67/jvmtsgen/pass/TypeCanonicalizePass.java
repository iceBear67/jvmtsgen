package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;
import io.ib67.kiwi.routine.op.UniOp;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class TypeCanonicalizePass implements ElementPass {
    protected final List<TypeCanonicalizer> canonicalizers;

    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        traverse(element).onItem(this::dispatch);
        return element;
    }

    private void dispatch(TSElement element) {
        switch (element) {
            case TSMethod method -> transformMethod(method);
            case TSClassDecl claz -> transformClass(claz);
            case TSFieldDecl field -> transformField(field);
            case TSVarDecl varDecl -> transformVarDecl(varDecl);
            case TSConstructor tcon -> transformContructor(tcon);
            case TSTypeDecl typeDecl -> transformTypeDecl(typeDecl);
            default -> {
            } // omit
        }
    }

    protected void transformTypeDecl(TSTypeDecl typeDecl) {
        transformTypeMap(typeDecl, typeDecl.getTypeParam());
        typeDecl.setType(transformType(typeDecl, typeDecl.getType()));
    }

    protected void transformContructor(TSConstructor tcon) {
        transformTypeMap(tcon, tcon.getParameters());
    }

    protected void transformVarDecl(TSVarDecl varDecl) {
        varDecl.setType(transformType(varDecl, varDecl.getType()));
    }

    protected void transformField(TSFieldDecl field) {
        transformVarDecl(field.getVariableDecl());
    }

    protected void transformClass(TSClassDecl claz) {
        var type = claz.getType();
        transformTypeMap(claz, type.typeParam());
    }

    protected void transformMethod(TSMethod method) {
        var type = method.getType();
        transformTypeMap(method, type.typeParam());
        transformTypeMap(method, type.parameters());
    }

    private TSType transformType(TSElement element, TSType type) {
        for (TypeCanonicalizer canonicalizer : canonicalizers) {
            type = canonicalizer.transform(element, type);
        }
        return type;
    }

    protected void transformTypeMap(TSElement element, Map<String, TSType> typeMap) {
        for (Map.Entry<String, TSType> entry : typeMap.entrySet()) {
            var type = entry.getValue();
            for (TypeCanonicalizer canonicalizer : canonicalizers) {
                type = canonicalizer.transform(element, type);
            }
            entry.setValue(type);
        }
    }

    public interface TypeCanonicalizer {

        default Map<String, TSType> transformTypeMap(TSElement element, Map<String, TSType> typeMap) {
            var newMap = new HashMap<String, TSType>();
            for (Map.Entry<String, TSType> entry : typeMap.entrySet()) {
                newMap.put(entry.getKey(), transform(element, entry.getValue()));
            }
            return newMap;
        }

        default Map<String, TSType.TSObject.ObjectProperty> transformProperties(TSElement element, Map<String, TSType.TSObject.ObjectProperty> properties) {
            var newMap = new HashMap<String, TSType.TSObject.ObjectProperty>();
            for (var entry : properties.entrySet()) {
                var v = entry.getValue();
                newMap.put(entry.getKey(), v.withType(transform(element, v.type())));
            }
            return newMap;
        }

        @SuppressWarnings("unchecked")
        default <T extends TSType> T transform(TSElement element, T type) {
            return (T) transformFromBottom(element, switch (type) {
                case TSType.TSArray tarr -> tarr.withElement(transform(element, tarr.element()));
                case TSType.TSBounded bounded -> bounded.withTypeVar(transform(element, bounded.typeVar()))
                        .withBound(transform(element, bounded.bound()));
                case TSType.TSClass claz -> claz.withTypeParam(transformTypeMap(element, claz.typeParam()));
                case TSType.TSObject to -> to.withProperties(transformProperties(element, to.properties()));
                case TSType.TSUnion tu ->
                        tu.withLeft(transform(element, tu.left())).withRight(transform(element, tu.right()));
                case TSType.TSFunction tfn -> tfn.withTypeParam(transformTypeMap(element, tfn.typeParam()))
                        .withReturnType(transform(element, tfn.returnType()))
                        .withParameters(transformTypeMap(element, tfn.parameters()));
                case TSType.TSIntersection in -> in.withLeft(transform(element, in.left()))
                        .withRight(transform(element, in.right()));
                default -> type;
            });
        }

        TSType transformFromBottom(TSElement element, TSType type);
    }
}
