package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.ClassModelManager;
import io.ib67.jvmtsgen.ModelBuilder;
import io.ib67.jvmtsgen.TSourceTree;
import io.ib67.jvmtsgen.tsdef.TSType;

import java.lang.classfile.ClassModel;
import java.util.Map;

public interface TransformerContext {
    TSourceTree getSourceTree();
    ModelBuilder getModelBuilder();
    ClassModelManager getClassModels();
    String stubNameOf(String javaInternalName);
}
