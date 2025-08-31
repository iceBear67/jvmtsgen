let java: any
export class Test<G> {
    public field: G
    public readonly readonlyField: string | null
    protected protectedField: number
    private privateField: G
    protected defaultAccessField: G
    private privateField() {}
    public genericMethod<T>(p0: T | null, p1: G): T {return {} as T}
    public static factory<A>(): Test<A> | null {return java.Test.factory()}
}
export function newTest<G>(p0: String): Test<G> {return new java.Test(p0);}
export function newTest<G>(): Test<G> {return new java.Test();}