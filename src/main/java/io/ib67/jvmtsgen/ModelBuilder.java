package io.ib67.jvmtsgen;

import io.ib67.jvmtsgen.pass.TransformerContext;
import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;

import static io.ib67.jvmtsgen.TypeUtil.*;
import static java.lang.Math.max;

import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.Signature;
import java.lang.classfile.attribute.SignatureAttribute;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModelBuilder {
    protected final TypeScriptModel writer;
    protected final TransformerContext context;

    public ModelBuilder(TypeScriptModel writer, TransformerContext context) {
        this.writer = writer;
        this.context = context;
    }

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
                tm.setParent(cw.element);
                cw.newConstructor(tm.getType().parameters());
            } else {
                cw.addElement(fromMethodModel(method));
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
        var paramMap = params.collect(Collectors.toMap(i -> "p" + counter.getAndIncrement(), Function.identity()));
        var typeParams = sign == null
                ? Map.<String, TSType>of()
                : Uni.from(sign.typeParameters()::forEach).collect(Collectors.toMap(Signature.TypeParam::identifier, this::fromTypeParam));
        var method = new TSMethod(
                null,
                model.methodName().stringValue(),
                new TSType.TSFunction(false, returnType, paramMap, typeParams)); //todo async
        method.setModifiers(TSModifier.from(model.flags()));
        return method;
    }

    protected TSType typeFromField(FieldModel model) {
        var sign = model.elementStream().filter(it -> it instanceof SignatureAttribute).findFirst().map(it -> ((SignatureAttribute) it).asTypeSignature()).orElse(null);
        if (sign != null) {
            return fromSignature(sign);
        }
        var sym = model.fieldTypeSymbol();
        return new TSType.TSClass(
                "L" + (sym.packageName() + "." + sym.displayName()).replace(".", "/") + ";"
                , new HashMap<>());
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
                case SUPER -> new TSType.TSClass("Partial", Map.of(null, fromSignature(bounded.boundType())));
            };
            case Signature.TypeArg.Unbounded _ -> TSType.TSPrimitive.UNKNOWN;
        };
    }
    
    protected String stubName(String internal){
        return "java."+internal.replace("/", ".");
    }

    public void write(ClassModel model) {
        writeStub(model);
    }

    private void writeStub(ClassModel model) {
        writer.newVariable("java", TSType.TSPrimitive.ANY, null);
        
        var classDecl = generateClass(model);
        for (TSElement element : classDecl.elements()) {
            switch (element) {
                case TSMethod method
                        when method.getCode() == null && method.getModifiers().contains(TSModifier.STATIC) -> {
                    var code = stubName(model.thisClass().asInternalName()) + "." + method.getName() +
                            "(" + String.join(", ", method.getType().parameters().keySet()) + ")";
                    if (method.getModifiers().contains(TSModifier.ASYNC)
                            || method.getType().returnType() != TSType.TSPrimitive.VOID) {
                        code = "return " + code;
                    }
                    method.setCode(code);
                }
                case TSMethod method
                        when method.getCode() == null -> {
                    if (method.getModifiers().contains(TSModifier.ASYNC)
                            || method.getType().returnType() != TSType.TSPrimitive.VOID) {
                        method.setCode("return {} as " + method.getType().returnType());
                    } else {
                        method.setCode("");
                    }
                }
                default -> {
                }
            }
        }
    }
}
