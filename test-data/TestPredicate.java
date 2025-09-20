import java.lang.FunctionalInterface;


@FunctionalInterface
public interface TestPredicate<A> {
	boolean check(A a);
	default void defaultMethod(){}
}
