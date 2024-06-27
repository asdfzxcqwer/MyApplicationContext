package pl.slichota.container.test_classes.normal;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class B {


    @Autowired
    private C c;

    @Autowired
    private A a;
}
