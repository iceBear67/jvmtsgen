package io.ib67.jvmtsgen;

import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;
import lombok.Builder;

import static io.ib67.jvmtsgen.TypeUtil.*;

import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
public class ModelBuilder {
    protected final boolean nullableByDefault;
    protected final boolean checkNull;
    private final boolean generateSynthetic;

    public void write(TypeScriptModel tsm, ClassModel model) {
        if (model.flags().has(AccessFlag.SYNTHETIC) && !generateSynthetic) return;
        tsm.newVariable("java", TSType.TSPrimitive.ANY, null);
        tsm.addElement(writeClass(model));
    }

    private TSClassDecl writeClass(ClassModel model) {
        var simpleName = "L" + model.thisClass().asInternalName() + ";";
        var clazz = new TSClassDecl(null, simpleName, new HashMap<>());
        if (model.flags().has(AccessFlag.PUBLIC)) {
            clazz.getModifiers().add(TSModifier.EXPORT);
        }
        clazz.setJavaInternalName(model.thisClass().asInternalName());
        var sign = model.findAttribute(Attributes.signature()).orElse(null);
        if (sign != null) {
            var clazSign = sign.asClassSignature();
            clazz.getType().typeParam().putAll(Uni.from(clazSign.typeParameters()::forEach)
                    .collect(Collectors.toMap(Signature.TypeParam::identifier, this::fromTypeParam)));
        }
        if (model.flags().has(AccessFlag.INTERFACE)) {
            clazz.setKind(TSClassDecl.Kind.INTERFACE);
        } else if (model.flags().has(AccessFlag.ENUM)) {
            clazz.setKind(TSClassDecl.Kind.ENUM);
        }
        var cw = new TypeScriptModel(clazz);
        for (FieldModel field : model.fields()) {
            var flags = field.flags();
            if (hasInvisible(flags)) continue;
            cw.addElement(writeField(field));
        }
        for (MethodModel method : model.methods()) {
            var flags = method.flags();
            var name = method.methodName().stringValue();
            if (hasInvisible(flags)) continue;
            if (name.equals("<clinit>")) continue;
            if (name.equals("<init>")) {
                var tm = writeMethod(method);
                cw.newConstructor(tm.getType().parameters());
            } else {
                cw.addElement(writeMethod(method));
            }
        }
        return clazz;
    }

    public TSFieldDecl writeField(FieldModel field) {
        return new TSFieldDecl(
                new TSVarDecl(
                        field.fieldName().stringValue(),
                        typeFromField(field),
                        null
                ), TSModifier.from(field.flags()));
    }

    public TSMethod writeMethod(MethodModel model) {
        var sign = model.findAttribute(Attributes.signature()).map(SignatureAttribute::asMethodSignature).orElse(null);
        var typeSym = model.methodTypeSymbol();
        TSType returnType = sign == null ? fromClassDesc(typeSym.returnType()) : fromSignature(sign.result());
        var params = sign == null
                ? Uni.from(typeSym.parameterList()::forEach).map(this::fromClassDesc)
                : Uni.from(sign.arguments()::forEach).map(this::fromSignature);
        var typeParams = sign == null
                ? Map.<String, TSType>of()
                : Uni.from(sign.typeParameters()::forEach).collect(Collectors.toMap(Signature.TypeParam::identifier, this::fromTypeParam));
        Map<String, TSType> paramMap;
        var listOfParams = extractAnnotationsParams(model);
        var methodParamNames = findParameterNames(model);
        var parameterCounter = new AtomicInteger(0);
        if (checkNull && !listOfParams.isEmpty()) { // has annotation parameters
            paramMap = params.collect(Collectors.toMap(
                    _ -> methodParamNames.get(parameterCounter.get()),
                    type -> annotateNullable(listOfParams.get(parameterCounter.getAndIncrement()), type)));
        } else {
            paramMap = params.collect(Collectors.toMap(
                    _ -> methodParamNames.get(parameterCounter.getAndIncrement()), Function.identity()));
        }
        var method = new TSMethod(
                null,
                model.methodName().stringValue(),
                new TSType.TSFunction(
                        annotateNullable(extractAnnotationsSingle(model), returnType),
                        paramMap, typeParams));
        method.setModifiers(TSModifier.from(model.flags()));
        return method;
    }
    // VIJCSBFDZ

    protected TSType.TSPrimitive primitiveFromDescriptor(char type) {
        return switch (type) {
            case 'V' -> TSType.TSPrimitive.VOID;
            case 'I', 'B', 'C', 'D', 'F', 'J', 'S' -> TSType.TSPrimitive.NUMBER;
            case 'Z' -> TSType.TSPrimitive.BOOLEAN;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    protected List<String> findParameterNames(MethodModel model) {
        var parameterCounter = new AtomicInteger(0);
        var typeSym = model.methodTypeSymbol();
        // todo javadocannotation
        var _methodParamNames = model.findAttribute(Attributes.methodParameters())
                .stream()
                .flatMap(it->it.parameters().stream())
                .map(it -> it.name().map(Utf8Entry::stringValue).orElse("p" + parameterCounter.getAndIncrement()))
                .toList(); //todo check if this is working on other samples
        if (_methodParamNames.size() != typeSym.parameterCount()) {
            _methodParamNames = Uni.infiniteAscendingNum().limit(typeSym.parameterCount()).map(it -> "p" + it).toList();
        }
        return _methodParamNames;
    }

    protected TSType typeFromField(FieldModel model) {
        var ogType = model.findAttribute(Attributes.signature())
                .map(SignatureAttribute::asTypeSignature)
                .map(this::fromSignature)
                .orElseGet(() -> {
                    var sym = model.fieldTypeSymbol();
                    return new TSType.TSClass(
                            "L" + (sym.packageName() + "." + sym.displayName()).replace(".", "/") + ";"
                            , new HashMap<>());
                });

        return annotateNullable(extractAnnotationsSingle(model), ogType);
    }

    protected <E extends ClassFileElement> List<Annotation> extractAnnotationsSingle(CompoundElement<E> element) {
        return Uni.from(element.elementList()::forEach)
                .<List>multiMap((a, sink) -> {
                    switch (a) {
                        case RuntimeInvisibleAnnotationsAttribute riaa -> sink.onValue(riaa.annotations());
                        case RuntimeVisibleAnnotationsAttribute riav -> sink.onValue(riav.annotations());
                        case RuntimeVisibleTypeAnnotationsAttribute attr -> sink.onValue(attr.annotations());
                        case RuntimeInvisibleTypeAnnotationsAttribute attr -> sink.onValue(attr.annotations());
                        default -> {
                        }
                    }
                }).flatMap(it -> it::forEach)
                .map(it -> (Object) it)
                .map(it -> {
                    if (it instanceof TypeAnnotation ta) {
                        return ta.annotation();
                    }
                    return (Annotation) it;
                }).toList();
    }

    protected List<List<Annotation>> extractAnnotationsParams(MethodModel element) {
        List<List<Annotation>> listOfLists = null;
        for (MethodElement methodElement : element.elementList()) {
            var annotations = switch (methodElement) {
                case RuntimeInvisibleParameterAnnotationsAttribute attr -> attr.parameterAnnotations();
                case RuntimeVisibleParameterAnnotationsAttribute attr -> attr.parameterAnnotations();
                default -> null;
            };
            if (annotations == null) continue;
            if (listOfLists == null) {
                listOfLists = annotations;
                continue;
            }
            for (int i = 0; i < listOfLists.size(); i++) {
                var combined = new ArrayList<>(listOfLists.get(i));
                combined.addAll(annotations.get(i));
                listOfLists.set(i, combined);
            }
        }
        return listOfLists == null ? List.of() : listOfLists;
    }

    protected <E extends ClassFileElement> TSType annotateNullable(List<Annotation> annotations, TSType ogType) {
        if (!checkNull) return ogType;
        for (Annotation annotation : annotations) {
            var desc = annotation.className().stringValue().toLowerCase();
            if (desc.endsWith("nullable;")) {
                return new TSType.TSUnion(ogType, TSType.TSPrimitive.NULL);
            } else if (desc.endsWith("notnull;") || desc.endsWith("nonnull;")) {
                return ogType;
            }
        }
        if (nullableByDefault)
            return new TSType.TSUnion(ogType, TSType.TSPrimitive.NULL);
        return ogType;
    }

    protected TSType fromTypeParam(Signature.TypeParam param) {
        var classBound = param.classBound();
        var typeVar = new TSType.TSTypeVar(param.identifier());
        TSType type;
        if (classBound.isPresent()) {
            type = fromSignature(classBound.get());
            if (type instanceof TSType.TSBounded bounded) {
                type = bounded.withTypeVar(typeVar);
            }
            return type;
        }
        return param.interfaceBounds().stream()
                .map(this::fromSignature)
                .reduce(TSType.TSIntersection::new)
                .orElse(typeVar);
    }

    protected TSType fromSignature(Signature signature) {
        return switch (signature) {
            case Signature.BaseTypeSig baseType -> primitiveFromDescriptor(baseType.baseType());
            case Signature.TypeVarSig tvs -> new TSType.TSTypeVar(tvs.identifier());
            case Signature.ArrayTypeSig ats -> new TSType.TSArray(fromSignature(ats.componentSignature()));
            case Signature.ClassTypeSig cts -> fromClassTypeSig(cts);
        };
    }

    protected TSType fromClassDesc(ClassDesc desc) {
        if (desc.isPrimitive()) {
            return primitiveFromDescriptor(desc.descriptorString().charAt(0));
        }
        if (desc.isArray()) {
            return new TSType.TSArray(fromClassDesc(desc.componentType()));
        }
        return new TSType.TSClass(desc.descriptorString(), new HashMap<>());
    }

    protected TSType fromClassTypeSig(Signature.ClassTypeSig cts) {
//        if (cts.className().equals("java/lang/Object"))
//            return TSType.TSPrimitive.ANY;
        var counter = new AtomicInteger(0);
        return new TSType.TSClass("L" + cts.className() + ";", Uni.from(cts.typeArgs()::forEach)
                .collect(Collectors.toMap(it -> "p" + counter.getAndIncrement(), this::resolveBound)));
    }

    protected TSType resolveBound(Signature.TypeArg typeArg) {
        return switch (typeArg) {
            case Signature.TypeArg.Bounded bounded -> switch (bounded.wildcardIndicator()) {
                case NONE -> fromSignature(bounded.boundType());
                case EXTENDS ->
                        new TSType.TSBounded(null, TSType.TSBounded.Indicator.EXTENDS, fromSignature(bounded.boundType()));
                case SUPER -> new TSType.TSClass("Partial", Map.of("T", fromSignature(bounded.boundType())));
            };
            case Signature.TypeArg.Unbounded _ -> TSType.TSPrimitive.UNKNOWN;
        };
    }
}
