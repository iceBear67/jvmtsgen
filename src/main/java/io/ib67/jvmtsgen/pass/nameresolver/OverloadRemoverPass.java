package io.ib67.jvmtsgen.pass.nameresolver;

import io.ib67.jvmtsgen.tsdef.*;

import javax.annotation.Nullable;
import java.util.List;

public enum OverloadRemoverPass implements OverloadPass {
    INSTANCE;

    @Nullable
    @Override
    public TSMethod tryMergeOverload(TSElement.TSCompoundElement clazz, List<TSElement> method) {
        method.sort((a, b) -> {
            if (a.getModifiers().contains(TSModifier.PRIVATE)) {
                return -1;
            } else if (b.getModifiers().contains(TSModifier.PRIVATE)) {
                return 1;
            } else {
                return 0;
            }
        });
        for (int i = 0; i < method.size(); i++) {
            if (i != method.size() - 1) clazz.elements().remove(method.get(i));
        }
        return null;
    }
}
