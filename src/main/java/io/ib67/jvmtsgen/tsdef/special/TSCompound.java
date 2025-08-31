package io.ib67.jvmtsgen.tsdef.special;

import io.ib67.jvmtsgen.tsdef.TSElement;
import io.ib67.jvmtsgen.tsdef.TSModifier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
public class TSCompound extends TSElement.TSCompoundElement {
    protected final List<TSElement> elements;

    public TSCompound(TSElement parent, List<TSElement> elements) {
        super(parent);
        this.elements = elements;
    }

    @Override
    public List<TSElement> elements() {
        return elements;
    }

    @Override
    public Set<TSModifier> getModifiers() {
        return Set.of();
    }
}
