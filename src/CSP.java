// CSP.java
import java.io.*;
import java.util.*;

public class CSP {

    static class Graph {
        Map<Integer, Set<Integer>> neighbors = new HashMap<>();
        Set<Integer> vars = new HashSet<>();
        void addEdge(int u, int v) {
            neighbors.putIfAbsent(u, new HashSet<>());
            neighbors.putIfAbsent(v, new HashSet<>());
            neighbors.get(u).add(v);
            neighbors.get(v).add(u);
            vars.add(u); vars.add(v);
        }
    }

    static class Arc { int xi, xj; Arc(int xi, int xj) { this.xi = xi; this.xj = xj; } }

    Graph graph;
    int K;
    Map<Integer, Set<Integer>> domains;
    Deque<Pair> trail = new ArrayDeque<>(); // pruned (var, value) pairs for undo
    Map<Integer, Integer> assignment = new HashMap<>();

    static class Pair { int var; int val; Pair(int v,int val){this.var=v; this.val=val;} }

    CSP(Graph g, int K) {
        this.graph = g;
        this.K = K;
        domains = new HashMap<>();
        for (int v : graph.vars) {
            Set<Integer> dom = new LinkedHashSet<>();
            for (int i=1;i<=K;i++) dom.add(i);
            domains.put(v, dom);
        }
    }

    // AC-3 support
    boolean revise(int xi, int xj) {
        boolean revised = false;
        Set<Integer> dx = domains.get(xi);
        Set<Integer> dy = domains.get(xj);
        if (dx == null || dy == null) return false;
        List<Integer> toRemove = new ArrayList<>();
        for (int a : dx) {
            boolean supported = false;
            for (int b : dy) {
                if (a != b) { supported = true; break; }
            }
            if (!supported) toRemove.add(a);
        }
        for (int val : toRemove) {
            dx.remove(val);
            trail.push(new Pair(xi, val));
            revised = true;
        }
        return revised;
    }

    boolean ac3(Queue<Arc> queue) {
        while (!queue.isEmpty()) {
            Arc arc = queue.poll();
            if (revise(arc.xi, arc.xj)) {
                if (domains.get(arc.xi).isEmpty()) return false;
                for (int xk : graph.neighbors.getOrDefault(arc.xi, Collections.emptySet())) {
                    if (xk == arc.xj) continue;
                    queue.add(new Arc(xk, arc.xi));
                }
            }
        }
        return true;
    }

    // Initialize AC3 on whole graph
    boolean initialAC3() {
        Queue<Arc> q = new ArrayDeque<>();
        for (int xi : graph.vars) {
            for (int xj : graph.neighbors.getOrDefault(xi, Collections.emptySet())) {
                q.add(new Arc(xi, xj));
            }
        }
        return ac3(q);
    }

    // MRV: pick unassigned var with smallest domain size
    int selectMRV() {
        int bestVar = -1;
        int bestSize = Integer.MAX_VALUE;
        for (int v : graph.vars) {
            if (assignment.containsKey(v)) continue;
            int sz = domains.get(v).size();
            if (sz < bestSize) {
                bestSize = sz;
                bestVar = v;
            } else if (sz == bestSize && bestVar != -1) {
                // tie-breaker by var id to be deterministic
                if (v < bestVar) bestVar = v;
            }
        }
        return bestVar;
    }

    // LCV: order values by fewest eliminations (ascending)
    List<Integer> orderLCV(int var) {
        List<Integer> vals = new ArrayList<>(domains.get(var));
        Map<Integer, Integer> eliminated = new HashMap<>();
        for (int val : vals) {
            int elim = 0;
            for (int nb : graph.neighbors.getOrDefault(var, Collections.emptySet())) {
                if (assignment.containsKey(nb)) continue;
                if (domains.get(nb).contains(val)) elim++;
            }
            eliminated.put(val, elim);
        }
        vals.sort((a,b) -> {
            int c = Integer.compare(eliminated.get(a), eliminated.get(b));
            if (c != 0) return c;
            return Integer.compare(a,b);
        });
        return vals;
    }

    void assign(int var, int value, Map<Integer,Integer> assignedBefore, int trailSizeBefore) {
        assignment.put(var, value);
        // prune other values from var's domain
        Set<Integer> dom = domains.get(var);
        List<Integer> toRemove = new ArrayList<>();
        for (int v : dom) if (v != value) toRemove.add(v);
        for (int v : toRemove) {
            dom.remove(v);
            trail.push(new Pair(var, v));
        }
    }

    void undoToSize(int targetTrailSize) {
        while (trail.size() > targetTrailSize) {
            Pair p = trail.pop();
            domains.get(p.var).add(p.val);
        }
    }

    boolean consistentAssign(int var, int value) {
        for (int nb : graph.neighbors.getOrDefault(var, Collections.emptySet())) {
            if (assignment.containsKey(nb) && assignment.get(nb) == value) return false;
        }
        return true;
    }

    boolean backtrack() {
        if (assignment.size() == graph.vars.size()) return true;
        int var = selectMRV();
        if (var == -1) return false; // no variable available
        List<Integer> ordered = orderLCV(var);
        int trailSizeBefore = trail.size();
        for (int val : ordered) {
            if (!consistentAssign(var, val)) continue;
            // save domain states by pruning and running AC3
            Map<Integer,Integer> assignedBefore = new HashMap<>(assignment);
            assign(var, val, assignedBefore, trailSizeBefore);

            // prepare AC3 queue: arcs (neighbor, var)
            Queue<Arc> q = new ArrayDeque<>();
            for (int nb : graph.neighbors.getOrDefault(var, Collections.emptySet())) {
                q.add(new Arc(nb, var));
            }
            boolean ok = ac3(q);
            if (ok) {
                if (backtrack()) return true;
            }
            // undo
            assignment.remove(var);
            undoToSize(trailSizeBefore);
        }
        return false;
    }

    // Solve returns assignment map or null if failure
    Map<Integer,Integer> solve() {
        // check self-loops
        for (int v : graph.vars) {
            if (graph.neighbors.getOrDefault(v, Collections.emptySet()).contains(v)) {
                return null;
            }
        }
        // initial AC3
        if (!initialAC3()) return null;
        boolean ok = backtrack();
        if (ok) return assignment;
        return null;
    }

    // Parser & runner
    static void usage() {
        System.out.println("Usage: java CSP <inputfile>");
    }

    public static void main(String[] args) {
        if (args.length < 1) { usage(); return; }
        String filename = args[0];
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            Graph g = new Graph();
            int K = -1;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("colors=")) {
                    K = Integer.parseInt(line.substring("colors=".length()).trim());
                } else {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        int u = Integer.parseInt(parts[0].trim());
                        int v = Integer.parseInt(parts[1].trim());
                        g.addEdge(u, v);
                    }
                }
            }
            br.close();
            if (K < 1) {
                System.out.println("failure");
                return;
            }
            CSP solver = new CSP(g, K);
            Map<Integer,Integer> sol = solver.solve();
            if (sol == null) {
                System.out.println("failure");
            } else {
                // produce exactly one line as requested
                // sort keys for deterministic printing
                List<Integer> keys = new ArrayList<>(sol.keySet());
                Collections.sort(keys);
                StringBuilder sb = new StringBuilder();
                sb.append("SOLUTION: {");
                for (int i=0;i<keys.size();i++) {
                    int k = keys.get(i);
                    sb.append(k).append(": ").append(sol.get(k));
                    if (i < keys.size()-1) sb.append(", ");
                }
                sb.append("}");
                System.out.println(sb.toString());
            }
        } catch (IOException ex) {
            System.err.println("IO error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

