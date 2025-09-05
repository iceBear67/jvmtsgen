package io.ib67.jvmtsgen.writer;

import io.ib67.jvmtsgen.TypeUtil;
import io.ib67.jvmtsgen.tsdef.*;
import io.ib67.kiwi.routine.Uni;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeScriptWriter {
    protected final Set<WriterFeature> features;

    public TypeScriptWriter(Set<WriterFeature> features) {
        this.features = features;
    }

    public TypeScriptWriter(WriterFeature... features) {
        this();
        Collections.addAll(this.features, Objects.requireNonNull(features));
    }

    public TypeScriptWriter() {
        this(EnumSet.noneOf(WriterFeature.class));
    }

    public String generate(TSElement element) {
        return switch (element) {
            case TSClassDecl classDecl -> writeClassDecl(classDecl);
            case TSConstructor constructor -> writeConstructor(constructor);
            case TSFieldDecl fieldDecl -> writeField(fieldDecl);
            case TSMethod methodDecl -> writeMethod(methodDecl);
            case TSVarDecl varDecl -> writeVarDecl(varDecl);
            case TSSourceFile tsf -> writeSourceFile(tsf);
            case TSTypeDecl typeDecl -> writeTypeDecl(typeDecl);
            case TSElement.TSCompoundElement otherCompound -> otherCompound.elements().stream()
                    .map(this::generate)
                    .collect(Collectors.joining("\n"));
            default -> throw new IllegalArgumentException("Unexpected value: " + element);
        };
    }

    private String writeTypeDecl(TSTypeDecl typeDecl) {
        return TypeUtil.getModifiers(typeDecl, typeDecl.getModifiers())
                + (feature(WriterFeature.EMIT_DECLARATION_ONLY) ? "declare " : "")
                + "type " + typeDecl.getName()
                + (typeDecl.getTypeParam().isEmpty() ? "" : TypeUtil.typeParamString(typeDecl.getTypeParam()))
                + " = " + typeDecl.getType();
    }

    protected String writeSourceFile(TSSourceFile tsf) {
        return tsf.elements().stream().map(this::generate).collect(Collectors.joining("\n"));
    }

    protected String writeVarDecl(TSVarDecl varDecl) {
        if (!fromInterface(varDecl) && feature(WriterFeature.EMIT_DECLARATION_ONLY)) {
            return TypeUtil.getModifiers(varDecl, varDecl.getModifiers()) + "declare let " + varDecl.getName() + ": " + varDecl.getType();
        }
        var r = TypeUtil.getModifiers(varDecl, varDecl.getModifiers()) + "let " + varDecl.getName() + ": " + varDecl.getType();
        var defaultInitializer = varDecl.getDefaultInitializer();
        if (!fromInterface(varDecl) && defaultInitializer != null && !defaultInitializer.isEmpty()) {
            return r + " = " + defaultInitializer;
        }
        return r;
    }

    protected String writeMethod(TSMethod methodDecl) {
        var sb = new StringBuilder();
        if (!fromInterface(methodDecl) && !feature(WriterFeature.ALWAYS_INTERFACE)) {
            sb.append(TypeUtil.getModifiers(methodDecl, methodDecl.getModifiers()));
        }
        if (!(methodDecl.getParent() instanceof TSClassDecl)) { // top level function
            sb.append("function ");
        }
        sb.append(methodDecl.getName());
        var fn = methodDecl.getType();
        if (!fn.typeParam().isEmpty()) {
            sb.append(TypeUtil.typeParamString(fn.typeParam()));
        }
        sb.append('(').append(TSType.paramSet(fn.parameters())).append(')');
        if (fn.returnType() != TSType.TSPrimitive.VOID) {
            sb.append(": ").append(fn.returnType());
        }

        if (fromInterface(methodDecl)
                || methodDecl.getCode() == null
                || feature(WriterFeature.ALWAYS_INTERFACE)
                || feature(WriterFeature.EMIT_DECLARATION_ONLY)) {
            return sb.toString();
        }
        sb.append(" {").append(methodDecl.getCode()).append("}");
        return sb.toString();
    }

    protected String writeField(TSFieldDecl fieldDecl) {
        var sb = new StringBuilder();
        var varDecl = fieldDecl.getVariableDecl();
        sb.append(TypeUtil.getModifiers(fieldDecl, fieldDecl.getModifiers()))
                .append(varDecl.getName()).append(": ").append(varDecl.getType());
        return sb.toString();
    }

    protected String writeConstructor(TSConstructor constructor) {
        if (fromInterface(constructor) || feature(WriterFeature.ALWAYS_INTERFACE)) {
            return ""; // constructors are omitted
        }
        return TypeUtil.getModifiers(constructor, constructor.getModifiers()) +
                " constructor(" + TSType.paramSet(constructor.getParameters()) + "){" + constructor.getBody() + "}";
    }

    protected String writeClassDecl(TSClassDecl classDecl) {
        var sb = new StringBuilder();
        var clazType = classDecl.getType();
        sb.append(TypeUtil.getModifiers(classDecl, classDecl.getModifiers()));
        if (feature(WriterFeature.EMIT_DECLARATION_ONLY)) {
            sb.append("declare ");
        }
        var typePrefix = switch (classDecl.getKind()) {
            case INTERFACE -> "interface ";
            case ENUM -> "class ";
            case CLASS -> feature(WriterFeature.ALWAYS_INTERFACE) ? "interface " : "class ";
        };
        sb.append(typePrefix);
        sb.append(clazType.name()).append(TypeUtil.typeParamString(clazType.typeParam())).append(" {\n");
        for (TSElement element : classDecl.elements()) {
            sb.append(generate(element)).append('\n');
        }
        sb.append("}");
        return sb.toString();
    }

    protected boolean fromInterface(TSElement element) {
        return element.getParent() instanceof TSClassDecl decl && decl.getKind() == TSClassDecl.Kind.INTERFACE;
    }

    protected boolean feature(WriterFeature f) {
        return features.contains(f);
    }
}
