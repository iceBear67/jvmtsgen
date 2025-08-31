package io.ib67.jvmtsgen.tsdef;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.classfile.AccessFlags;
import java.lang.reflect.AccessFlag;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum TSModifier {
    READ_ONLY("readonly"),
    EXPORT("export"),
    PUBLIC("public"),
    PROTECTED("protected"),
    STATIC("static"),

    PRIVATE("private"),
    ABSTRACT("abstract");

    private final String keyword;
    private static final Map<AccessFlag, TSModifier> accessMap = Map.of(
            AccessFlag.PUBLIC, PUBLIC,
            AccessFlag.PRIVATE, PRIVATE,
            AccessFlag.PROTECTED, PROTECTED,
            AccessFlag.FINAL, READ_ONLY,
            AccessFlag.STATIC, STATIC,
            AccessFlag.ABSTRACT, ABSTRACT
    );

    public static Set<TSModifier> from(AccessFlags flag) {
        var set = EnumSet.noneOf(TSModifier.class);
        for (AccessFlag accessFlag : flag.flags()) {
            var corresponding = accessMap.get(accessFlag);
            if(corresponding != null) set.add(corresponding);
        }
        if (!set.contains(PROTECTED) && !set.contains(PRIVATE) && !set.contains(PUBLIC))
            set.add(TSModifier.PROTECTED);

        return set;
    }
}
