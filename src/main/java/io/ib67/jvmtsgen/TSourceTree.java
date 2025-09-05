package io.ib67.jvmtsgen;

import io.ib67.jvmtsgen.tsdef.TSSourceFile;
import io.ib67.jvmtsgen.tsdef.TSType;
import io.ib67.kiwi.routine.Result;
import io.ib67.kiwi.routine.Uni;
import lombok.With;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public sealed interface TSourceTree {
    TSourceTree parent();

    @With
    record Directory(
            TSourceTree parent,
            Map<String, TSourceTree> entries
    ) implements TSourceTree {
    }

    @With
    record File(
            TSourceTree parent,
            TSSourceFile content
    ) implements TSourceTree {
    }

    private TSourceTree getObject0(String path) {
        if (!(this instanceof Directory d)) {
            throw new UnsupportedOperationException("Cannot get objects from a " + this);
        }
        var result = d.entries().get(path);
        if (result == null) {
            var nd = new Directory(this, new HashMap<>());
            d.entries.put(path, nd);
            return nd;
        }
        return result;
    }

    default Result<TSourceTree> getObject(String... path) {
        var current = new AtomicReference<>(this);
        return Uni.of(path)
                .map(it -> it.split("/"))
                .flatMap(Uni::of)
                .mapFallible(it -> current.get().getObject0(it))
                .flatMap(Function.identity())
                .last();
    }

    default Uni<TSourceTree> traverse() {
        return c -> {
            switch (this) {
                case Directory d -> {
                    c.onValue(d);
                    Uni.from(d.entries().values()::forEach).flatMap(TSourceTree::traverse).onItem(c);
                }
                case File f -> c.onValue(f);
            }
        };
    }

    default void transform(UnaryOperator<TSourceTree> transform) {
        traverse().onItem(it -> {
            if (!(it instanceof Directory d)) return;
            for (var entry : d.entries().entrySet()) {
                entry.setValue(transform.apply(entry.getValue()));
            }
        });
    }
}
