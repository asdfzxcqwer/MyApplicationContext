package pl.slichota.container;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;
import pl.slichota.container.exception.ApplicationContextException;
import pl.slichota.container.exception.ApplicationContextExceptionMessage;
import pl.slichota.cycle.DependencyGraph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class MyApplicationContext implements ApplicationContext{

    private final Map<Class<?>, Object> beans = new HashMap<>();

    public MyApplicationContext(String packageName) {
        var dependencies = new DependencyGraph();
        var classes = getClasses(packageName);
        var components = getComponents(classes);

        for (Class<?> c : components) {
            buildDependencyGraph(c, dependencies);
        }
        if (dependencies.hasCycle()) {
            throw new ApplicationContextException(ApplicationContextExceptionMessage.CYCLE_DETECTED.getMessage(), dependencies.showEdges());
        }

        for (Class<?> c : components) {
            createInstance(c);
        }
    }


    public MyApplicationContext(Class<?> clazz) {
        this(clazz.getPackage().getName());
    }

    private void createInstance(Class<?> clazz) {
        if (clazz == null) return;

        Constructor<?>[] constructors = clazz.getConstructors();
        int annotatedConstructorsLength = getAnnotatedConstructorsLength(constructors);

        if (annotatedConstructorsLength > 1) {
            throw new ApplicationContextException(ApplicationContextExceptionMessage.MULTIPLE_CONSTRUCTORS.getMessage(), Arrays.toString(constructors));
        }
        else if (getAnnotatedConstructorsLength(constructors) == 1) {
            createInstanceWithAnnotatedConstructor(clazz, constructors);
        } else {
            createInstanceWithNoArgsConstructor(clazz);
        }
    }

    private void createInstanceWithAnnotatedConstructor(Class<?> clazz, Constructor<?>[] constructors) {
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                Class<?>[] constructorParameters = constructor.getParameterTypes();
                Object[] parametersInstances = createParametersInstances(constructorParameters);

                try {
                    Object instance = constructor.newInstance(parametersInstances);
                    injectFields(clazz, instance);
                    if (!beans.containsKey(clazz)) {
                        beans.put(clazz, instance);
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    throw new ApplicationContextException(ApplicationContextExceptionMessage.CANNOT_CREATE_INSTANCE.getMessage(), String.valueOf(e));
                }
            }
        }
    }

    private Object[] createParametersInstances(Class<?>[] constructorParameters) {
        Object[] parametersInstances = new Object[constructorParameters.length];
        for (int i=0; i<constructorParameters.length; i++) {
            if (!constructorParameters[i].isAnnotationPresent(Component.class)) {
                throw new ApplicationContextException(ApplicationContextExceptionMessage.BEAN_NOT_FOUND.getMessage(), String.valueOf(constructorParameters[i]));
            }
            if (!beans.containsKey(constructorParameters[i])) {
                createInstance(constructorParameters[i]);
            }
            parametersInstances[i] = beans.get(constructorParameters[i]);
        }
        return parametersInstances;
    }

    private void createInstanceWithNoArgsConstructor(Class<?> clazz) {
        Constructor<?> noParamsConstructor = clazz.getConstructors()[0];
        Object instance;
        try {
            instance = noParamsConstructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ApplicationContextException(ApplicationContextExceptionMessage.CANNOT_CREATE_INSTANCE.getMessage(), String.valueOf(e));
        }

        injectFields(clazz, instance);
        if (!beans.containsKey(clazz)) {
            beans.put(clazz, instance);
        }

    }

    private void injectFields(Class<?> clazz, Object instance) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                if (!field.getType().isAnnotationPresent(Component.class)) {
                    throw new ApplicationContextException(ApplicationContextExceptionMessage.BEAN_NOT_FOUND.getMessage(), String.valueOf(field.getType()));
                }
                if (!beans.containsKey(field.getType())) {
                    createInstance(field.getType());
                }
                Object dependencyInstance = beans.get(field.getType());
                if (!field.canAccess(instance)) {
                    field.setAccessible(true);
                    try {
                        field.set(instance, dependencyInstance);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    field.setAccessible(false);
                } else {
                    try {
                        field.set(instance, dependencyInstance);
                    } catch (IllegalAccessException e) {
                        throw new ApplicationContextException(ApplicationContextExceptionMessage.CANNOT_CREATE_INSTANCE.getMessage(), String.valueOf(e));
                    }
                }
            }
        }
    }


    private int getAnnotatedConstructorsLength(Constructor<?>[] constructors) {
        int length = 0;
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                length++;
            }
        }
        return length;
    }

    private void buildDependencyGraph(Class<?> c, DependencyGraph dependencyGraph){
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                if (!field.getType().isAnnotationPresent(Component.class)) {
                    throw new ApplicationContextException(ApplicationContextExceptionMessage.BEAN_NOT_FOUND.getMessage(), String.valueOf(field.getType()));
                }
                if (dependencyGraph.edgeExists(c, field.getType())) {
                    return;
                } else {
                    dependencyGraph.addEdge(c, field.getType());
                    buildDependencyGraph(field.getType(), dependencyGraph);
                }
            }
        }
    }

    public <T> T getBean(Class<T> clazz) {
        if (beans.containsKey(clazz)) {
            return (T) beans.get(clazz);
        }
        return null;
    }

    @Override
    public List<String> getBeanDefinitionNames() {
        return beans.keySet()
                .stream()
                .map(Class::getName)
                .toList();
    }

    private List<Class<?>> getComponents(List<Class<?>> classes) {
        var components = new ArrayList<Class<?>>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                components.add(clazz);
            }
        }
        return components;
    }

    private List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                try {
                    var clazz = Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                    if (clazz.isAnnotationPresent(Component.class)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return classes;
    }

    private List<Class<?>> getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }

        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }

        return classes;
    }

}