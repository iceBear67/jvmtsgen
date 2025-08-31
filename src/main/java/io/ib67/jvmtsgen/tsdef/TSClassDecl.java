package io.ib67.jvmtsgen.tsdef;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@Getter
@Setter
@ToString
public class TSClassDecl extends TSElement.TSCompoundElement {
    private Set<TSModifier> modifiers;
    private TSType.TSClass type;
    private List<TSElement> elements;
    private boolean isInterface;
    private String javaInternalName;

    public TSClassDecl(TSElement parent, String name, Map<String, TSType> typeArgs) {
        super(parent);
        type = new TSType.TSClass(name, typeArgs);
        elements = new ArrayList<>();
        modifiers = EnumSet.noneOf(TSModifier.class);
    }

    @Override
    public List<TSElement> elements() {
        return elements;
    }
}
