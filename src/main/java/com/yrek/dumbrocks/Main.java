package com.yrek.dumbrocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import com.yrek.ifstd.blorb.Blorb;
import com.yrek.ifstd.glk.GlkDispatch;
import com.yrek.ifstd.glulx.Glulx;
import com.yrek.ifstd.zcode.ZCode;

public class Main {
    public static void main(String[] args) throws Exception {
        File storyFile = new File(args[0]);
        int width = 80;
        int height = 24;
        DumbGlk glk = new DumbGlk(new File(new File(System.getProperty("user.home", "/tmp"), ".dumbGlk"), storyFile.getName()), new InputStreamReader(System.in), new OutputStreamWriter(System.out), width, height);
        ZCode zcode = null;
        Glulx glulx = null;
        try {
            Blorb blorb = Blorb.from(storyFile);
            for (Blorb.Resource res : blorb.resources()) {
                if (res.getUsage() == Blorb.Exec && res.getChunk().getId() == Blorb.GLUL) {
                    glulx = new Glulx(res.getChunk().getContents(), glk);
                }
                if (res.getUsage() == Blorb.Exec && res.getChunk().getId() == Blorb.ZCOD) {
                    zcode = new ZCode(res.getChunk().getContents(), new GlkDispatch(glk)).initGlk(0,0);
                }
            }
        } catch (Exception e) {
        }
        if (zcode == null && glulx == null) {
            FileInputStream fin = new FileInputStream(storyFile);
            int magic;
            try {
                magic = fin.read();
            } finally {
                fin.close();
            }
            switch (magic) {
            case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8:
                zcode = new ZCode(storyFile, new GlkDispatch(glk)).initGlk(0,0);
                break;
            case 71:
                glulx = new Glulx(storyFile, glk);
                break;
            default:
                break;
            }
        }
        if (zcode != null) {
            zcode.run();
        } else if (glulx != null) {
            glulx.run();
        }
    }
}
