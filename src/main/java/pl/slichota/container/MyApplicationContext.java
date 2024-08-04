package pl.slichota.container;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;
import pl.slichota.container.exception.ApplicationContextException;
import pl.slichota.container.exception.ApplicationContextMessage;
import pl.slichota.cycle.DependencyGraph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static pl.slichota.cycle.DependencyGraph.buildDependencyGraph;

public class MyApplicationContext implements ApplicationContext{

    private final Map<Class<?>, Object> beans = new HashMap<>();

    public MyApplicationContext(String packageName) {
        DependencyGraph dependencies = new DependencyGraph();
        List<Class<?>> classes = getClasses(packageName);
        List<Class<?>> components = getComponents(classes);

        components.forEach(c -> DependencyGraph.buildDependencyGraph(c, dependencies));
        initializeDependencies(dependencies);
    }

    public MyApplicationContext(Class<?> clazz) {
        this(clazz.getPackage().getName());
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        return (T) beans.get(clazz);
    }

    @Override
    public List<String> getBeanDefinitionNames() {
        return beans.keySet().stream()
                .map(Class::getName)
                .toList();
    }


    private void initializeDependencies(DependencyGraph dependencies) {
        dependencies.hasCycle();

        for (Map.Entry<Class<?>, List<Class<?>>> entry : dependencies.getAdjList().entrySet()) {
            var component = entry.getKey();
            var componentDependencies = entry.getValue();

            for (int i = componentDependencies.size() - 1; i >= 0; i--) {
                Object dependencyInstance = createInstance(componentDependencies.get(i), dependencies);
                beans.put(componentDependencies.get(i), dependencyInstance);
            }

            Object componentInstance = createInstance(component, dependencies);
            beans.put(component, componentInstance);
        }
    }

    private Object createInstance(Class<?> clazz, DependencyGraph dependencies) {
        if (beans.containsKey(clazz)) {
            return beans.get(clazz);
        }
        if (checkIfConstructorExists(clazz, dependencies)) {
            return createInstanceAllArgsConstructor(clazz, dependencies);
        } else {
            return createInstanceNoArgsConstructor(clazz);
        }
    }

    private boolean checkIfConstructorExists(Class<?> clazz, DependencyGraph dependencyGraph) {
        List<Class<?>> classDependencies = dependencyGraph.getAdjList().getOrDefault(clazz, Collections.emptyList());
        try {
            clazz.getConstructor(classDependencies.toArray(new Class[0]));
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private Object createInstanceAllArgsConstructor(Class<?> clazz, DependencyGraph dependencies) {
        Map<Class<?>, List<Class<?>>> adjList = dependencies.getAdjList();
        List<Class<?>> classDependencies = adjList.getOrDefault(clazz, Collections.emptyList());

        List<Object> dependencyInstances = classDependencies.stream()
                .map(beans::get)
                .toList();
        try {
            Constructor<?> allArgsConstuctor = clazz.getConstructor(classDependencies.toArray(new Class[0]));
            return allArgsConstuctor.newInstance(dependencyInstances.toArray());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private Object createInstanceNoArgsConstructor(Class<?> clazz) {
        try {
            Constructor<?> noParamConstructor = clazz.getDeclaredConstructor();
            Object instance = noParamConstructor.newInstance();
            injectAutowiredFields(instance);
            return instance;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to create instance of " + clazz.getName(), e);
        }
    }

    private void injectAutowiredFields(Object instance) throws IllegalAccessException {
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                boolean accessible = field.canAccess(instance);
                if (!accessible) {
                    field.setAccessible(true);
                }
                Object bean = beans.get(field.getType());
                field.set(instance, bean);
                if (!accessible) {
                    field.setAccessible(false);
                }
            }
        }
    }

    private List<Class<?>> getComponents(List<Class<?>> classes) {
        return classes.stream()
                .filter(clazz -> clazz.isAnnotationPresent(Component.class))
                .toList();
    }

    private List<Class<?>> findClasses(File directory, String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (!file.getName().contains(".")) {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                }
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Component.class)) {
                        classes.add(clazz);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Class not found: " + className, e);
                }
            }
        }
        return classes;
    }

    private List<Class<?>> getClasses(String packageName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            throw new IllegalStateException("Class loader is not available.");
        }
        String path = packageName.replace('.', '/');
        List<URL> resources;
        try {
            resources = Collections.list(classLoader.getResources(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to get resources for " + path, e);
        }
        List<File> dirs = resources.stream()
                .map(resource -> new File(resource.getFile()))
                .toList();

        List<Class<?>> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes;
    }
}