import io.ib67.jvmtsgen.strategy.AsisStrategy;
import io.ib67.kiwi.routine.Result;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

public class TestModelBuilder {
    @Test
    @SneakyThrows
    public void readSingle() {
        var strategy = new AsisStrategy();
        var cf = ClassFile.of();
        var outDir = Path.of("_out");
        if (Files.notExists(outDir)) Files.createDirectory(outDir);
        var testDatas = Path.of("test-data");
        try (var l = Files.walk(testDatas)) {
            l.filter(it -> it.toAbsolutePath().toString().endsWith("class"))
                    .map(i -> Result.fromAny(() -> cf.parse(i)).orElseThrow())
                    .forEach(strategy::scan);
        }
        strategy.output(outDir);
    }

    @Test
    @SneakyThrows
    public void readJar() {
        var jar = Path.of("/home/icybear/IdeaProjects/kiwi/lang/build/libs/lang-1.2.2-SNAPSHOT.jar");
        var strategy = new AsisStrategy();
        var cf = ClassFile.of();
        var outDir = Path.of("_out");
        if (Files.notExists(outDir)) Files.createDirectory(outDir);
        try (var zipFile = new ZipFile(jar.toFile())) {
            var iter = zipFile.entries().asIterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                if (!entry.getName().endsWith(".class")) continue;
                try (var is = zipFile.getInputStream(entry)) {
                    strategy.scan(cf.parse(is.readAllBytes()));
                }
            }
        }
        strategy.output(outDir);
    }
}
