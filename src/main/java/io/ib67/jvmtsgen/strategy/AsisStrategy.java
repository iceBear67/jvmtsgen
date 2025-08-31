package io.ib67.jvmtsgen.strategy;

import io.ib67.jvmtsgen.ModelBuilder;
import io.ib67.jvmtsgen.TypeScriptModel;
import io.ib67.jvmtsgen.pass.*;
import io.ib67.jvmtsgen.pass.canonicalizer.PrimitiveCanonicalizer;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSSourceFile;

import java.lang.classfile.ClassModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AsisStrategy implements TransformerContext {
    protected final List<ElementPass> passes;

    public AsisStrategy() {
        passes = List.of(
                new TypeCanonicalizePass(List.of(PrimitiveCanonicalizer.INSTANCE)),
                RenamerPass.builder().asis(true).build(),
                HoistingPass.builder().hoistConstructor(true).hoistStaticMethod(false).build()
//                TypeDefPass.builder().build()
        );
    }

    public TSSourceFile transform(ClassModel model) {
        Path savePath = Path.of("");
        var tsf = new TSSourceFile(savePath, new ArrayList<>());
        var modelBuilder = ModelBuilder.builder()
                .context(this)
                .writer(new TypeScriptModel(tsf))
                .nullableByDefault(false)
                .checkNull(true)
                .build();
        modelBuilder.write(model);
        TSElement ele = tsf;
        for (ElementPass pass : passes) {
            ele = pass.transform(this, ele);
        }
        if (ele instanceof TSSourceFile _tsf) return _tsf;
        return new TSSourceFile(savePath, List.of(ele));
    }

    @Override
    public String stubNameOf(String javaInternalName) {
        return "java." + javaInternalName.replace('/', '.');
    }
}
