package pl.slichota.cycle;

import java.util.*;

public class DependencyGraph {
    private final Map<Class<?>, List<Class<?>>> adjList;

    public DependencyGraph() {
        adjList = new HashMap<>();
    }

    public void addVertex(Class<?> clazz) {
        adjList.putIfAbsent(clazz, new ArrayList<>());
    }

    public void addEdge(Class<?> source, Class<?> destination) {
        addVertex(source);
        addVertex(destination);
        adjList.get(source).add(destination);
    }


    public boolean edgeExists(Class<?> source, Class<?> destination) {
        List<Class<?>> neighbors = adjList.get(source);
        if (neighbors != null) {
            return neighbors.contains(destination);
        }
        return false;
    }

    public void showEdges() {
        for (Map.Entry<Class<?>, List<Class<?>>> entry : adjList.entrySet()) {
            Class<?> source = entry.getKey();
            for (Class<?> destination : entry.getValue()) {
                System.out.println(source.getName() + " -> " + destination.getName());
            }
        }
    }


    public boolean hasCycle() {
        Set<Class<?>> visited = new HashSet<>();
        Set<Class<?>> recStack = new HashSet<>();

        for (Class<?> vertex : adjList.keySet()) {
            if (hasCycleUtil(vertex, visited, recStack)) {
                return true;
            }
        }
        return false;
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
