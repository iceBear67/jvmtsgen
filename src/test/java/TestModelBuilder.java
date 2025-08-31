import io.ib67.jvmtsgen.ModelBuilder;
import io.ib67.jvmtsgen.TypeScriptModel;
import io.ib67.jvmtsgen.strategy.AsisStrategy;
import io.ib67.jvmtsgen.tsdef.TSSourceFile;
import io.ib67.jvmtsgen.writer.TypeScriptWriter;
import io.ib67.jvmtsgen.writer.WriterFeature;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.nio.file.Path;
import java.util.ArrayList;

public class TestModelBuilder {
    @SneakyThrows
    @Test
    public void testSignature() {

        var strategy = new AsisStrategy();
        var path = Path.of("/home/icybear/IdeaProjects/jvmtsgen/Test.class");
        var cf = ClassFile.of();
        var model = cf.parse(path);
        var tsf = strategy.transform(model);
        System.out.println(new TypeScriptWriter().generate(tsf));
    }

}
