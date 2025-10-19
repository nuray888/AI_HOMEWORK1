## 🧠 Homework 1 — A* (Three Modes) + CSP (Graph Coloring)

**Name:** Nuray Muxtarli  
**Course:** Artificial Intelligence  
**Language:** Java  
**IDE:** IntelliJ IDEA / Command Line  

---

### 📁 Project Structure
```
AStarCSP/
│
├── src/
│   ├── AStar.java
│   ├── Graph.java
│   ├── Node.java
│   ├── CSP.java
│   ├── Variable.java
│   ├── Constraint.java
│   ├── Main.java
│
├── inputs/
│   ├── astar_small.txt
│   ├── astar_medium.txt
│   ├── csp_easy.txt
│   ├── csp_tight.txt
│
└── README.md
```

---

### 🚀 How to Run the Code

#### **1️⃣ Compile all Java files**
```bash
cd src
javac *.java
```

#### **2️⃣ Run A* Search (Three Modes)**

**Uniform Cost Search (UCS):**

 java AStar astar_small.txt
 
> 📄 You can replace `astar_small.txt` with `astar_medium.txt` to test larger graphs.

---

#### **3️⃣ Run CSP (Graph Coloring)**

java CSP csp_small.txt
> 📄 You can replace `csp_small.txt` with `astar_medium.txt` to test larger graphs.

---

### 🧩 Implementation Details

#### **A\* Search**
- **Input format:**
  ```
  # Vertices: id,cell_id
  1,11
  2,12
  ...
  # Edges: u,v,w
  1,2,2
  2,3,1
  ...
  # Source and Destination
  S,1
  D,10
  ```

- **Heuristics implemented:**
  - `UCS`: h(n) = 0  
  - `A* Euclidean`: h(n) = sqrt((x2-x1)^2 + (y2-y1)^2)  
  - `A* Manhattan`: h(n) = |x2-x1| + |y2-y1|

- **Output includes:**
  - Path found  
  - Total cost  
  - Number of expanded nodes  

---

#### **CSP (Graph Coloring)**
- **Input format:** adjacency list + number of colors (k)
- **Algorithm used:** Backtracking + MRV (Minimum Remaining Values)
- **Output includes:**
  - Color assigned to each node
  - Whether the graph is k-colorable
  - Number of backtracks

---

### 📊 Test Files

| File | Description |
|------|--------------|
| `astar_small.txt` | Simple test (≤10 nodes) |
| `astar_medium.txt` | Medium test (≥30 nodes, used for performance comparison) |
| `csp_easy.txt` | Graph with loose constraints |
| `csp_tight.txt` | Graph that includes a Kₖ subgraph (requires all k colors) |

---

### 💬 Analysis

**A\* Comparison:**
- UCS explores more nodes because it doesn’t use a heuristic.
- A* with Euclidean is efficient on continuous or grid-like maps.
- A* with Manhattan is better when movement is restricted to 4 directions.

**CSP Results:**
- Tight graphs (like `csp_tight.txt`) require more backtracking.
- MRV heuristic greatly improves performance on constrained instances.

---

### 📚 References
- [AIMA Java Repository](https://github.com/aimacode/aima-java)
- *Artificial Intelligence: A Modern Approach* (Russell & Norvig, 3rd Edition)
