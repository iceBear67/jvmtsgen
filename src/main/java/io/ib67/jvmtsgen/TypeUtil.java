package io.ib67.jvmtsgen;

import io.ib67.jvmtsgen.tsdef.*;

import java.lang.classfile.AccessFlags;
import java.lang.reflect.AccessFlag;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public class TypeUtil {
    private static final EnumSet<AccessFlag> INVISIBLE = EnumSet.of(
            AccessFlag.BRIDGE,
            AccessFlag.SYNTHETIC,
            AccessFlag.MANDATED
    );

    public static boolean hasInvisible(AccessFlags flags) {
        for (AccessFlag f : INVISIBLE) {
            if (flags.has(f)) return true;
        }
        return false;
    }

    public static boolean isVisible(AccessFlag flag) {
        return !INVISIBLE.contains(flag);
    }

    public static String toString(TSElement element, Set<TSAccessFlag> flags) {
        var str = new StringBuilder();
        var isClass = element instanceof TSClassDecl;
        var isField = element instanceof TSFieldDecl;
        var isMethod = element instanceof TSMethod;
        var isConstructor = element instanceof TSConstructor;
        var isVarDecl = element instanceof TSVarDecl;
        var isWithinClass = element.getParent() instanceof TSClassDecl;
        if (flags.contains(TSAccessFlag.EXPORT)) {
            if (!isClass && !isVarDecl && (isMethod && isWithinClass))
                throw new UnsupportedOperationException("Cannot annotate export on " + element);
            str.append("export ");
        }
        if (flags.contains(TSAccessFlag.PUBLIC)) {
            if (flags.contains(TSAccessFlag.PRIVATE) || (!isMethod && !isField && !isConstructor) || !isWithinClass)
                throw new UnsupportedOperationException("Cannot annotate public on " + element);
            str.append("public ");
        }
        if (flags.contains(TSAccessFlag.PRIVATE)) {
            if (isWithinClass || (!isMethod && !isField && !isConstructor))
                throw new UnsupportedOperationException("Cannot annotate private on " + element);
            str.append("private ");
        }
        if (flags.contains(TSAccessFlag.ASYNC)) {
            if (!isMethod)
                throw new UnsupportedOperationException("Cannot annotate async on " + element);
            str.append("async ");
        }
        if (flags.contains(TSAccessFlag.STATIC)) {
            if (!isMethod && !isField)
                throw new UnsupportedOperationException("Cannot annotate static on " + element);
            str.append("static ");
        }
        if (flags.contains(TSAccessFlag.READ_ONLY)) {
            if (!isField)
                throw new UnsupportedOperationException("Cannot annotate readonly on " + element);
            str.append("readonly ");
        }
        if (flags.contains(TSAccessFlag.ABSTRACT)) {
            if (!isWithinClass || (!isClass && !isMethod && !isField))
                throw new UnsupportedOperationException("Cannot annotate abstract on "+element);
            str.append("abstract ");
        }
        return str.toString();
    }

    public static String typeParamString(Map<String, TSType> typeParam) {
        if (typeParam.isEmpty()) return "";
        return '<' +
                typeParam.entrySet().stream()
                        .map(e -> e.getValue() != TSType.TSPrimitive.ANY ? e.getKey() + " extends " + e.getValue() : e.getKey())
                        .collect(Collectors.joining(",")) +
                '>';
    }

}
