package pl.slichota.container.test_classes.normal;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class B {

    @Autowired
    private final C c;

    @Autowired
    private final A a;

    @Autowired
    public B(C c, A a) {
        this.c = c;
        this.a = a;
    }

    public C getC() {
        return c;
    }

    public A getA() {
        return a;
    }
}
