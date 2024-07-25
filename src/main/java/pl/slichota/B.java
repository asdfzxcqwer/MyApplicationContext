package pl.slichota;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;

@Component
public class B {

    @Autowired
    private D d;
}
