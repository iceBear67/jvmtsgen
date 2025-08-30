package io.ib67.jvmtsgen.tsdef;

import io.ib67.jvmtsgen.TypeUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class TSClassDecl extends TSElement.TSCompoundElement {
    private Set<TSAccessFlag> access;
    private TSType.TSClass type;
    private List<TSElement> elements;
    private boolean isInterface;
    private String javaInternalName;

    public TSClassDecl(TSElement parent, String name, Map<String, TSType> typeArgs) {
        super(parent);
        type = new TSType.TSClass(name, typeArgs);
        elements = new ArrayList<>();
        access = EnumSet.noneOf(TSAccessFlag.class);
    }

    @Override
    public List<TSElement> elements() {
        return elements;
    }
}
