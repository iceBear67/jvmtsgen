package io.ib67.jvmtsgen.pass;

import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSModifier;

public enum VisibilityFilterPass implements ElementPass{
    INSTANCE;
    @Override
    public TSElement transform(TransformerContext context, TSElement element) {
        traverse(element).filter(it->it.getModifiers().contains(TSModifier.PRIVATE))
                .onItem(it->{
                    if(it.getParent() instanceof TSElement.TSCompoundElement c){
                        c.elements().remove(it);
                    }
                });
        return element;
    }
}
