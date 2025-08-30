package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.TSType;

import java.util.Map;

public interface TransformerContext {
    String stubNameOf(String javaInternalName);
}
