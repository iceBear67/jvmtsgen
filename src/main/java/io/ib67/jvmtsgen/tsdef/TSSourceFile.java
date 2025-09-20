package io.ib67.jvmtsgen.tsdef;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@ToString
public final class TSSourceFile extends TSElement.TSCompoundElement {
    private final List<TSElement> elements;
    @Getter
    @Setter
    private Path path;
    // simplename -> path
    @Getter
    @Setter
    private Map<String, String> importTable;

    public TSSourceFile(Path path, List<TSElement> elements) {
        super(null);
        this.path = path;
        this.elements = elements;
        this.importTable = new HashMap<>();
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
