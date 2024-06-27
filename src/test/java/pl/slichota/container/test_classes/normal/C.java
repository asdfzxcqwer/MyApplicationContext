package pl.slichota.container.test_classes.normal;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class C {

    @Autowired
    private A a;
}
