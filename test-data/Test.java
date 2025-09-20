import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.concurrent.Future;

public class Test<G> {
    public G field;
    @Nullable
    public final String readonlyField;
    protected Integer protectedField;
    private G privateField;
    G defaultAccessField;

    private void privateField(String veryLongParameterNameMakeMeSpin) {
    }

    public void privateField(){}

    public <T> T genericMethod(@Nullable T t, G g) {
        return null;
    }

    public Future<String> awaitable(){
        return null;
    }

    @Nullable
    public static <A> Test<A> factory() {
        return null;
    }

    public void overloadA(String s){}

    public void overloadA(int i){}

    public void overloadB(String s){}

    public void overloadB(){}

    public Test(String constr) {
        this.readonlyField = constr;
    }

    public Test() {
        this(null);
    }

    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
    public @interface Nullable {

    }
}
