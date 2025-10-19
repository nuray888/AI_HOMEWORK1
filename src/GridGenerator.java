// GridGenerator.java
import java.io.*;
public class GridGenerator {
    public static void main(String[] args) throws Exception {
        int rows = 5; // x: 0..4
        int cols = 6; // y: 0..5 -> total 30 nodes
        PrintWriter pw = new PrintWriter(new FileWriter("astar_medium.txt"));
        pw.println("# generated grid graph");
        for (int x=0;x<rows;x++){
            for (int y=0;y<cols;y++){
                int id = x*cols + y + 1; // make vertex ids 1..30
                int cell = x*10 + y;
                pw.printf("%d,%d%n", id, cell);
            }
        }
        // edges (4-neighborhood with weight 1)
        for (int x=0;x<rows;x++){
            for (int y=0;y<cols;y++){
                int id = x*cols + y + 1;
                if (y+1<cols) {
                    int id2 = x*cols + (y+1) + 1;
                    pw.printf("%d,%d,1%n", id, id2);
                }
                if (x+1<rows) {
                    int id2 = (x+1)*cols + y + 1;
                    pw.printf("%d,%d,1%n", id, id2);
                }
            }
        }
        // source top-left id=1, dest bottom-right id = rows*cols
        pw.println("S,1");
        pw.println("D," + (rows*cols));
        pw.close();
        System.out.println("astar_medium.txt generated (rows=" + rows + ", cols=" + cols + ")");
    }
}

