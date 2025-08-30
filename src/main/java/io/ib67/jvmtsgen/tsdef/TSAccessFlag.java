package io.ib67.jvmtsgen.tsdef;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.classfile.AccessFlags;
import java.lang.reflect.AccessFlag;
import java.util.EnumSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum TSAccessFlag {
    ASYNC("async"),
    READ_ONLY("readonly"),
    EXPORT("export"),
    PUBLIC("public"),
    STATIC("static"),

    PRIVATE("private"),
    ABSTRACT("abstract");

    private final String keyword;

    public static Set<TSAccessFlag> from(AccessFlags flag) {
        var set = EnumSet.noneOf(TSAccessFlag.class);
        if (flag.has(AccessFlag.PUBLIC)) {
            set.add(TSAccessFlag.PUBLIC);
        }
        if (flag.has(AccessFlag.PRIVATE)) {
            set.add(TSAccessFlag.PRIVATE);
        }
        if(flag.has(AccessFlag.FINAL)){
            set.add(TSAccessFlag.READ_ONLY);
        }
        if(flag.has(AccessFlag.STATIC)){
            set.add(TSAccessFlag.STATIC);
        }
        if(flag.has(AccessFlag.ABSTRACT)){
            set.add(TSAccessFlag.ABSTRACT);
        }
        return set;
    }
}
