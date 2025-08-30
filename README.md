# jvmtsgen

TypeScript definition generator for GraalJS-based programs. Currently working in progress.

# Type Conversion Rule

| TS        | Java                                                                                                                                                   | Comment                                                                                                                              |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| int       | Integer types                                                                                                                                          | integer is an alias of `number` primitive, indicating the supplied, or wanted number should be a integer. (no matter how long it is) |
| float     | float, double                                                                                                                                          | alias of `number` primitive. like `int`, indicating floating numbers are present                                                     |
| string    | String                                                                                                                                                 |                                                                                                                                      |
| Promise   | [Thenable](https://www.graalvm.org/jdk24/reference-manual/js/JavaInteroperability/#creating-javascript-promise-objects-that-can-be-resolved-from-java) |                                                                                                                                      |
| array<T>  | List<T>                                                                                                                                                |
| enum      | enum                                                                                                                                                   |
| null      | null                                                                                                                                                   |
| T \| null | @Nullable T                                                                                                                                            | Only matches the annotation's simple name                                                                                            |
| T         | T                                                                                                                                                      | where T is generated from codegen. By default, we asserts that's not null, unless explicitly specified.                              |
| T         | @NotNull T                                                                                                                                             |                                                                                                                                      |
| void      | void                                                                                                                                                   |                                                                                                                                      |
| T: Map    | Map<String, Object>                                                                                                                                    | 

## Styles
Only `as-is` is implemented in this time. Other styles are under discussion, since package bundlers are likely able to 
do these things.

1. `as-is`: One class to one ts definition.
2. `reduce-by-package`: Group Java types into one typescript files by its package.
3. `bundled`: Bundle all types into one typescript file. Classes with conflict names will both use canonical names instead.

# Sample
Some sample output from the first prototype.

Input:
```java
public class Test<G> {
  public static Test<String> t;
  public static String s;
  public static <T,Z > T genericMethodStatic(Z z, String s){ return null; }
  public G genericMethod() { return null; }
}
```

Output:
```typescript
let java: any
export class Test<G> {
    t: Test<String>
    s: String
    public genericMethod(): G {return {} as G}
}
export function newTest<G>(): Test<any> {return new java.Test();}
function genericMethodStatic<T,Z>(p0: Z, p1: String): T {return java.Test.genericMethodStatic(p0,p1);}
```

