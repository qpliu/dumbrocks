package com.yrek.dumbrocks;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

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
    }
}
