package pl.slichota.container;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;
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
        initializeDependencies(dependencies);
    }


    public MyApplicationContext(Class<?> clazz) {
        this(clazz.getPackage().getName());
    }

    private void buildDependencyGraph(Class<?> c, DependencyGraph dependencyGraph){
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                if (!field.getType().isAnnotationPresent(Component.class)) {
                    throw new RuntimeException("It is not bean: " + field.getType());
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

    private void initializeDependencies(DependencyGraph dependencies) {
        if (dependencies.hasCycle()) {
            throw new RuntimeException("Graph has cycle");
        }
        for (Map.Entry<Class<?>, List<Class<?>>> entry : dependencies.getAdjList().entrySet()) {
            var component = entry.getKey();
            var componentDependencies = entry.getValue();
            for (int i = componentDependencies.size() - 1; i >= 0; i--) {
                Object dependencyInstance;
                // drugi warunek jest niepoprawny
                if (checkIfConstructorExists(componentDependencies.get(i), dependencies) && !dependencies.getAdjList().get(componentDependencies.get(i)).isEmpty()) {
                    dependencyInstance = createInstanceAllArgsConstructor(componentDependencies.get(i), dependencies);
                } else {
                    dependencyInstance = createInstanceNoArgsConstructor(componentDependencies.get(i));
                }
                beans.put(componentDependencies.get(i), dependencyInstance);
            }
            Object componentInstance;
            if (checkIfConstructorExists(component, dependencies) && !componentDependencies.isEmpty()) {
                componentInstance = createInstanceAllArgsConstructor(component, dependencies);
            } else {
                componentInstance = createInstanceNoArgsConstructor(component);
            }

            beans.put(component, componentInstance);
        }
    }

    private boolean checkIfConstructorExists(Class<?> clazz, DependencyGraph dependencyGraph) {
        var classDependencies = dependencyGraph
                .getAdjList()
                .get(clazz);
        try {
            clazz.getConstructor(classDependencies.toArray(new Class[0]));
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private Object createInstanceAllArgsConstructor(Class<?> clazz, DependencyGraph dependencies) {
        if (beans.containsKey(clazz)) {
            return beans.get(clazz);
        }
        var adjs = dependencies.getAdjList();
        var classDependencies = adjs.get(clazz);

        var objects = new ArrayList<>();
        for (Class<?> clazzDependency : classDependencies) {
            objects.add(beans.get(clazzDependency));
        }
        try {
            Constructor<?> allArgsConstructor = clazz.getConstructor(classDependencies.toArray(new Class[0]));
            Object instance = allArgsConstructor.newInstance(objects.toArray());
            return instance;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Object createInstanceNoArgsConstructor(Class<?> clazz) {
        if (beans.containsKey(clazz)) {
            return beans.get(clazz);
        }
        Constructor<?> noParamsConstructor = clazz.getConstructors()[0];
        Object instance = null;
        try {
            instance = noParamsConstructor.newInstance();
            Field[] fields = instance.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    if (!field.canAccess(instance)) {
                        field.setAccessible(true);
                        var bean = beans.get(field.getType());
                        field.set(instance, bean);
                        field.setAccessible(false);
                    } else {
                        var bean = beans.get(field.getType());
                        field.set(instance, bean);
                    }
                }
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return null;
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