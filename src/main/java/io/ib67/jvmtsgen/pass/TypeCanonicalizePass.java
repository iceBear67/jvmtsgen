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
        traverse(element).onItem(it -> dispatch(context, it));
        return element;
    }

    private void dispatch(TransformerContext context, TSElement element) {
        switch (element) {
            case TSMethod method -> transformMethod(context, method);
            case TSClassDecl claz -> transformClass(context,claz);
            case TSFieldDecl field -> transformField(context,field);
            case TSVarDecl varDecl -> transformVarDecl(context,varDecl);
            case TSConstructor tcon -> transformContructor(context,tcon);
            case TSTypeDecl typeDecl -> transformTypeDecl(context, typeDecl);
            default -> {
            } // omit
        }
    }

    protected void transformTypeDecl(TransformerContext context, TSTypeDecl typeDecl) {
        typeDecl.setTypeParam(transformTypeMap(context, typeDecl, typeDecl.getTypeParam()));
        typeDecl.setType(transformType(context, typeDecl, typeDecl.getType()));
    }

    protected void transformContructor(TransformerContext context, TSConstructor tcon) {
        tcon.setParameters(transformTypeMap(context, tcon, tcon.getParameters()));
    }

    protected void transformVarDecl(TransformerContext context, TSVarDecl varDecl) {
        varDecl.setType(transformType(context, varDecl, varDecl.getType()));
    }

    protected void transformField(TransformerContext context, TSFieldDecl field) {
        transformVarDecl(context, field.getVariableDecl());
    }

    protected void transformClass(TransformerContext context, TSClassDecl claz) {
        var type = claz.getType();
        claz.setType(type.withTypeParam(transformTypeMap(context, claz, type.typeParam())));
    }

    protected void transformMethod(TransformerContext context, TSMethod method) {
        var type = method.getType();
        var newType = new TSType.TSFunction(
                transformType(context, method, type.returnType()),
                transformTypeMap(context, method, type.parameters()),
                transformTypeMap(context, method, type.typeParam())
        );
        method.setType(newType);
    }

    private TSType transformType(TransformerContext context, TSElement element, TSType type) {
        for (TypeCanonicalizer canonicalizer : canonicalizers) {
            type = canonicalizer.transform(context, element, type);
        }
        return type;
    }

    protected Map<String, TSType> transformTypeMap(TransformerContext context, TSElement element, Map<String, TSType> typeMap) {
        var newMap = new HashMap<String, TSType>(typeMap.size());
        for (Map.Entry<String, TSType> entry : typeMap.entrySet()) {
            var type = entry.getValue();
            for (TypeCanonicalizer canonicalizer : canonicalizers) {
                type = canonicalizer.transform(context, element, type);
            }
            newMap.put(entry.getKey(), type);
        }
        return newMap;
    }

    public interface TypeCanonicalizer {

        default Map<String, TSType> transformTypeMap(
                TransformerContext context,
                TSElement element, Map<String, TSType> typeMap) {
            var newMap = new HashMap<String, TSType>();
            for (Map.Entry<String, TSType> entry : typeMap.entrySet()) {
                newMap.put(entry.getKey(), transform(context, element, entry.getValue()));
            }
            return newMap;
        }

        default Map<String, TSType.TSObject.ObjectProperty> transformProperties(
                TransformerContext context,
                TSElement element,
                Map<String, TSType.TSObject.ObjectProperty> properties) {
            var newMap = new HashMap<String, TSType.TSObject.ObjectProperty>();
            for (var entry : properties.entrySet()) {
                var v = entry.getValue();
                newMap.put(entry.getKey(), v.withType(transform(context, element, v.type())));
            }
            return newMap;
        }

        @SuppressWarnings("unchecked")
        default <T extends TSType> T transform(TransformerContext context, TSElement element, T type) {
            return (T) transformFromBottom(context, element, switch (type) {
                case TSType.TSArray tarr -> tarr.withElement(transform(context, element, tarr.element()));
                case TSType.TSBounded bounded -> new TSType.TSBounded(
                        transform(context, element, bounded.typeVar()),
                        bounded.indicator(), transform(context, element, bounded.bound()));
                case TSType.TSClass claz -> claz.withTypeParam(transformTypeMap(context, element, claz.typeParam()));
                case TSType.TSObject to -> to.withProperties(transformProperties(context, element, to.properties()));
                case TSType.TSUnion tu ->
                        new TSType.TSUnion(transform(context, element, tu.left()), transform(context, element, tu.right()));
                case TSType.TSFunction tfn -> new TSType.TSFunction(transform(context, element, tfn.returnType()),
                        transformTypeMap(context, element, tfn.parameters()), transformTypeMap(context, element, tfn.typeParam()));
                case TSType.TSIntersection in ->
                        new TSType.TSIntersection(transform(context, element, in.left()), transform(context, element, in.right()));
                default -> type;
            });
        }

        TSType transformFromBottom(TransformerContext context, TSElement element, TSType type);
    }
}
