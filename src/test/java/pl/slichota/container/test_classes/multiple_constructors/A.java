package pl.slichota.container.test_classes.multiple_constructors;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class A {

    @Autowired
    public A() {

    }

    @Autowired
    public A(int i) {

    }
}
