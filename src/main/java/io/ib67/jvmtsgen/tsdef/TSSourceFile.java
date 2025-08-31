package io.ib67.jvmtsgen.tsdef;

import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ToString
public final class TSSourceFile extends TSElement.TSCompoundElement {
    @Getter
    private final Path path;
    private final List<TSElement> elements;

    public TSSourceFile(Path path, List<TSElement> elements) {
        super(null);
        this.path = path;
        this.elements = elements;
    }

    @Override
    public List<TSElement> elements() {
        return elements;
    }

    @Override
    public Set<TSModifier> getModifiers() {
        return Set.of();
    }
}
