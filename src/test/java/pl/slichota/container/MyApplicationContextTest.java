package pl.slichota.container;

import org.junit.jupiter.api.Test;
import pl.slichota.container.exception.ApplicationContextException;
import pl.slichota.container.exception.ApplicationContextExceptionMessage;
import pl.slichota.container.test_classes.normal.A;
import pl.slichota.container.test_classes.normal.B;
import pl.slichota.container.test_classes.normal.C;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
public class MyApplicationContextTest {

    MyApplicationContext context;


    @Test
    public void context_can_create_instances() {
        context = new MyApplicationContext("pl.slichota.container.test_classes.normal");
        List<String> expected = Stream.of(A.class.getName(), B.class.getName(), C.class.getName()).sorted().toList();

        List<String> result = context.getBeanDefinitionNames().stream().sorted().toList();

        assertArrayEquals(expected.toArray(), result.toArray());
    }

    @Test
    public void context_can_create_bean_property() {
        context = new MyApplicationContext("pl.slichota.container.test_classes.normal");
        var result = context.getBean(B.class);
        assertNotNull(result.getA());
        assertNotNull(result.getC());
    }

    @Test
    public void context_create_singleton_bean() {
        context = new MyApplicationContext("pl.slichota.container.test_classes.normal");
        var result1 = context.getBean(A.class);
        var result2 = context.getBean(A.class);
        assertEquals(result1, result2);
    }

    @Test
    public void context_can_detect_non_bean_dependency() {
        Exception exception = assertThrows(ApplicationContextException.class,
                () -> new MyApplicationContext("pl.slichota.container.test_classes.non_bean_dependency"));
        String expectedMessage = ApplicationContextExceptionMessage.BEAN_NOT_FOUND.getMessage() + " class pl.slichota.container.test_classes.non_bean_dependency.B";
        String actualMessage = exception.getMessage();
        System.out.println(actualMessage);
        System.out.println(expectedMessage);
        assertTrue(actualMessage.equals(expectedMessage));
    }

    @Test
    public void context_can_detect_cycle() {
        Exception exception = assertThrows(ApplicationContextException.class,
                () -> new MyApplicationContext("pl.slichota.container.test_classes.cycle"));
        System.out.println(exception);
    }

    @Test
    public void context_can_detect_multiple_annotated_constructors() {
        assertThrows(ApplicationContextException.class,
                () -> new MyApplicationContext("pl.slichota.container.test_classes.multiple_constructors"),
                ApplicationContextExceptionMessage.MULTIPLE_CONSTRUCTORS.getMessage());
    }
}
