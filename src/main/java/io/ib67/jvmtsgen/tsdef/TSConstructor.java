package io.ib67.jvmtsgen.tsdef;

import lombok.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class TSConstructor extends TSElement {
    private Set<TSAccessFlag> access;
    private Map<String, TSType> parameters;
    private String body;

    public TSConstructor(TSElement parent){
        super(parent);
        parameters = new HashMap<>();
        access = EnumSet.noneOf(TSAccessFlag.class);
        body = "";
    }
}
