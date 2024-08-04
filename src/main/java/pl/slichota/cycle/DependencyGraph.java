package pl.slichota.cycle;

import pl.slichota.annotations.Autowired;
import pl.slichota.annotations.Component;
import pl.slichota.cycle.exception.DependencyGraphException;
import pl.slichota.cycle.exception.DependencyGraphExceptionMessage;

import java.lang.reflect.Field;
import java.util.*;

public class DependencyGraph {
    private final Map<Class<?>, List<Class<?>>> adjList;

    public DependencyGraph() {
        adjList = new HashMap<>();
    }

    public void addVertex(Class<?> clazz) {
        if (clazz != null) {
            adjList.putIfAbsent(clazz, new ArrayList<>());
        }
    }

    public static void buildDependencyGraph(Class<?> c, DependencyGraph dependencyGraph){
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                if (!field.getType().isAnnotationPresent(Component.class)) {
                    throw new DependencyGraphException(DependencyGraphExceptionMessage.IT_IS_NOT_BEAN.getMessage(), field.getType().getName());
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

    public void addEdge(Class<?> source, Class<?> destination) {
        addVertex(source);
        addVertex(destination);
        if (destination != null) {
            adjList.get(source).add(destination);
        }
    }

    public boolean edgeExists(Class<?> source, Class<?> destination) {
        List<Class<?>> neighbors = adjList.get(source);
        if (neighbors != null) {
            return neighbors.contains(destination);
        }
        return false;
    }

    public String showEdges() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Class<?>, List<Class<?>>> entry : adjList.entrySet()) {
            Class<?> source = entry.getKey();
            for (Class<?> destination : entry.getValue()) {
                sb.append(source.getName()).append(" -> ").append(destination.getName()).append("\n");
            }
        }
        return sb.toString();
    }

    public void hasCycle() {
        Set<Class<?>> visited = new HashSet<>();
        Set<Class<?>> recStack = new HashSet<>();

        for (Class<?> vertex : adjList.keySet()) {
            if (hasCycleUtil(vertex, visited, recStack)) {
                throw new DependencyGraphException(DependencyGraphExceptionMessage.CYCLE_DETECTED.getMessage(), adjList.get(vertex).toString());
            }
        }
    }

    private boolean hasCycleUtil(Class<?> current, Set<Class<?>> visited, Set<Class<?>> recStack) {
        if (recStack.contains(current)) {
            return true;
        }
        if (visited.contains(current)) {
            return false;
        }

        visited.add(current);
        recStack.add(current);

        List<Class<?>> children = adjList.get(current);

        for (Class<?> child : children) {
            if (hasCycleUtil(child, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(current);
        return false;
    }

    public Map<Class<?>, List<Class<?>>> getAdjList() {
        return adjList;
    }
}
