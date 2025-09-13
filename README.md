# jvmtsgen

TypeScript definition generator for GraalJS-based programs. Currently working in progress.

## Features

This generator is designed to use with GraalJS with an extra constraint: Library codes aren't allowed to be bundled

 - Partially support for method overloading
 - Primitive Java types are canonicalized to corresponding TS types, including  `Promise<T>`
 - Generic types are fully supported.
 - Stay in TypeScript, constructor can be hoisted into factory methods, eliminating the need of using `new` on `java` stubs.
 - Highly customizable.

## Styles

Only `as-is` is implemented in this time. Other styles are under discussion, since package bundlers are likely able to
do these things.

1. `as-is`: One class to one ts definition.
2. `reduce-by-package`: Group Java types into one typescript files by its package.
3. `bundled`: Bundle all types into one typescript file. Classes with conflict names will both use canonical names
   instead.

# Known Limitation

1. Method overloading or duplicated names in a same type is not allowed.
    - However, Method overload with same return type, same modifier and same number of parameters are allowed and they
      will be merged into union types.
    - You can also apply OverloadRemoverPass to keep only one member.

# Type Conversion Rule

| TS        | Java                                                                                                                                                   | Comment                                                                                                 |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| number    | Number types                                                                                                                                           |                                                                                                         |
| string    | String                                                                                                                                                 |                                                                                                         |
| Promise   | [Thenable](https://www.graalvm.org/jdk24/reference-manual/js/JavaInteroperability/#creating-javascript-promise-objects-that-can-be-resolved-from-java) |                                                                                                         |
| array<T>  | List<T>                                                                                                                                                |
| enum      | enum                                                                                                                                                   |
| null      | null                                                                                                                                                   |
| T \| null | @Nullable T                                                                                                                                            | Only matches the annotation's simple name                                                               |
| T         | T                                                                                                                                                      | where T is generated from codegen. By default, we asserts that's not null, unless explicitly specified. |
| T         | @NotNull T                                                                                                                                             |                                                                                                         |
| void      | void                                                                                                                                                   |                                                                                                         |
| T         | ? extends T                                                                                                                                            |                                                                                                         |
