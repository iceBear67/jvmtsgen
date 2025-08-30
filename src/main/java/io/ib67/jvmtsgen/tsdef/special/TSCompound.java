package io.ib67.jvmtsgen.tsdef.special;

import io.ib67.jvmtsgen.tsdef.TSElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
}
