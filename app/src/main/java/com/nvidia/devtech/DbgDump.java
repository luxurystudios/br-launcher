package com.nvidia.devtech;
import java.io.*;
import java.util.*;
public class DbgDump {
    private static boolean started = false;
    public static synchronized void start() {
        if (started) return;
        started = true;
        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                try { Thread.sleep(i == 0 ? 40000 : 15000); } catch (Exception e) {}
                dump(i);
            }
        }).start();
    }
    static String rd(File f) {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) { String l = r.readLine(); return l == null ? "" : l; } catch (Exception e) { return ""; }
    }
    static void dump(int n) {
        try (FileWriter w = new FileWriter("/storage/emulated/0/LUXRUSSIA/SAMP/threads_dbg.txt", n > 0)) {
            w.write("=== dump " + n + " t=" + System.currentTimeMillis() + "\n");
            File[] ts = new File("/proc/self/task").listFiles();
            if (ts != null) for (File t : ts) {
                String stat = rd(new File(t, "stat"));
                int k = stat.lastIndexOf(')');
                if (k < 0) continue;
                String[] p = stat.substring(k + 2).split(" ");
                w.write(t.getName() + " " + rd(new File(t, "comm")) + " st=" + p[0] + " ut=" + p[11] + " stm=" + p[12] + " wchan=" + rd(new File(t, "wchan")) + "\n");
            }
            for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                w.write("JAVA " + e.getKey().getName() + ":");
                StackTraceElement[] s = e.getValue();
                for (int j = 0; j < Math.min(5, s.length); j++) w.write(" " + s[j]);
                w.write("\n");
            }
        } catch (Exception e) {}
    }
}
