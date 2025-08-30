package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.TSElement;

public interface ElementPass {
    TSElement transform(TransformerContext context, TSElement element);
}
