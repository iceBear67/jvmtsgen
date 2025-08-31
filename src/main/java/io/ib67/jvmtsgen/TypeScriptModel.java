package io.ib67.jvmtsgen;

import io.ib67.jvmtsgen.tsdef.*;

import java.util.Map;

public class TypeScriptModel {
    protected final TSElement.TSCompoundElement element;

    public TypeScriptModel(TSElement.TSCompoundElement element) {
        this.element = element;
    }

    public TSMethod newMethod(String name, TSType returnType, Map<String, TSType> param) {
        var method = new TSMethod(element, name, new TSType.TSFunction(returnType, param, null));
        element.elements().add(method);
        return method;
    }

    public TSFieldDecl newField(String name, TSType type, String initializer) {
        if(!(element instanceof TSClassDecl))
            throw new IllegalStateException("Field declarations can only be in class.");
        var field = new TSFieldDecl(element, new TSVarDecl(name, type, initializer));
        element.elements().add(field);
        return field;
    }

    public TSVarDecl newVariable(String name, TSType type, String initializer) {
        if (element instanceof TSClassDecl)
            throw new IllegalStateException("Top level variable declarations cannot be in class");
        var decl = new TSVarDecl(name, type, initializer);
        element.elements().add(decl);
        return decl;
    }

    public TSConstructor newConstructor(Map<String, TSType> param) {
        if (!(element instanceof TSClassDecl)) {
            throw new IllegalStateException("Constructor cannot exist without class.");
        }
        var constructor = new TSConstructor(element);
        constructor.setParameters(param);
        element.elements().add(constructor);
        return constructor;
    }

    public TSClassDecl newClass(String name, boolean export) {
        var clazz = new TSClassDecl(element, name, null);
        if(export) clazz.getModifiers().add(TSModifier.EXPORT);
        element.elements().add(clazz);
        return clazz;
    }

    public void addElement(TSElement element) {
        element.setParent(this.element);
        this.element.elements().add(element);
    }
}
