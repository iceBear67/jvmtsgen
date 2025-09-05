package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.*;

public enum PlaceholderCodePass implements ElementPass {
    INSTANCE;
    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        traverse(element).onItem(e -> {
            switch (e) {
                case TSMethod method
                        when method.getCode() == null
                        && method.getModifiers().contains(TSModifier.STATIC)
                        && method.getParent() instanceof TSClassDecl classDecl -> {
                    var code = context.stubNameOf(classDecl.getJavaInternalName()) + "." + method.getName() +
                            "(" + String.join(", ", method.getType().parameters().keySet()) + ")";
                    if (method.isAsync()
                            || method.getType().returnType() != TSType.TSPrimitive.VOID) {
                        code = "return " + code;
                    }
                    method.setCode(code);
                }
                case TSMethod meth
                        when meth.getCode() == null || meth.getCode().isEmpty() -> {
                    var type = meth.getType();
                    if (type.returnType() == TSType.TSPrimitive.VOID) {
                        meth.setCode("return;");
                    } else {
                        meth.setCode("return {} as "+type.returnType());
                    }
                }
                default -> {}
            }
        });
        return element;
    }
}
