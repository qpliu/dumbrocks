package com.yrek.dumbrocks;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.yrek.ifstd.glk.Glk;
import com.yrek.ifstd.glk.GlkByteArray;
import com.yrek.ifstd.glk.GlkEvent;
import com.yrek.ifstd.glk.GlkFile;
import com.yrek.ifstd.glk.GlkGestalt;
import com.yrek.ifstd.glk.GlkIntArray;
import com.yrek.ifstd.glk.GlkStream;
import com.yrek.ifstd.glk.GlkStreamMemory;
import com.yrek.ifstd.glk.GlkStreamMemoryUnicode;
import com.yrek.ifstd.glk.GlkStreamResult;
import com.yrek.ifstd.glk.GlkWindow;
import com.yrek.ifstd.glk.GlkWindowArrangement;
import com.yrek.ifstd.glk.GlkWindowSize;
import com.yrek.ifstd.glk.GlkWindowStream;
import com.yrek.ifstd.glk.UnicodeString;
import com.yrek.ifstd.glulx.Glulx;

public class DumbGlk implements Glk {
    private final File rootDir;
    private final Reader in;
    private final Writer out;
    private final int width;
    private final int height;

    private GlkStream currentStream = null;
    private GlkWindowStream rootStream = null;
    private GlkWindow rootWindow = null;
    private int outputCount = 0;
    private boolean charEventRequested = false;
    private GlkByteArray lineEventBuffer = null;
    private int lineEventInitLength = 0;

    public DumbGlk(File rootDir, Reader in, Writer out, int width, int height) {
        this.rootDir = rootDir;
        this.in = in;
        this.out = out;
        this.width = width;
        this.height = height;
    }

    private class Exit extends RuntimeException {
        private static final long serialVersionUID = 0L;
    }

    @Override
    public void main(Runnable main) throws IOException {
        try {
            main.run();
        } catch (Exit e) {
        }
    }

    @Override
    public void exit() {
        throw new Exit();
    }

    @Override
    public void setInterruptHandler(Runnable handler) {
    }

    @Override
    public void tick() {
    }

    @Override
    public int gestalt(int selector, int value) {
        switch (selector) {
        case GlkGestalt.Version:
            return Glk.GlkVersion;
        case GlkGestalt.CharInput:
        case GlkGestalt.LineInput:
        case GlkGestalt.CharOutput:
            return 1;
        case GlkGestalt.MouseInput:
        case GlkGestalt.Timer:
        case GlkGestalt.Graphics:
        case GlkGestalt.DrawImage:
        case GlkGestalt.Sound:
        case GlkGestalt.SoundVolume:
        case GlkGestalt.SoundNotify:
        case GlkGestalt.Hyperlinks:
        case GlkGestalt.HyperlinkInput:
        case GlkGestalt.SoundMusic:
        case GlkGestalt.GraphicsTransparency:
            return 0;
        case GlkGestalt.Unicode:
            return 1;
        case GlkGestalt.UnicodeNorm:
        case GlkGestalt.LineInputEcho:
        case GlkGestalt.LineTerminators:
        case GlkGestalt.LineTerminatorKey:
        case GlkGestalt.DateTime:
        case GlkGestalt.Sound2:
        case GlkGestalt.ResourceStream:
        default:
            return 0;
        }
    }

    @Override
    public int gestaltExt(int selector, int value, GlkIntArray array) {
        switch (selector) {
        case GlkGestalt.CharOutput:
            if (value < 256 && (value == 10 || !Character.isISOControl(value))) {
                array.setIntElement(1);
                return GlkGestalt.CharOutput_ExactPrint;
            } else {
                array.setIntElement(0);
                return GlkGestalt.CharOutput_CannotPrint;
            }
        default:
            return 0;
        }
    }


    @Override
    public GlkWindow windowGetRoot() {
        return rootWindow;
    }

    @Override
    public GlkWindow windowOpen(GlkWindow split, int method, int size, int winType, int rock) {
        if (rootWindow != null || split != null || winType != GlkWindow.TypeTextBuffer) {
            return null;
        }
        outputCount = 0;
        rootWindow = new GlkWindow(rock) {
            @Override
            public GlkWindowStream getStream() {
                return rootStream;
            }

            @Override
            public GlkStreamResult close() throws IOException {
                if (currentStream == rootStream) {
                    currentStream = null;
                }
                destroy();
                rootStream.destroy();
                rootWindow = null;
                rootStream = null;
                return new GlkStreamResult(0, outputCount);
            }

            @Override
            public GlkWindowSize getSize() {
                return new GlkWindowSize(width, height);
            }

            @Override
            public void setArrangement(int method, int size, GlkWindow key) {
                throw new RuntimeException("unimplemented");
            }


            @Override
            public GlkWindowArrangement getArrangement() {
                throw new RuntimeException("unimplemented");
            }

            @Override
            public int getType() {
                return GlkWindow.TypeTextBuffer;
            }

            @Override
            public GlkWindow getParent() {
                return null;
            }

            @Override
            public GlkWindow getSibling() {
                return null;
            }

            @Override
            public void clear() throws IOException {
                for (int i = 0; i < height; i++) {
                    out.append('\n');
                }
            }

            @Override
            public void moveCursor(int x, int y) throws IOException {
            }

            @Override
            public int getCursorX() {
                return 0;
            }

            @Override
            public int getCursorY() {
                return 0;
            }

            @Override
            public boolean styleDistinguish(int style1, int style2) {
                return false;
            }

            @Override
            public Integer styleMeasure(int style, int hint) {
                return null;
            }

            @Override
            public void requestLineEvent(GlkByteArray buffer, int initLength) {
                if (lineEventBuffer != null || charEventRequested) {
                    throw new IllegalStateException();
                }
                lineEventBuffer = buffer;
                lineEventInitLength = initLength;
            }

            @Override
            public void requestCharEvent() {
                if (lineEventBuffer != null || charEventRequested) {
                    throw new IllegalStateException();
                }
                charEventRequested = true;
            }

            @Override
            public void requestMouseEvent() {
            }

            @Override
            public GlkEvent cancelLineEvent() {
                lineEventBuffer = null;
                return new GlkEvent(GlkEvent.TypeLineInput, this, 0, 0);
            }

            @Override
            public void cancelCharEvent() {
                charEventRequested = false;
            }

            @Override
            public void cancelMouseEvent() {
            }

            @Override
            public boolean drawImage(int resourceId, int val1, int val2) throws IOException {
                return false;
            }

            @Override
            public boolean drawScaledImage(int resourceId, int val1, int val2, int width, int height) throws IOException {
                return false;
            }

            @Override
            public void flowBreak() {
            }

            @Override
            public void eraseRect(int left, int top, int width, int height) {
            }

            @Override
            public void fillRect(int color, int left, int top, int width, int height) {
            }

            @Override
            public void setBackgroundColor(int color) {
            }

            @Override
            public void setEchoLineEvent(boolean echoLineEvent) {
            }
        };
        rootStream = new GlkWindowStream(rootWindow) {
            @Override
            public void putChar(int ch) throws IOException {
                super.putChar(ch);
                outputCount++;
                out.write((char) (ch & 255));
            }

            @Override
            public void putString(CharSequence string) throws IOException {
                super.putString(string);
                outputCount += string.length();
                out.append(string);
            }

            @Override
            public void putBuffer(GlkByteArray buffer) throws IOException {
                super.putBuffer(buffer);
                int length = buffer.getArrayLength();
                outputCount += length;
                for (int i = 0; i < length; i++) {
                    out.write((char) (buffer.getByteElementAt(i) & 255));
                }
            }

            @Override
            public void putCharUni(int ch) throws IOException {
                super.putCharUni(ch);
                outputCount++;
                out.write(Character.toChars(ch));
            }

            @Override
            public void putStringUni(UnicodeString string) throws IOException {
                super.putStringUni(string);
                outputCount += string.codePointCount();
                out.append(string);
            }

            @Override
            public void putBufferUni(GlkIntArray buffer) throws IOException {
                super.putBufferUni(buffer);
                int length = buffer.getArrayLength();
                outputCount += length;
                for (int i = 0; i < length; i++) {
                    out.write(Character.toChars(buffer.getIntElementAt(i)));
                }
            }

            @Override
            public void setStyle(int style) {
                super.setStyle(style);
            }
        };
        return rootWindow;
    }

    @Override
    public void setWindow(GlkWindow window) {
        currentStream = window == null ? null : window.getStream();
    }

    @Override
    public GlkStream streamOpenFile(GlkFile file, int mode, int rock) throws IOException {
        if (mode == GlkFile.ModeRead && !file.exists()) {
            return null;
        }
        return new DumbStreamFile(file, mode, false, rock);
    }

    @Override
    public GlkStream streamOpenFileUni(GlkFile file, int mode, int rock) throws IOException {
        if (mode == GlkFile.ModeRead && !file.exists()) {
            return null;
        }
        return new DumbStreamFile(file, mode, true, rock);
    }

    @Override
    public GlkStream streamOpenMemory(GlkByteArray memory, int mode, int rock) {
        return new GlkStreamMemory(memory, rock);
    }

    @Override
    public GlkStream streamOpenMemoryUni(GlkIntArray memory, int mode, int rock) {
        return new GlkStreamMemoryUnicode(memory, rock);
    }

    @Override
    public void streamSetCurrent(GlkStream stream) {
        currentStream = stream;
    }

    @Override
    public GlkStream streamGetCurrent() {
        return currentStream;
    }

    @Override
    public void putChar(int ch) throws IOException {
        currentStream.putChar(ch);
    }

    @Override
    public void putString(CharSequence string) throws IOException {
        currentStream.putString(string);
    }

    @Override
    public void putBuffer(GlkByteArray buffer) throws IOException {
        currentStream.putBuffer(buffer);
    }

    @Override
    public void putCharUni(int ch) throws IOException {
        currentStream.putCharUni(ch);
    }

    @Override
    public void putStringUni(UnicodeString string) throws IOException {
        currentStream.putStringUni(string);
    }

    @Override
    public void putBufferUni(GlkIntArray buffer) throws IOException {
        currentStream.putBufferUni(buffer);
    }

    @Override
    public void setStyle(int style) {
        currentStream.setStyle(style);
    }

    @Override
    public void styleHintSet(int winType, int style, int hint, int value) {
    }

    @Override
    public void styleHintClear(int winType, int style, int hint) {
    }


    @Override
    public GlkFile fileCreateTemp(int usage, int rock) throws IOException {
        return new DumbFile(File.createTempFile("dg.",".tmp"), usage, GlkFile.ModeReadWrite, rock);
    }

    @Override
    public GlkFile fileCreateByName(int usage, CharSequence name, int rock) throws IOException {
        return new DumbFile(new File(rootDir, URLEncoder.encode("dg."+name, "UTF-8")), usage, GlkFile.ModeReadWrite, rock);
    }

    @Override
    public GlkFile fileCreateByPrompt(int usage, int mode, int rock) throws IOException {
        switch (usage & GlkFile.UsageTypeMask) {
        case GlkFile.UsageData:
            out.append("Enter filename for data file: ");
            break;
        case GlkFile.UsageSavedGame:
            out.append("Enter filename for save file: ");
            break;
        case GlkFile.UsageTranscript:
            out.append("Enter filename for transcript: ");
            break;
        case GlkFile.UsageInputRecord:
            out.append("Enter filename for input record: ");
            break;
        default:
            throw new IllegalArgumentException();
        }
        out.flush();
        StringBuilder sb = new StringBuilder();
        for (;;) {
            int ch = in.read();
            if (ch < 0 || ch == 10) {
                break;
            }
            sb.append((char) ch);
        }
        return new DumbFile(new File(rootDir, URLEncoder.encode("dg."+sb, "UTF-8")), usage, mode, rock);
    }

    @Override
    public GlkFile fileCreateFromFile(int usage, GlkFile file, int rock) throws IOException {
        return new DumbFile(((DumbFile) file).file, usage, ((DumbFile) file).mode, rock);
    }

    private class DumbFile extends GlkFile {
        final File file;
        final int usage;
        final int mode;

        DumbFile(File file, int usage, int mode, int rock) {
            super(rock);
            this.file = file;
            this.usage = usage;
            this.mode = mode;
        }

        @Override
        public void delete() {
            file.delete();
        }

        @Override
        public boolean exists() {
            return file.exists();
        }
    }

    private class DumbStreamFile extends GlkStream {
        final RandomAccessFile file;
        final boolean unicode;
        int readCount = 0;
        int writeCount = 0;

        DumbStreamFile(GlkFile file, int mode, boolean unicode, int rock) throws IOException {
            super(rock);
            rootDir.mkdirs();
            this.file = new RandomAccessFile(((DumbFile) file).file, mode == GlkFile.ModeRead ? "r" : "rw");
            switch (mode) {
            case GlkFile.ModeWrite:
                this.file.setLength(0L);
                break;
            case GlkFile.ModeRead:
            case GlkFile.ModeReadWrite:
                this.file.seek(0L);
                break;
            case GlkFile.ModeWriteAppend:
                this.file.seek(this.file.length());
                break;
            default:
                throw new IllegalArgumentException();
            }
            this.unicode = unicode;
        }

        @Override
        public GlkStreamResult close() throws IOException {
            file.close();
            destroy();
            return new GlkStreamResult(readCount, writeCount);
        }

        @Override
        public void putChar(int ch) throws IOException {
            writeCount++;
            if (unicode) {
                file.writeInt(ch & 255);
            } else {
                file.write(ch);
            }
        }

        @Override
        public void putString(CharSequence string) throws IOException {
            writeCount += string.length();
            if (unicode) {
                for (int i = 0; i < string.length(); i++) {
                    file.writeInt(string.charAt(i) & 255);
                }
            } else {
                file.writeBytes(string.toString());
            }
        }

        @Override
        public void putBuffer(GlkByteArray buffer) throws IOException {
            writeCount += buffer.getArrayLength();
            if (unicode) {
                for (int i = 0; i < buffer.getArrayLength(); i++) {
                    file.writeInt(buffer.getByteElementAt(i) & 255);
                }
            } else {
                for (int i = 0; i < buffer.getArrayLength(); i++) {
                    file.write(buffer.getByteElementAt(i));
                }
            }
        }

        @Override
        public void putCharUni(int ch) throws IOException {
            writeCount++;
            if (unicode) {
                file.writeInt(ch);
            } else {
                file.write(ch);
            }
        }

        @Override
        public void putStringUni(UnicodeString string) throws IOException {
            writeCount += string.codePointCount();
            if (unicode) {
                for (int i = 0; i < string.codePointCount(); i++) {
                    file.writeInt(string.codePointAt(i));
                }
            } else {
                for (int i = 0; i < string.codePointCount(); i++) {
                    file.write(string.codePointAt(i));
                }
            }
        }

        @Override
        public void putBufferUni(GlkIntArray buffer) throws IOException {
            writeCount += buffer.getArrayLength();
            if (unicode) {
                for (int i = 0; i < buffer.getArrayLength(); i++) {
                    file.writeInt(buffer.getIntElementAt(i));
                }
            } else {
                for (int i = 0; i < buffer.getArrayLength(); i++) {
                    file.write(buffer.getIntElementAt(i));
                }
            }
        }

        @Override
        public void setStyle(int style) {
        }

        @Override
        public int getChar() throws IOException {
            int ch = -1;
            try {
                if (unicode) {
                    ch = file.readInt() & 255;
                } else {
                    ch = file.readUnsignedByte();
                }
                readCount++;
            } catch (EOFException e) {
            }
            return ch;
        }

        @Override
        public int getLine(GlkByteArray buffer) throws IOException {
            int index = 0;
            while (index < buffer.getArrayLength() - 1) {
                int ch;
                try {
                    if (unicode) {
                        ch = file.readInt() & 255;
                    } else {
                        ch = file.readUnsignedByte();
                    }
                } catch (EOFException e) {
                    break;
                }
                readCount++;
                buffer.setByteElementAt(index, ch);
                index++;
                if (ch == 10) {
                    break;
                }
            }
            buffer.setByteElementAt(index, 0);
            return index;
        }

        @Override
        public int getBuffer(GlkByteArray buffer) throws IOException {
            int index = 0;
            while (index < buffer.getArrayLength() - 1) {
                int ch;
                try {
                    if (unicode) {
                        ch = file.readInt() & 255;
                    } else {
                        ch = file.readUnsignedByte();
                    }
                } catch (EOFException e) {
                    break;
                }
                readCount++;
                buffer.setByteElementAt(index, ch);
                index++;
            }
            buffer.setByteElementAt(index, 0);
            return index;
        }

        @Override
        public int getCharUni() throws IOException {
            int ch = -1;
            try {
                if (unicode) {
                    ch = file.readInt();
                } else {
                    ch = file.readUnsignedByte();
                }
                readCount++;
            } catch (EOFException e) {
            }
            return ch;
        }

        @Override
        public int getLineUni(GlkIntArray buffer) throws IOException {
            int index = 0;
            while (index < buffer.getArrayLength() - 1) {
                int ch;
                try {
                    if (unicode) {
                        ch = file.readInt();
                    } else {
                        ch = file.readUnsignedByte();
                    }
                } catch (EOFException e) {
                    break;
                }
                readCount++;
                buffer.setIntElementAt(index, ch);
                index++;
                if (ch == 10) {
                    break;
                }
            }
            buffer.setIntElementAt(index, 0);
            return index;
        }

        @Override
        public int getBufferUni(GlkIntArray buffer) throws IOException {
            int index = 0;
            while (index < buffer.getArrayLength() - 1) {
                int ch;
                try {
                    if (unicode) {
                        ch = file.readInt();
                    } else {
                        ch = file.readUnsignedByte();
                    }
                } catch (EOFException e) {
                    break;
                }
                readCount++;
                buffer.setIntElementAt(index, ch);
                index++;
            }
            buffer.setIntElementAt(index, 0);
            return index;
        }

        @Override
        public void setPosition(int position, int seekMode) throws IOException {
            switch (seekMode) {
            case GlkStream.SeekModeStart:
                file.seek((long) position);
                break;
            case GlkStream.SeekModeCurrent:
                file.seek(file.getFilePointer() + position);
                break;
            case GlkStream.SeekModeEnd:
                file.seek(file.length() + position);
                break;
            default:
                throw new IllegalArgumentException();
            }
        }

        @Override
        public int getPosition() throws IOException {
            return (int) file.getFilePointer();
        }

        @Override
        public DataOutput getDataOutput() {
            return file;
        }

        @Override
        public DataInput getDataInput() {
            return file;
        }
    }


    @Override
    public GlkEvent select() throws IOException {
        printProfilingData(5);
        Glulx.resetProfilingData();
        out.flush();
        if (lineEventBuffer != null) {
            int count = 0;
            for (;;) {
                int ch = in.read();
                if (ch == 10 || ch == -1) {
                    lineEventBuffer = null;
                    return new GlkEvent(GlkEvent.TypeLineInput, rootWindow, count, 0);
                }
                if (count < lineEventBuffer.getArrayLength()) {
                    lineEventBuffer.setByteElementAt(count, ch);
                    count++;
                }
            }
        } else {
            assert charEventRequested;
            int input = -1;
            for (;;) {
                int ch = in.read();
                if (ch == 10 || ch == -1) {
                    charEventRequested = false;
                    return new GlkEvent(GlkEvent.TypeCharInput, rootWindow, input == -1 ? ch : input, 0);
                }
                if (input == -1) {
                    input = ch;
                }
            }
        }
    }

    @Override
    public GlkEvent selectPoll() throws IOException {
        return new GlkEvent(GlkEvent.TypeNone, null, 0, 0);
    }


    @Override
    public void requestTimerEvents(int millisecs) {
    }


    @Override
    public boolean imageGetInfo(int resourceId, int[] size) {
        return false;
    }

    public void printProfilingData(int maxLines) throws IOException {
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
            int lines = 0;
            for (Map.Entry<String,long[]> entry : list) {
                long[] numbers = entry.getValue();
                out.append(String.format("%14s c=%8d t=%6dms avg=%.3fus\n", entry.getKey(), numbers[0], numbers[1], numbers[0] == 0 ? 0.0 : 1000.0 * (double) numbers[1]/(double) numbers[0]));
                lines++;
                if (lines >= maxLines) {
                    break;
                }
            }
        }
    }
}
