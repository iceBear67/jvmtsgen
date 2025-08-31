package io.ib67.jvmtsgen.tsdef;

import lombok.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class TSVarDecl extends TSElement {
    private String name;
    private TSType type;
    private Set<TSModifier> modifiers;
    private String defaultInitializer;

    public TSVarDecl(String name, TSType type, String initializer) {
        this(name, type, EnumSet.noneOf(TSModifier.class), initializer);
    }

    public TSVarDecl(TSElement parent, String name, TSType type) {
        super(parent);
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }
}
