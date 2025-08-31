declare let java: any

export function newTest<G>(p0: String): Test<G>
export function newTest<G>(): Test<G>

export declare type Test<G> = {
    readonly readonlyField: string | null,
    readonly awaitable: () => Promise<String>,
    field: G,
    readonly genericMethod: <T>(p0: T | null, p1: G) => T
}
export declare let Test: { readonly factory: <A>() => Test<A> | null }