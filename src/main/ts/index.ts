let java: any

export function newTest<G>(p0: String): Test<any> {
    return new java.Test(p0);
}

export function newTest<G>(): Test<any> {
    return new java.Test();
}

type Test<G> = {
    readonly readonlyField: String,
    field: G,
    genericMethod: <T>(p0: T, p1: G) => T
}
let Test: { factory: <A>() => Test<A> } = java.Test