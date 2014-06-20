package com.yrek.dumbrocks;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glulx.Glulx;

public class Main {
    public static void main(String[] args) throws Exception {
        String storyFile = args[0];
        int width = 80;
        int height = 24;
        DumbGlk glk = new DumbGlk(new InputStreamReader(System.in), new OutputStreamWriter(System.out), width, height);
        byte[] storyBytes = null;
        try {
            Blorb blorb = Blorb.from(new File(storyFile));
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
            new Glulx(new File(storyFile), glk).run();
        }
    }
}
