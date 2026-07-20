@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface MyAnno {
    String value();
}

@MyAnno(MY_VAL)
public class TestAnno2 {
    public static final String MY_VAL = "hello";
}
