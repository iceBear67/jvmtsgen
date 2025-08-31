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

    public static String toString(TSElement element, Set<TSModifier> flags) {
        var str = new StringBuilder();
        var isClass = element instanceof TSClassDecl;
        var isField = element instanceof TSFieldDecl;
        var isMethod = element instanceof TSMethod;
        var isConstructor = element instanceof TSConstructor;
        var isVarDecl = element instanceof TSVarDecl;
        var isWithinClass = element.getParent() instanceof TSClassDecl;
        if (flags.contains(TSModifier.EXPORT)) {
            if (!isClass && !isVarDecl && (isMethod && isWithinClass))
                throw new UnsupportedOperationException("Cannot annotate export on " + element);
            str.append("export ");
        }

        var hasAccessModifier = false;
        if (flags.contains(TSModifier.PUBLIC)) {
            if ((!isMethod && !isField && !isConstructor) || !isWithinClass)
                throw new UnsupportedOperationException("Cannot annotate public on " + element);
            str.append("public ");
            hasAccessModifier = true;
        }
        if (flags.contains(TSModifier.PRIVATE)) {
            if (!isWithinClass || (!isMethod && !isField && !isConstructor) || hasAccessModifier)
                throw new UnsupportedOperationException("Cannot annotate private on " + element);
            str.append("private ");
            hasAccessModifier = true;
        }
        if(flags.contains(TSModifier.PROTECTED)){
            if(!isWithinClass || (!isMethod && !isField && !isConstructor) || hasAccessModifier)
                throw new UnsupportedOperationException("Cannot annotate protected on " + element);
            str.append("protected ");
            hasAccessModifier = true;
        }

        if (flags.contains(TSModifier.ASYNC)) {
            if (!isMethod)
                throw new UnsupportedOperationException("Cannot annotate async on " + element);
            str.append("async ");
        }
        if (flags.contains(TSModifier.STATIC)) {
            if (!isMethod && !isField)
                throw new UnsupportedOperationException("Cannot annotate static on " + element);
            str.append("static ");
        }
        if (flags.contains(TSModifier.READ_ONLY)) {
            if(isField) {
                str.append("readonly ");
            } else if (isVarDecl) {
                str.append("const ");
            }else{
                throw new UnsupportedOperationException("Cannot annotate readonly on " + element);
            }
        }
        if (flags.contains(TSModifier.ABSTRACT)) {
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
