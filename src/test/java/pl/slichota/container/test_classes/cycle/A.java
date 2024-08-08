package pl.slichota.container.test_classes.cycle;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class A {

    @Autowired
    private final B b;

    public A(B b) {
        this.b = b;
    }
}
