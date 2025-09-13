package io.ib67.jvmtsgen;

import io.ib67.kiwi.routine.Result;
import io.ib67.kiwi.routine.Uni;

import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClassModelManager {
    protected final Map<String, ClassModel> classModels = new HashMap<>();

    public void addClass(ClassModel model) {
        classModels.put("L"+model.thisClass().asInternalName()+";", model);
    }

    public Optional<ClassModel> getClassModel(String className) {
        return Optional.ofNullable(classModels.get("L"+className+";"));
    }

    public Optional<ClassModel> getByDescriptor(String descriptor) {
        return Optional.ofNullable(classModels.get(descriptor));
    }

    public Uni<String> findAncestors(String name) {
        var clazz = classModels.get(name);
        if (clazz == null) return Uni.of();
        return traverseAncestors(clazz);
    }

    protected Uni<String> traverseAncestors(ClassModel model) {
        return Uni.from(c -> {
            Uni.from(model.interfaces()::forEach)
                    .merge(model.superclass()::ifPresent)
                    .map(ClassEntry::asInternalName)
                    .peek(c)
                    .flatMap(it -> getClassModel(it)::ifPresent);
//                    .onItem(it -> traverseAncestors(it).onItem(c));
        });
    }
}
