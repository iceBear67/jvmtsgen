package io.ib67.jvmtsgen.tsdef;

import io.ib67.kiwi.routine.Uni;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@ToString
public abstract class TSElement {
    @Getter
    @Setter
    protected TSElement parent;

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
