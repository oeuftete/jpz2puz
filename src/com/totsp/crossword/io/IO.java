package com.totsp.crossword.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;

import com.totsp.crossword.puz.Box;
import com.totsp.crossword.puz.Puzzle;


public class IO {
  public static final String FILE_MAGIC = "ACROSS&DOWN";
  public static final String VERSION_STRING = "1.3";

  private static final Charset CHARSET = Charset.forName("Cp1252");

  // Extra Section IDs
  private static final int GEXT = 0;

  // GEXT section bitmasks
  private static final byte GEXT_SQUARE_CIRCLED = (byte) 0x80;

    public static Puzzle loadNative(DataInputStream input) throws IOException {
        Puzzle puz = new Puzzle();

        input.skipBytes(0x18);
        byte[] versionString = new byte[3];
        for (int i = 0; i < versionString.length; i++) {
            versionString[i] = input.readByte();
        }

        input.skip(1);
        puz.setVersion(new String(versionString));

        input.skipBytes(2);
        puz.setSolutionChecksum(Short.reverseBytes(input.readShort()));

        input.skipBytes(0xC);

        puz.setWidth(0xFFFF & input.readByte());
        puz.setHeight(0xFFFF & input.readByte());
        puz.setNumberOfClues(Short.reverseBytes(input.readShort()));

        input.skipBytes(2);
        puz.setScrambled(input.readShort() != 0);

        Box[][] boxes = new Box[puz.getHeight()][puz.getWidth()];
        byte[] answerByte = new byte[1];
        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
              answerByte[0] = input.readByte();
                char solution = new String(answerByte, CHARSET.name() ).charAt(0);

                if (solution != '.') {
                    boxes[x][y] = new Box();
                    boxes[x][y].setSolution((char) solution);
                }
            }
        }

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                answerByte[0] = input.readByte();
                char answer = new String(answerByte, CHARSET.name()).charAt(0);

                if (answer == '.') {
                    continue;
                } else if (answer == '-') {
                    boxes[x][y].setResponse(' ');
                } else {
                    boxes[x][y].setResponse(answer);
                }
            }
        }

        puz.setBoxes(boxes);

        puz.setTitle(readNullTerminatedString(input));
        puz.setAuthor(readNullTerminatedString(input));
        puz.setCopyright(readNullTerminatedString(input));

        ArrayList<String> acrossClues = new ArrayList<String>();
        ArrayList<Integer> acrossCluesLookup = new ArrayList<Integer>();
        ArrayList<Integer> downCluesLookup = new ArrayList<Integer>();
        ArrayList<String> downClues = new ArrayList<String>();
        ArrayList<String> rawClues = new ArrayList<String>();

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] == null) {
                    continue;
                }

                if (boxes[x][y].isAcross() && (boxes[x][y].getClueNumber() != 0)) {
                    String value = readNullTerminatedString(input);

                    acrossCluesLookup.add(boxes[x][y].getClueNumber());
                    acrossClues.add(value);
                    rawClues.add(value);
                }

                if (boxes[x][y].isDown() && (boxes[x][y].getClueNumber() != 0)) {
                    String value = readNullTerminatedString(input);
                    downCluesLookup.add(boxes[x][y].getClueNumber());
                    downClues.add(value);
                    rawClues.add(value);
                }
            }
        }

        puz.setDownClues(downClues.toArray(new String[downClues.size()]));
        puz.setDownCluesLookup(downCluesLookup.toArray(new Integer[downCluesLookup.size()]));
        puz.setAcrossClues(acrossClues.toArray(new String[acrossClues.size()]));
        puz.setAcrossCluesLookup(acrossCluesLookup.toArray(new Integer[acrossCluesLookup.size()]));
        puz.setRawClues(rawClues.toArray(new String[rawClues.size()]));

        puz.setNotes(readNullTerminatedString(input));

        boolean eof = false;

        while(!eof) {
          try {
            switch(readExtraSectionType(input)) {
              case GEXT:
                readGextSection(input, puz);
                break;
              default:
                skipExtraSection(input);
            }
          } catch(EOFException e) {
            eof = true;
          }
        }

        return puz;
    }

    public static int readExtraSectionType(DataInputStream input) throws IOException {
      byte[] title = new byte[4];
      for(int i = 0; i < title.length; i++) {
        title[i] = input.readByte();
      }
      String section = new String(title);
      if("GEXT".equals(section)) {
        return GEXT;
      }
      return -1;
    }

    public static void skipExtraSection(DataInputStream input) throws IOException {
      short numBytes = Short.reverseBytes(input.readShort());
      input.skipBytes(2); // checksum
      input.skipBytes(numBytes); // data
      input.skipBytes(1); // null terminator
    }

    public static void readGextSection(DataInputStream input, Puzzle puz) throws IOException {
      puz.setGEXT(true);
      input.skipBytes(4);
      Box[][] boxes = puz.getBoxes();
        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
              byte gextInfo = input.readByte();
              if((gextInfo & GEXT_SQUARE_CIRCLED) != 0) {
                if (boxes[x][y] != null) {
                  boxes[x][y].setCircled(true);
                }
              }
            }
        }
        input.skipBytes(1);
    }

    public static String readNullTerminatedString(InputStream is)
        throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(128);
        for (byte nextByte = (byte) is.read(); nextByte != 0x0;
                nextByte = (byte) is.read()) {
            if (nextByte != 0x0) {
              baos.write(nextByte);
            }
            if(baos.size() > 4096){
              throw new IOException("Run on string!");
            }
        }
        return (baos.size() == 0) ? null : new String(baos.toByteArray(), CHARSET.name());
    }

    public static void saveNative(Puzzle puz, DataOutputStream dos)
        throws IOException {
      /*
       * We write the puzzle to a temporary output stream, with 0 entered for any checksums.
       * Once we have this written out, we can calculate all of the checksums and write the
       * file to the original output stream.
       */
      ByteArrayOutputStream tmp = new ByteArrayOutputStream();
      DataOutputStream tmpDos = new DataOutputStream(tmp);

      tmpDos.writeShort(0);

        tmpDos.writeBytes(FILE_MAGIC);
        tmpDos.writeByte(0);

        tmpDos.write(new byte[10]);

        tmpDos.writeBytes(VERSION_STRING);
        tmpDos.writeByte(0);

        tmpDos.write(new byte[2]);

        tmpDos.writeShort(Short.reverseBytes(puz.getSolutionChecksum()));

        tmpDos.write(new byte[0xC]);

        int width = puz.getWidth();
        int height = puz.getHeight();
        int numberOfBoxes = width * height;

        tmpDos.writeByte(width);
        tmpDos.writeByte(height);

        int numberOfClues = puz.getNumberOfClues();

        tmpDos.writeShort(Short.reverseBytes((short) numberOfClues));
        tmpDos.writeShort(Short.reverseBytes((short) 1));

        short scrambled = puz.isScrambled() ? (short) 4 : (short) 0;
        tmpDos.writeShort(Short.reverseBytes(scrambled));

        Box[][] boxes = puz.getBoxes();
        byte[] gextSection = null;
        if (puz.getGEXT()) {
          gextSection = new byte[numberOfBoxes];
        }

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] == null) {
                    tmpDos.writeByte('.');
                } else {
                  byte val = (byte) boxes[x][y].getSolution();//Character.toString().getBytes("Cp1252")[0];
                  if (puz.getGEXT() && boxes[x][y].isCircled()) {
                    gextSection[width*x+y] = GEXT_SQUARE_CIRCLED;
                  }
                    tmpDos.writeByte(val);
                }
            }
        }

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] == null) {
                    tmpDos.writeByte('.');
                } else {
                  byte val = (byte) boxes[x][y].getResponse(); //Character.toString().getBytes("Cp1252")[0];
                    tmpDos.writeByte((boxes[x][y].getResponse() == ' ') ? '-'
                                                                : val);
                }
            }
        }

        writeNullTerminatedString(tmpDos, puz.getTitle());
        writeNullTerminatedString(tmpDos, puz.getAuthor());
        writeNullTerminatedString(tmpDos, puz.getCopyright());

        for (String clue : puz.getRawClues()) {
            writeNullTerminatedString(tmpDos, clue);
        }

        writeNullTerminatedString(tmpDos, puz.getNotes());

        if (puz.getGEXT()) {
          tmpDos.writeBytes("GEXT");
          tmpDos.writeShort(Short.reverseBytes((short) numberOfBoxes));
          // Calculate checksum here so we don't need to find this place in the file later.
          int c_gext = cksum_region(gextSection, 0, numberOfBoxes, 0);
          tmpDos.writeShort(Short.reverseBytes((short) c_gext));
          tmpDos.write(gextSection);
          tmpDos.writeByte(0);
        }

        tmpDos.flush();
        byte[] puzByteArray = tmp.toByteArray();
        ByteBuffer bb = ByteBuffer.wrap(puzByteArray);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // Calculate checksums and write to byte array.
        int c_cib = cksum_cib(puzByteArray, 0);
        bb.putShort(0x0E, (short) c_cib);

        int c_primary = cksum_primary_board(puzByteArray, numberOfBoxes, numberOfClues, c_cib);
        bb.putShort(0, (short) c_primary);

        int c_sol = cksum_solution(puzByteArray, numberOfBoxes, 0);
        int c_grid = cksum_grid(puzByteArray, numberOfBoxes, 0);
        int c_part = cksum_partial_board(puzByteArray, numberOfBoxes, numberOfClues, 0);

        bb.position(0x10);
        bb.put((byte) (0x49 ^ (c_cib & 0xFF)));
        bb.put((byte) (0x43 ^ (c_sol & 0xFF)));
        bb.put((byte) (0x48 ^ (c_grid & 0xFF)));
        bb.put((byte) (0x45 ^ (c_part & 0xFF)));
        bb.put((byte) (0x41 ^ ((c_cib & 0xFF00) >> 8)));
        bb.put((byte) (0x54 ^ ((c_sol & 0xFF00) >> 8)));
        bb.put((byte) (0x45 ^ ((c_grid & 0xFF00) >> 8)));
        bb.put((byte) (0x44 ^ ((c_part & 0xFF00) >> 8)));

        // Dump byte array to output stream.
        dos.write(puzByteArray);
    }

    public static void writeNullTerminatedString(OutputStream os, String value)
        throws IOException {

        value = (value == null) ? "" : value;
        byte[] encoded = CHARSET.encode(value).array();
        os.write(encoded);
        os.write(0);
    }

    public static void save(Puzzle puz, DataOutputStream puzzleOutputStream) throws IOException{
        IO.saveNative(puz, puzzleOutputStream);
      puzzleOutputStream.close();
    }

    public static void save(Puzzle puz, File baseFile) throws IOException {
      long incept = System.currentTimeMillis();
      FileOutputStream puzzle= new FileOutputStream(baseFile);
      IO.save(puz, new DataOutputStream(puzzle));
      System.out.println("Save complete in "+(System.currentTimeMillis() - incept));
    }

    public static Puzzle load(DataInputStream puzzleInput) throws IOException{
        Puzzle puz = IO.loadNative(puzzleInput);
        puzzleInput.close();
        return puz;
    }

    public static Puzzle load(File baseFile) throws IOException {
      FileInputStream fis = new FileInputStream(baseFile);
      Puzzle puz = IO.loadNative( new DataInputStream(fis));
      fis.close();
      return puz;
    }

    public static int cksum_region(byte[] data, int offset, int length, int cksum) {
      for(int i = offset; i < offset + length; i++) {
        if ((cksum & 0x1) != 0) {
          cksum = (cksum >> 1) + 0x8000;
        } else {
          cksum = cksum >> 1;
        }
        cksum += (0xFF & data[i]);
        cksum = cksum & 0xFFFF;
      }
      return cksum;
    }

    private static int cksum_cib(byte[] puzByteArray, int cksum) {
      return cksum_region(puzByteArray, 0x2C, 8, cksum);
    }

    private static int cksum_primary_board(byte[] puzByteArray, int numberOfBoxes,
        int numberOfClues, int cksum) {
      cksum = cksum_solution(puzByteArray, numberOfBoxes, cksum);
      cksum = cksum_grid(puzByteArray, numberOfBoxes, cksum);
      cksum = cksum_partial_board(puzByteArray, numberOfBoxes, numberOfClues, cksum);
      return cksum;
    }

    private static int cksum_solution(byte[] puzByteArray, int numberOfBoxes, int cksum) {
      return cksum_region(puzByteArray, 0x34, numberOfBoxes, cksum);
    }

    private static int cksum_grid(byte[] puzByteArray, int numberOfBoxes, int cksum) {
      return cksum_region(puzByteArray, 0x34 + numberOfBoxes, numberOfBoxes, cksum);
    }

    private static int cksum_partial_board(byte[] puzByteArray, int numberOfBoxes,
        int numberOfClues, int cksum) {
      int offset = 0x34 + 2*numberOfBoxes;
      for(int i = 0; i < 4 + numberOfClues; i++) {
          int startOffset = offset;
          while(puzByteArray[offset] != 0) {
            offset++;
          }
          int length = offset - startOffset;
          if (i > 2 && i < 3 + numberOfClues) {
            cksum = cksum_region(puzByteArray, startOffset, length, cksum);
          }
          else if (length > 0) {
            cksum = cksum_region(puzByteArray, startOffset, length+1, cksum);
          }
          offset++;
        }
      return cksum;
    }
}
