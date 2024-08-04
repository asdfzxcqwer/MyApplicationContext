package pl.slichota.container.test_classes.cycle;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class B {

    @Autowired
    private final A a;

    public B(A a) {
        this.a = a;
    }
}
