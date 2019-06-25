import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import rrcf.general.ShingledForest;
import rrcf.memory.SmallShingledForest;

public class TreeSizeBenchmark {
    public static void main(String[] args) throws IOException {
        for (int trees = 10; trees < 50; trees += 10) {
            for (int shingle = 10; shingle < 100; shingle += 10) {
                for (int size = 100; size < 1000; size += 100) {
                    SmallShingledForest small = new SmallShingledForest(new Random(1), shingle, trees, size);
                    ShingledForest normal = new ShingledForest(new Random(1), shingle, trees, size);
                    Random r = new Random();
                    for (int i = 0; i < size + 2 * shingle; i++) {
                        double d = r.nextDouble() * 1000;
                        small.addPoint(d);
                        normal.addPoint(d);
                    }
                    System.out.printf("Running with (%d, %d, %d)\n", shingle, trees, size);
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    ObjectOutputStream o = new ObjectOutputStream(b);
                    GZIPOutputStream zip = new GZIPOutputStream(b);
                    ObjectOutputStream zipO = new ObjectOutputStream(zip);
                    o.writeObject(small);
                    System.out.printf("Small: %d --> %f\n", b.size(), b.size() / (double)(trees * shingle * size));
                    b.reset();
                    zipO.writeObject(small);
                    System.out.printf("Small GZipped: %d --> %f\n", b.size(), b.size() / (double)(trees * shingle * size));
                    b.reset();
                    o.writeObject(normal);
                    System.out.printf("Normal: %d --> %f\n", b.size(), b.size() / (double)(trees * shingle * size));
                    b.reset();
                    zipO.writeObject(normal);
                    System.out.printf("Normal GZipped: %d --> %f\n", b.size(), b.size() / (double)(trees * shingle * size));
                    o.close();
                }
            }
        }
    }
}