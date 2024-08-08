## About the project
Basic IoC (Inversion of Control) container inspired by Application Context used in Spring framework.

An Inversion of Control (IoC) container is a design pattern used in software development to manage the instantiation, 
configuration, and lifecycle of objects. This pattern is a key component of Dependency Injection (DI), where the control 
of creating and managing dependencies is inverted from the object itself to an external container.
## How to use it?
In main function, we have to create object of `ApplicationContext` like `MyApplicationContext` by using the constructor.
When the constructor is called, whole package and subpackages will be scanned in order to find all beans that will be injected.
### Basic annotations:

`@Component` - This annotation is used to mark used to mark a class as a bean. When component-scanning  mechanism finds
a class annotated with @Component, it will automatically create an instance of that class and register it
in the application context.

Example:
```java
@Component
public class MyComponent {
    public void doSomething() {
        System.out.println("Doing something");
    }
}
```


`@Autowired` - This annotation is used to mark dependencies of class and can be used to mark a constructor or field,
When `ApplicationContext` dependency injection mechanism finds this annotation, it automatically resolves and injects the
necessary dependency. It worth to notice that only one constructor of a class can be mark with this annotation. Otherwise,
the exception will be thrown. Also, there is possibility to use this annotation either in fields or constructor. But constructor
injection has higher priority.

#### Example when @Autowired is used in constructor:
```java
@Component
public class B {

    private final C c;

    private final A a;

    @Autowired
    public B(C c, A a) {
        this.c = c;
        this.a = a;
    }
}
```

#### Example when @Autowired is used in field:
```java
@Component
public class A {

    @Autowired
    private final B b;

    public A(B b) {
        this.b = b;
    }
}
```

## TODO:
- remove bean from context
- get startup time