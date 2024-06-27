package pl.slichota;


import pl.slichota.container.ApplicationContext;
import pl.slichota.container.MyApplicationContext;

public class Main {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new MyApplicationContext(Main.class);
    }


}