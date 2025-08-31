package io.ib67.jvmtsgen;

import io.ib67.jvmtsgen.pass.TransformerContext;
import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;
import lombok.Builder;

import static io.ib67.jvmtsgen.TypeUtil.*;

import java.lang.classfile.*;
import java.lang.classfile.attribute.*;
import java.lang.classfile.constantpool.ClassEntry;
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
    protected final TypeScriptModel writer;
    protected final TransformerContext context;
    protected final boolean nullableByDefault;
    protected final boolean checkNull;
    private final boolean generateSynthetic;

    private TSClassDecl generateClass(ClassModel model) {
        var simpleName = "L" + model.thisClass().asInternalName() + ";";
        var clazz = writer.newClass(simpleName, model.flags().has(AccessFlag.PUBLIC));
        clazz.setJavaInternalName(model.thisClass().asInternalName());
        var sign = (SignatureAttribute) model.elementStream().filter(it -> it instanceof SignatureAttribute).findFirst().orElse(null);
        if (sign != null) {
            var clazSign = sign.asClassSignature();
            clazz.getType().typeParam().putAll(Uni.from(clazSign.typeParameters()::forEach)
                    .collect(Collectors.toMap(Signature.TypeParam::identifier, this::fromTypeParam)));
        }
        if(model.flags().has(AccessFlag.INTERFACE)){
            clazz.setKind(TSClassDecl.Kind.INTERFACE);
        }else if(model.flags().has(AccessFlag.ENUM)){
            clazz.setKind(TSClassDecl.Kind.ENUM);
        }
        var cw = new TypeScriptModel(clazz);
        for (FieldModel field : model.fields()) {
            var flags = field.flags();
            if (hasInvisible(flags)) continue;
            var f = cw.newField(field.fieldName().stringValue(), typeFromField(field), null);
            f.setModifiers(TSModifier.from(flags));
        }
        for (MethodModel method : model.methods()) {
            var flags = method.flags();
            if (hasInvisible(flags)) continue;
            var name = method.methodName().stringValue();
            if (name.equals("<clinit>")) continue;
            if (name.equals("<init>")) {
                var tm = fromMethodModel(method);
                cw.newConstructor(tm.getType().parameters());
            } else {
                cw.addElement(fromMethodModel(method));
            }
        }
        for (ClassElement classElement : model.elementList()) {
            if(classElement instanceof NestMembersAttribute attribute){
                for (ClassEntry nestMember : attribute.nestMembers()) { //todo nest members
                }
            }
        }
        return clazz;
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

    protected TSMethod fromMethodModel(MethodModel model) {
        var sign = model.elementStream()
                .filter(it -> it instanceof SignatureAttribute)
                .findFirst()
                .map(it -> ((SignatureAttribute) it).asMethodSignature()).orElse(null);
        var typeSym = model.methodTypeSymbol();
        TSType returnType = sign == null ? fromClassDesc(typeSym.returnType()) : fromSignature(sign.result());
        // todo name mapping
        var counter = new AtomicInteger(0);
        var params = sign == null
                ? Uni.from(typeSym.parameterList()::forEach).map(this::fromClassDesc)
                : Uni.from(sign.arguments()::forEach).map(this::fromSignature);
        var typeParams = sign == null
                ? Map.<String, TSType>of()
                : Uni.from(sign.typeParameters()::forEach).collect(Collectors.toMap(Signature.TypeParam::identifier, this::fromTypeParam));
        Map<String, TSType> paramMap;
        var listOfParams = extractAnnotationsParams(model);
        if (checkNull && !listOfParams.isEmpty()) {
            paramMap = params.collect(Collectors.toMap(
                    _ -> "p" + counter.get(),
                    type -> annotateNullable(listOfParams.get(counter.getAndIncrement()), type)));
        } else {
            paramMap = params.collect(Collectors.toMap(i -> "p" + counter.getAndIncrement(), Function.identity()));
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

    protected TSType typeFromField(FieldModel model) {
        var ogType = model.elementStream()
                .filter(it -> it instanceof SignatureAttribute)
                .findFirst()
                .map(it -> ((SignatureAttribute) it).asTypeSignature())
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
        return element.elementStream().mapMulti((a, sink) -> {
            switch (a) {
                case RuntimeInvisibleAnnotationsAttribute riaa -> sink.accept(riaa.annotations());
                case RuntimeVisibleAnnotationsAttribute riav -> sink.accept(riav.annotations());
                case RuntimeVisibleTypeAnnotationsAttribute attr -> sink.accept(attr.annotations());
                case RuntimeInvisibleTypeAnnotationsAttribute attr -> sink.accept(attr.annotations());
                default -> {
                }
            }
        }).flatMap(it -> ((List<Annotation>) it).stream()).toList();
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
                System.out.println(bounded.bound());
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
        return new TSType.TSClass(desc.displayName(), new HashMap<>());
    }

    protected TSType fromClassTypeSig(Signature.ClassTypeSig cts) {
        if (cts.className().equals("java/lang/Object"))
            return TSType.TSPrimitive.ANY;
        var counter = new AtomicInteger(0);
        return new TSType.TSClass("L" + cts.className() + ";", Uni.from(cts.typeArgs()::forEach)
                .collect(Collectors.toMap(it -> "p" + counter.getAndIncrement(), this::resolveBound)));
    }

    protected TSType resolveBound(Signature.TypeArg typeArg) {
        return switch (typeArg) {
            case Signature.TypeArg.Bounded bounded -> switch (bounded.wildcardIndicator()) {
                case NONE -> {
                    if (bounded.boundType().signatureString().contains("java/lang/Object")) {
                        yield TSType.TSPrimitive.ANY;
                    }
                    yield fromSignature(bounded.boundType());
                }
                case EXTENDS ->
                        new TSType.TSBounded(null, TSType.TSBounded.Indicator.EXTENDS, fromSignature(bounded.boundType()));
                case SUPER -> new TSType.TSClass("Partial", Map.of("T", fromSignature(bounded.boundType())));
            };
            case Signature.TypeArg.Unbounded _ -> TSType.TSPrimitive.UNKNOWN;
        };
    }

    public void write(ClassModel model) {
        if(model.flags().has(AccessFlag.SYNTHETIC) && !generateSynthetic) return;
        writeStub();
        generateClass(model);
    }

    private void writeStub() {
        writer.newVariable("java", TSType.TSPrimitive.ANY, null);
    }
}
