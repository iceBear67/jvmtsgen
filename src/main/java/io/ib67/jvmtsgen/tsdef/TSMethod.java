package io.ib67.jvmtsgen.tsdef;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class TSMethod extends TSElement {
    private String name;
    private Set<TSAccessFlag> access;
    private TSType.TSFunction type;
    private String code;

    public TSMethod(TSElement parent, String name, TSType.TSFunction type) {
        super(parent);
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
        access = EnumSet.noneOf(TSAccessFlag.class);
    }
}
