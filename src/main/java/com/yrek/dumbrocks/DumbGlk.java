package com.yrek.dumbrocks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

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
import com.yrek.ifstd.glk.UnicodeString;

public class DumbGlk implements Glk {
    private final Reader in;
    private final Writer out;
    private final int width;
    private final int height;

    private GlkStream currentStream = null;
    private GlkStream rootStream = null;
    private GlkWindow rootWindow = null;
    private int outputCount = 0;
    private boolean charEventRequested = false;
    private GlkByteArray lineEventBuffer = null;
    private int lineEventInitLength = 0;

    public DumbGlk(Reader in, Writer out, int width, int height) {
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
            return 0;
        case GlkGestalt.LineInput:
            return 1;
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
        case GlkGestalt.Unicode:
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
        rootStream = new GlkStream(0) {
            @Override
            public GlkStreamResult close() throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public void putChar(int ch) throws IOException {
                outputCount++;
                out.write((char) (ch & 255));
            }

            @Override
            public void putString(CharSequence string) throws IOException {
                outputCount += string.length();
                out.append(string);
            }

            @Override
            public void putBuffer(GlkByteArray buffer) throws IOException {
                int length = buffer.getArrayLength();
                outputCount += length;
                for (int i = 0; i < length; i++) {
                    out.write((char) (buffer.getByteElementAt(i) & 255));
                }
            }

            @Override
            public void putCharUni(int ch) throws IOException {
                outputCount++;
                out.write(Character.toChars(ch));
            }

            @Override
            public void putStringUni(UnicodeString string) throws IOException {
                outputCount += string.codePointCount();
                out.append(string);
            }

            @Override
            public void putBufferUni(GlkIntArray buffer) throws IOException {
                int length = buffer.getArrayLength();
                outputCount += length;
                for (int i = 0; i < length; i++) {
                    out.write(Character.toChars(buffer.getIntElementAt(i)));
                }
            }

            @Override
            public void setStyle(int style) {
            }

            @Override
            public int getChar() throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public int getLine(GlkByteArray buffer) throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public int getBuffer(GlkByteArray buffer) throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public int getCharUni() throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public int getLineUni(GlkIntArray buffer) throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public int getBufferUni(GlkIntArray buffer) throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public void setPosition(int position, int seekMode) throws IOException {
                throw new IllegalStateException();
            }

            @Override
            public int getPosition() throws IOException {
                return outputCount;
            }
        };
        rootWindow = new GlkWindow(rootStream, rock) {
            @Override
            public GlkStreamResult close() throws IOException {
                if (currentStream == getStream()) {
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
        };
        return rootWindow;
    }

    @Override
    public void setWindow(GlkWindow window) {
        currentStream = window == null ? null : window.getStream();
    }

    @Override
    public GlkStream streamOpenFile(GlkFile file, int mode, int rock) throws IOException {
        throw new RuntimeException("unimplemented");
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
        throw new RuntimeException("unimplemented");
    }

    @Override
    public GlkFile fileCreateByName(int usage, CharSequence name, int rock) throws IOException {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public GlkFile fileCreateByPrompt(int usage, int mode, int rock) throws IOException {
        throw new RuntimeException("unimplemented");
    }

    @Override
    public GlkFile fileCreateFromFile(int usage, GlkFile file, int rock) throws IOException {
        throw new RuntimeException("unimplemented");
    }


    @Override
    public GlkEvent select() throws IOException {
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
                if (input != -1) {
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
}
