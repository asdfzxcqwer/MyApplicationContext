package pl.slichota;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;
import wadw.C;

@Component
public class B {

    @Autowired
    private final C c;

    @Autowired
    private final A a;


    public B(C c, A a) {
        this.c = c;
        this.a = a;
    }
}
