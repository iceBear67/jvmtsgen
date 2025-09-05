package io.ib67.jvmtsgen.strategy;

import io.ib67.jvmtsgen.ModelBuilder;
import io.ib67.jvmtsgen.TSourceTree;
import io.ib67.jvmtsgen.TypeScriptModel;
import io.ib67.jvmtsgen.pass.*;
import io.ib67.jvmtsgen.pass.canonicalizer.FunctionTypeCanonicalizer;
import io.ib67.jvmtsgen.pass.canonicalizer.GenericTypeCanonicalizer;
import io.ib67.jvmtsgen.pass.canonicalizer.PrimitiveCanonicalizer;
import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSSourceFile;
import io.ib67.jvmtsgen.writer.TypeScriptWriter;
import io.ib67.kiwi.routine.Result;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.lang.classfile.ClassModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Log4j2
public class AsisStrategy implements TransformerContext {
    protected final TSourceTree.Directory root = new TSourceTree.Directory(null, new HashMap<>());
    protected final List<ElementPass> passes;
    protected final TypeScriptWriter writer = new TypeScriptWriter();

    public AsisStrategy() {
        passes = List.of(
                new TypeCanonicalizePass(List.of(PrimitiveCanonicalizer.INSTANCE, GenericTypeCanonicalizer.INSTANCE, FunctionTypeCanonicalizer.INSTANCE))
                ,PromiseRewritePass.builder().thenables(Set.of("Ljava/util/concurrent/Future;")).build()
                ,RenamerPass.builder().asis(true).build()
                ,PlaceholderCodePass.INSTANCE
                ,HoistingPass.builder().hoistConstructor(true).hoistStaticMethod(false).build()
//                ,VisibilityFilterPass.INSTANCE
//                , TypeDefPass.builder().build()
        );
    }

    public void output(Path path) {
        TSourceTree.Directory root = this.root;
        for (ElementPass pass : passes) {
            var newRoot = new TSourceTree.Directory(null, new HashMap<>());
            var unknownPackageFile = new TSSourceFile(Path.of("_orphan.ts"), new ArrayList<>());
            join(unknownPackageFile, newRoot);
            root.traverse().onItem(it -> {
                if (it instanceof TSourceTree.File f) {
                    var result = pass.transform(this, f.content());
                    if (result instanceof TSSourceFile _f && _f.getPath() != null) {
                        join(_f, newRoot);
                    } else unknownPackageFile.elements().add(result);
                }
            });
            root = newRoot;
        }
        root.traverse().onItem(it->{
            if(!(it instanceof TSourceTree.File(_, var content))) return;
            Result.runAny(() -> {
                var file = path.resolve(content.getPath());
                if(Files.notExists(file.getParent())) Files.createDirectories(file.getParent());
                var output = writer.generate(content);
                if(output == null) {
                    log.error("Output of {} is null", file);
                    return;
                }
                Files.writeString(file, output);
            }).orElseThrow();
        });
    }

    public void processPass(TSElement element, ElementPass pass) {
        pass.transform(this, element);
    }

    public void scan(ClassModel cm) {
        var start = System.currentTimeMillis();
        var tsf = transform(cm);
        tsf.setPath(Path.of(cm.thisClass().asInternalName()+".ts"));
        join(tsf, root);
        log.info("Processed class {} ({}ms)", cm.thisClass().asInternalName(), System.currentTimeMillis() - start);
    }

    protected void join(TSSourceFile f, TSourceTree.Directory root) {
        var p = f.getPath();
        var parent = p.getParent();
        var d = parent == null
                ? root : root.getObject(parent.toString()).orElseThrow();
        ((TSourceTree.Directory) d).entries().put(p.getFileName().toString(), new TSourceTree.File(d, f));
    }

    protected TSSourceFile transform(ClassModel model) {
        var tsf = new TSSourceFile(null, new ArrayList<>());
        var modelBuilder = ModelBuilder.builder()
                .generateSynthetic(true)
                .nullableByDefault(false)
                .checkNull(true)
                .build();
        modelBuilder.write(new TypeScriptModel(tsf), model);
        return tsf;
    }

    @Override
    public String stubNameOf(String javaInternalName) {
        return "java." + javaInternalName.replace('/', '.');
    }
}
