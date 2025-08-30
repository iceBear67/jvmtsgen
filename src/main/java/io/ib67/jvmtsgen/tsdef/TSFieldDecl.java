package io.ib67.jvmtsgen.tsdef;

import lombok.*;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class TSFieldDecl extends TSElement{
    private TSVarDecl variableDecl;
    private Set<TSAccessFlag> accessFlags;

    public TSFieldDecl(TSElement parent, TSVarDecl variableDecl) {
        super(parent);
        Objects.requireNonNull(variableDecl);
        this.variableDecl = variableDecl;
        accessFlags = EnumSet.noneOf(TSAccessFlag.class);
    }
}
