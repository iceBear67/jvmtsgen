declare let java: any

declare class Test<G> {
    t: Test<any>;
    s: String;

    constructor();

    public genericMethodStatic<T, Z>(p0: Z, p1: String): T;

    public genericMethod(): G;
}

declare abstract class T{
    abstract functi();
    abstract field: string;
}
declare type a = {}