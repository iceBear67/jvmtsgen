let java: any
export class Test<G> {
    t: Test<String>
    s: String
    public genericMethod(): G {return {} as G}
}
export function newTest<G>(): Test<any> {return new java.Test();}
function genericMethodStatic<T,Z>(p0: Z, p1: String): T {return java.Test.genericMethodStatic(p0,p1);}