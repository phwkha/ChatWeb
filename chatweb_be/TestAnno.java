@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface MyAnno {
    String value();
}

@MyAnno(TestAnno.MY_VAL)
public class TestAnno {
    public static final String MY_VAL = "hello";
}
