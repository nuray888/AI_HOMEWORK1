// AStar.java
import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

public class AStar {

    static class Edge {
        int to;
        double weight;
        Edge(int to, double w) { this.to = to; this.weight = w; }
    }

    static class Graph {
        Map<Integer,Integer> cellId = new HashMap<>(); // vertexId -> cell_id
        Map<Integer, List<Edge>> adj = new HashMap<>();
        Set<Integer> vertices = new HashSet<>();
        void addVertex(int id, int cell) {
            vertices.add(id);
            cellId.put(id, cell);
            adj.putIfAbsent(id, new ArrayList<>());
        }
        void addEdge(int u, int v, double w) {
            adj.putIfAbsent(u, new ArrayList<>());
            adj.putIfAbsent(v, new ArrayList<>());
            adj.get(u).add(new Edge(v,w));
            adj.get(v).add(new Edge(u,w));
            vertices.add(u); vertices.add(v);
        }
    }

    static class AResult {
        Double cost;
        List<Integer> path;
        long expanded;
        long pushes;
        int maxFrontier;
        double runtimeSec;
    }

    // decode cell id to x,y: x = cell_id // 10, y = cell_id % 10
    static int cellX(int cell) { return cell / 10; }
    static int cellY(int cell) { return cell % 10; }

    // heuristics
    static BiFunction<Graph,Integer,Double> zeroHeuristic(int goal) {
        return (g, v) -> 0.0;
    }
    static BiFunction<Graph,Integer,Double> euclideanHeuristic(int goal) {
        return (graph, v) -> {
            int cellV = graph.cellId.get(v);
            int cellG = graph.cellId.get(goal);
            double dx = cellX(cellV) - cellX(cellG);
            double dy = cellY(cellV) - cellY(cellG);
            return Math.hypot(dx, dy);
        };
    }
    static BiFunction<Graph,Integer,Double> manhattanHeuristic(int goal) {
        return (graph, v) -> {
            int cellV = graph.cellId.get(v);
            int cellG = graph.cellId.get(goal);
            double dx = Math.abs(cellX(cellV) - cellX(cellG));
            double dy = Math.abs(cellY(cellV) - cellY(cellG));
            return dx + dy;
        };
    }

    static class PQEntry {
        double f;
        double g;
        int node;
        PQEntry(double f, double g, int node) { this.f = f; this.g = g; this.node = node; }
    }

    // Run A* given graph, start, goal, heuristic (function that returns h for a node)
    static AResult astar(Graph graph, int start, int goal, BiFunction<Graph,Integer,Double> hfunc) {
        long pushes = 0;
        long expanded = 0;
        int maxFrontier = 0;
        final double INF = Double.POSITIVE_INFINITY;

        Map<Integer, Double> gCost = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        for (int v : graph.vertices) gCost.put(v, INF);

        Comparator<PQEntry> cmp = (a,b) -> {
            if (a.f < b.f) return -1;
            if (a.f > b.f) return 1;
            return Integer.compare(a.node, b.node); // deterministic tie-breaker
        };
        PriorityQueue<PQEntry> open = new PriorityQueue<>(cmp);

        gCost.put(start, 0.0);
        double startF = 0.0 + hfunc.apply(graph, start);
        open.add(new PQEntry(startF, 0.0, start));
        pushes++;

        double startTime = System.nanoTime();

        while (!open.isEmpty()) {
            maxFrontier = Math.max(maxFrontier, open.size());
            PQEntry cur = open.poll();
            int u = cur.node;
            double g_u = cur.g;

            // Only expand if this popped g matches best-known g
            if (Math.abs(g_u - gCost.getOrDefault(u, INF)) > 1e-9) {
                continue;
            }

            expanded++;

            if (u == goal) {
                double endTime = System.nanoTime();
                AResult res = new AResult();
                res.cost = gCost.get(goal);
                // reconstruct path
                LinkedList<Integer> path = new LinkedList<>();
                int x = goal;
                while (parent.containsKey(x)) {
                    path.addFirst(x);
                    x = parent.get(x);
                }
                path.addFirst(start);
                res.path = path;
                res.expanded = expanded;
                res.pushes = pushes;
                res.maxFrontier = maxFrontier;
                res.runtimeSec = (endTime - startTime) / 1e9;
                return res;
            }

            // expand neighbors
            List<Edge> neighbors = graph.adj.getOrDefault(u, Collections.emptyList());
            for (Edge e : neighbors) {
                int v = e.to;
                double w = e.weight;
                double tentative = g_u + w;
                if (tentative + 1e-12 < gCost.getOrDefault(v, INF)) {
                    gCost.put(v, tentative);
                    parent.put(v, u);
                    double f = tentative + hfunc.apply(graph, v);
                    open.add(new PQEntry(f, tentative, v));
                    pushes++;
                }
            }
        }

        double endTime = System.nanoTime();
        AResult res = new AResult();
        res.cost = null; // NO PATH
        res.path = null;
        res.expanded = expanded;
        res.pushes = pushes;
        res.maxFrontier = maxFrontier;
        res.runtimeSec = (endTime - startTime) / 1e9;
        return res;
    }

    // parse input file
    static class ParsedInput {
        Graph graph = new Graph();
        Integer S = null;
        Integer D = null;
    }

    static ParsedInput parseFile(String filename) throws IOException {
        ParsedInput out = new ParsedInput();
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split(",");
            for (int i=0;i<parts.length;i++) parts[i]=parts[i].trim();
            if (parts.length == 2 && parts[0].equalsIgnoreCase("S")) {
                out.S = Integer.parseInt(parts[1]);
            } else if (parts.length == 2 && parts[0].equalsIgnoreCase("D")) {
                out.D = Integer.parseInt(parts[1]);
            } else if (parts.length == 2) {
                int id = Integer.parseInt(parts[0]);
                int cell = Integer.parseInt(parts[1]);
                out.graph.addVertex(id, cell);
            } else if (parts.length == 3) {
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                double w = Double.parseDouble(parts[2]);
                out.graph.addEdge(a,b,w);
            } else {
                // ignore
            }
        }
        br.close();
        return out;
    }

    // check admissibility conditions for heuristics on edges
    static void checkHeuristicValidity(Graph g) {
        boolean euclidOk = true, manhOk = true;
        for (int u : g.vertices) {
            for (Edge e : g.adj.getOrDefault(u, Collections.emptyList())) {
                int v = e.to;
                if (u >= v) continue; // only check once per undirected
                int cellU = g.cellId.get(u);
                int cellV = g.cellId.get(v);
                double l2 = Math.hypot(cellX(cellU) - cellX(cellV), cellY(cellU) - cellY(cellV));
                double l1 = Math.abs(cellX(cellU) - cellX(cellV)) + Math.abs(cellY(cellU) - cellY(cellV));
                if (e.weight + 1e-12 < l2) euclidOk = false;
                if (e.weight + 1e-12 < l1) manhOk = false;
            }
        }
        System.out.println("Heuristic validity checks for this graph:");
        System.out.println("Euclidean admissible (w >= Euclidean for every edge)? " + (euclidOk ? "YES" : "NO"));
        System.out.println("Manhattan admissible (w >= Manhattan for every edge)? " + (manhOk ? "YES" : "NO"));
    }

    static void printModeResult(String mode, AResult r, int start, int goal) {
        System.out.println();
        System.out.println("MODE: " + mode);
        if (r.cost == null) {
            System.out.println("Optimal cost: NO PATH");
        } else {
            System.out.printf("Optimal cost: %.6f%n", r.cost);
        }
        if (r.path != null) {
            System.out.print("Path: ");
            for (int i=0;i<r.path.size();i++) {
                if (i>0) System.out.print(" -> ");
                System.out.print(r.path.get(i));
            }
            System.out.println();
        }
        System.out.println("Expanded: " + r.expanded);
        System.out.println("Pushes: " + r.pushes);
        System.out.println("Max frontier: " + r.maxFrontier);
        System.out.printf("Runtime (s): %.6f%n", r.runtimeSec);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java AStar <inputfile>");
            System.exit(1);
        }
        String filename = args[0];
        try {
            ParsedInput pi = parseFile(filename);
            if (pi.S == null || pi.D == null) {
                System.out.println("Missing S or D in input file.");
                return;
            }
            Graph g = pi.graph;
            System.out.printf("Parsed graph: %d vertices, adjacency lists for %d vertices.%n", g.vertices.size(), g.adj.size());
            checkHeuristicValidity(g);

            // UCS
            AResult rU = astar(g, pi.S, pi.D, zeroHeuristic(pi.D));
            printModeResult("UCS", rU, pi.S, pi.D);

            // A* Euclidean
            AResult rE = astar(g, pi.S, pi.D, euclideanHeuristic(pi.D));
            printModeResult("A* Euclidean", rE, pi.S, pi.D);

            // A* Manhattan
            AResult rM = astar(g, pi.S, pi.D, manhattanHeuristic(pi.D));
            printModeResult("A* Manhattan", rM, pi.S, pi.D);

            // Comparison quick summary
            System.out.println();
            System.out.println("Comparison:");
            System.out.println("Costs: ");
            System.out.printf(" UCS: %s, Euclid: %s, Manhattan: %s%n",
                    (rU.cost==null?"NO PATH":String.format("%.6f", rU.cost)),
                    (rE.cost==null?"NO PATH":String.format("%.6f", rE.cost)),
                    (rM.cost==null?"NO PATH":String.format("%.6f", rM.cost))
            );
            System.out.println("Expanded (UCS, Euclid, Manhattan): " + rU.expanded + ", " + rE.expanded + ", " + rM.expanded);

        } catch (IOException ex) {
            System.err.println("Error reading file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
