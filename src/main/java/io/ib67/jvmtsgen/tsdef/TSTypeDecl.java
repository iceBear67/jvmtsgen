package io.ib67.jvmtsgen.tsdef;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class TSTypeDecl extends TSElement {
    private String name;
    private TSType type;
    private Set<TSModifier> modifiers;
    private Map<String, TSType> typeParam;
}
