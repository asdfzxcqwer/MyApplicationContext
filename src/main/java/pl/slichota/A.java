package pl.slichota;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class A {

    @Autowired
    private B b;

    @Autowired
    private C c;

}
