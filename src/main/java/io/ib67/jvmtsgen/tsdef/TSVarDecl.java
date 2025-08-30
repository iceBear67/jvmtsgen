package io.ib67.jvmtsgen.tsdef;

import lombok.*;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class TSVarDecl extends TSElement {
    private String name;
    private TSType type;
    private String defaultInitializer;

    public TSVarDecl(TSElement parent, String name, TSType type) {
        super(parent);
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }
}
