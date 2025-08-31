let java: any

export function newTest<G>(p0: String): Test<any> {
    return new java.Test(p0);
}

export function newTest<G>(): Test<any> {
    return new java.Test();
}

export type Test<G> = {
    readonly readonlyField: string | null,
    readonly awaitable: () => Promise<String>,
    field: G,
    readonly genericMethod: <T>(p0: T | null, p1: G) => T
}
export let Test: { readonly factory: <A>() => Test<A> | null } = java.Test