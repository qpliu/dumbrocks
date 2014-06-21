package com.yrek.dumbrocks;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glulx.Glulx;

public class Main {
    public static void main(String[] args) throws Exception {
        File storyFile = new File(args[0]);
        int width = 80;
        int height = 24;
        DumbGlk glk = new DumbGlk(new File(new File(System.getProperty("user.home", "/tmp"), ".dumbGlk"), storyFile.getName()), new InputStreamReader(System.in), new OutputStreamWriter(System.out), width, height);
        byte[] storyBytes = null;
        try {
            Blorb blorb = Blorb.from(storyFile);
            for (Blorb.Resource res : blorb.resources()) {
                if (res.getUsage() == Blorb.Exec && res.getChunk().getId() == Blorb.GLUL) {
                    storyBytes = res.getChunk().getContents();
                    break;
                }
            }
        } catch (Exception e) {
        }
        if (storyBytes != null) {
            new Glulx(storyBytes, glk).run();
        } else {
            new Glulx(storyFile, glk).run();
        }
        printProfilingData();
    }

    private static void printProfilingData() {
        Map<String,long[]> data = Glulx.profilingData();
        if (data != null) {
            ArrayList<Map.Entry<String,long[]>> list = new ArrayList<Map.Entry<String,long[]>>(data.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String,long[]>>() {
                @Override public int compare(Map.Entry<String,long[]> e1, Map.Entry<String,long[]> e2) {
                    long diff = e2.getValue()[0] - e1.getValue()[0];
                    return diff == 0 ? 0 : diff > 0 ? 1 : -1;
                }
                @Override public boolean equals(Object obj) {
                    return this == obj;
                }
            });
            for (Map.Entry<String,long[]> entry : list) {
                long[] numbers = entry.getValue();
                System.out.println(String.format("%14s c=%8d t=%6dms avg=%.3fus", entry.getKey(), numbers[0], numbers[1], numbers[0] == 0 ? 0.0 : 1000.0 * (double) numbers[1]/(double) numbers[0]));
            }
        }
    }
}
