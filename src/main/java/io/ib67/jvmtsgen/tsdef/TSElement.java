package io.ib67.jvmtsgen.tsdef;

import io.ib67.kiwi.routine.Uni;
import lombok.*;

import java.util.List;
import java.util.Set;

@AllArgsConstructor
@ToString
public abstract class TSElement {
    @Getter
    @Setter
    protected TSElement parent;

    public abstract Set<TSModifier> getModifiers();

    public TSElement(){
        this(null);
    }

    public abstract static class TSCompoundElement extends TSElement {
        public TSCompoundElement(TSElement parent) {
            super(parent);
        }

        public abstract List<TSElement> elements();
    }
}
