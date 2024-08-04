package pl.slichota.container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.slichota.container.test_classes.normal.A;
import pl.slichota.container.test_classes.normal.B;
import pl.slichota.container.test_classes.normal.C;
import pl.slichota.cycle.exception.DependencyGraphException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
public class MyApplicationContextTest {

    MyApplicationContext context;

    @Test
    public void context_can_create_instances() {
        // Arrange
        context = new MyApplicationContext("pl.slichota.container.test_classes.normal");
        List<String> expected = Stream.of(A.class.getName(), B.class.getName(), C.class.getName()).sorted().toList();

        // Act
        List<String> result = context.getBeanDefinitionNames().stream().sorted().toList();

        // Assert
        assertArrayEquals(expected.toArray(), result.toArray());
    }

    @Test
    public void context_create_singleton_bean() {
        context = new MyApplicationContext("pl.slichota.container.test_classes.normal");
        var result1 = context.getBean(A.class);
        var result2 = context.getBean(A.class);
        assertEquals(result1, result2);
    }

    @Test
    public void context_throws_exception_when_bean_not_found() {
        context = new MyApplicationContext("pl.slichota.container.test_classes.normal");

    }

    @Test
    public void context_can_detect_cycle() {
        assertThrows(DependencyGraphException.class,
                () -> new MyApplicationContext("pl.slichota.container.test_classes.cycle"));
    }


}
