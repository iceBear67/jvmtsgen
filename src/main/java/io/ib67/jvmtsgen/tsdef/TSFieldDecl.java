package io.ib67.jvmtsgen.tsdef;

import lombok.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class TSFieldDecl extends TSElement{
    private TSVarDecl variableDecl;
    private Set<TSModifier> modifiers;

    public TSFieldDecl(TSElement parent, TSVarDecl variableDecl) {
        super(parent);
        Objects.requireNonNull(variableDecl);
        this.variableDecl = variableDecl;
        modifiers = EnumSet.noneOf(TSModifier.class);
    }
}
