package pl.slichota.container;

import java.util.List;

public interface ApplicationContext {
    <T> T getBean(Class<T> clazz);
    List<String> getBeanDefinitionNames();
}
