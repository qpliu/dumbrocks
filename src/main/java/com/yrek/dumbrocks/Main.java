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
        try {
            Blorb blorb = Blorb.from(storyFile);
            for (Blorb.Resource res : blorb.resources()) {
                if (res.getUsage() == Blorb.Exec && res.getChunk().getId() == Blorb.GLUL) {
                    new Glulx(res.getChunk().getContents(), glk).run();
                    return;
                }
                if (res.getUsage() == Blorb.Exec && res.getChunk().getId() == Blorb.ZCOD) {
                    new ZCode(res.getChunk().getContents(), new GlkDispatch(glk)).run();
                    return;
                }
            }
        } catch (Exception e) {
        }
        FileInputStream fin = new FileInputStream(storyFile);
        int magic;
        try {
            magic = fin.read();
        } finally {
            fin.close();
        }
        switch (magic) {
        case 1: case 2: case 3: case 4: case 5: case 6: case 7: case 8:
            new ZCode(storyFile, new GlkDispatch(glk)).run();
            return;
        case 71:
            new Glulx(storyFile, glk).run();
            return;
        default:
            break;
        }
    }
}
