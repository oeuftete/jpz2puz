package com.totsp.crossword.puz;

import java.util.Arrays;
import java.util.Date;

import com.totsp.crossword.io.IO;


public class Puzzle {
    private String author;
    private String copyright;
    private String notes;
    private String title;
    private String[] acrossClues;
    private Integer[] acrossCluesLookup;
    private String[] downClues;
    private Integer[] downCluesLookup;
    private int numberOfClues;
    private Date pubdate = new Date();
    private String source;
    private String sourceUrl = "";
    private Box[][] boxes;
    private Box[] boxesList;
    private String[] rawClues;
    private boolean updatable;
    private int height;
    private int width;
    private long playedTime;
    private boolean scrambled;
    private short solutionChecksum;
    private String version;
    private boolean hasGEXT;
    private boolean across = true;
    
    // Temporary fields used for unscrambling.
    private int[] unscrambleKey;
    private byte[] unscrambleTmp;
    private byte[] unscrambleBuf;

    public void setAcrossClues(String[] acrossClues) {
        this.acrossClues = acrossClues;
    }

    public String[] getAcrossClues() {
        return acrossClues;
    }

    public void setAcrossCluesLookup(Integer[] acrossCluesLookup) {
        this.acrossCluesLookup = acrossCluesLookup;
    }

    public Integer[] getAcrossCluesLookup() {
        return acrossCluesLookup;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setBoxes(Box[][] boxes) {
        this.boxes = boxes;

        int clueCount = 1;

        for (int x = 0; x < boxes.length; x++) {
            boolean tickedClue = false;

            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] == null) {
                    continue;
                }

                if (((x == 0) || (boxes[x - 1][y] == null)) &&
                        (((x + 1) < boxes.length) && (boxes[x + 1][y] != null))) {
                    boxes[x][y].setDown(true);

                    if ((x == 0) || (boxes[x - 1][y] == null)) {
                        boxes[x][y].setClueNumber(clueCount);
                        tickedClue = true;
                    }
                }

                if (((y == 0) || (boxes[x][y - 1] == null)) &&
                        (((y + 1) < boxes[x].length) &&
                        (boxes[x][y + 1] != null))) {
                    boxes[x][y].setAcross(true);

                    if ((y == 0) || (boxes[x][y - 1] == null)) {
                        boxes[x][y].setClueNumber(clueCount);
                        tickedClue = true;
                    }
                }

                if (tickedClue) {
                    clueCount++;
                    tickedClue = false;
                }
            }
        }
    }

    public Box[][] getBoxes() {
        return (boxes == null) ? this.buildBoxes() : boxes;
    }

    public void setBoxesList(Box[] value) {
        System.out.println("Setting list " + value.length);
        this.boxesList = value;
    }

    public Box[] getBoxesList() {
        Box[] result = new Box[boxes.length * boxes[0].length];
        int i = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                result[i++] = boxes[x][y];
            }
        }

        return result;
    }
    
    /**
     * Initialize the temporary unscramble buffers.  Returns the scrambled solution.
     */
    public byte[] initializeUnscrambleData() {
    	unscrambleKey = new int[4];
    	unscrambleTmp = new byte[9];
    	
    	byte[] solution = getSolutionDown();
    	unscrambleBuf = new byte[solution.length];
    	
    	return solution;
    }
	
    /**
     * Attempts to unscramble the solution using the input key.  Modifications to the solution
     * array occur in place.  If true, the unscrambled solution checksum is valid.
     */
	public boolean tryUnscramble(int key_int, byte[] solution) {
		unscrambleKey[0] = (key_int / 1000) % 10;
		unscrambleKey[1] = (key_int / 100) % 10;
		unscrambleKey[2] = (key_int / 10) % 10;
		unscrambleKey[3] = (key_int / 1) % 10;
		
		for (int i = 3; i >= 0; i--) {
			unscrambleString(solution);
			System.arraycopy(unscrambleBuf, 0, solution, 0, unscrambleBuf.length);
			unshiftString(solution, unscrambleKey[i]);
			for (int j = 0; j < solution.length; j++) {
				int letter = (solution[j] & 0xFF) - unscrambleKey[j % 4];
				if (letter < 65) {
					letter += 26;
				}
				solution[j] = (byte) letter;
			}
		}

		return solutionChecksum == (short) IO.cksum_region(solution, 0, solution.length, 0);
	}

	public void unshiftString(byte[] str, int keynum) {
		System.arraycopy(str, str.length-keynum, unscrambleTmp, 0, keynum);
		System.arraycopy(str, 0, str, keynum, str.length-keynum);
		System.arraycopy(unscrambleTmp, 0, str, 0, keynum);
	}
	
	public void unscrambleString(byte[] str) {
		int oddIndex = 0;
		int evenIndex = str.length / 2;
		for (int i = 0; i < str.length; i++) {
			if (i % 2 == 0) {
				unscrambleBuf[evenIndex++] = str[i];
			} else {
				unscrambleBuf[oddIndex++] = str[i];
			}
		}
	}
	
	private byte[] getSolutionDown() {
		StringBuilder ans = new StringBuilder();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (boxes[y][x] != null) {
					ans.append(boxes[y][x].getSolution());
				}
			}
		}
		return ans.toString().getBytes();
	}
	
	public void setUnscrambledSolution(byte[] solution) {
		int i = 0;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (boxes[y][x] != null) {
					boxes[y][x].setSolution((char) solution[i++]);
				}
			}
		}
		setScrambled(false);
		setUpdatable(false);
	}

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setDate(Date date) {
        this.pubdate = date;
    }

    public Date getDate() {
        return pubdate;
    }

    public void setDownClues(String[] downClues) {
        this.downClues = downClues;
    }

    public String[] getDownClues() {
        return downClues;
    }

    public void setDownCluesLookup(Integer[] downCluesLookup) {
        this.downCluesLookup = downCluesLookup;
    }

    public Integer[] getDownCluesLookup() {
        return downCluesLookup;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNumberOfClues(int numberOfClues) {
        this.numberOfClues = numberOfClues;
    }

    public int getNumberOfClues() {
        return numberOfClues;
    }

    public int getPercentComplete() {
        int total = 0;
        int correct = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (boxes[x][y].getResponse() == boxes[x][y].getSolution()) {
                        correct++;
                    }
                }
            }
        }

        return (correct * 100) / (total);
    }
    
    public int getPercentFilled() {
    	int total = 0;
    	int filled = 0;

        for (int x = 0; x < boxes.length; x++) {
            for (int y = 0; y < boxes[x].length; y++) {
                if (boxes[x][y] != null) {
                    total++;

                    if (boxes[x][y].getResponse() != ' ') {
                        filled++;
                    }
                }
            }
        }

        return (filled * 100) / (total);
    }

    public void setRawClues(String[] rawClues) {
        this.rawClues = rawClues;
    }

    public String[] getRawClues() {
        return rawClues;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setTime(long time) {
        this.playedTime = time;
    }

    public long getTime() {
        return this.playedTime;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public boolean isUpdatable() {
        return updatable;
    }
    
    public void setVersion(String version) {
    	this.version = version;
    }

     public String getVersion() {
    	 return version;
     }
    
    public void setGEXT(boolean hasGEXT) {
    	this.hasGEXT = hasGEXT;
    }
    
    public boolean getGEXT() {
    	return hasGEXT;
    }
    
    public void setAcross(boolean across) {
    	this.across = across;
    }
    
    public boolean getAcross() {
    	return across;
    }
    
    public void setScrambled(boolean scrambled) {
    	this.scrambled = scrambled;
    }
    
    public boolean isScrambled() {
    	return scrambled;
    }
    
    public void setSolutionChecksum(short checksum) {
    	this.solutionChecksum = checksum;
    }
    
    public short getSolutionChecksum() {
    	return solutionChecksum;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    public Box[][] buildBoxes() {
        System.out.println("Building boxes " + this.height + "x" + this.width);

        int i = 0;
        boxes = new Box[this.height][this.width];

        for (int y = 0; y < this.height; y++) {
            for (int x = 0; x < this.width; x++) {
                boxes[y][x] = boxesList[i++];
            }
        }

        return boxes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Puzzle other = (Puzzle) obj;

        if (!Arrays.equals(acrossClues, other.acrossClues)) {
            System.out.println("acrossClues");

            //            for(int i=0; i < acrossClues.length; i++)
            //            	System.out.println((acrossClues[i].equals(other.acrossClues[i]))+"["+acrossClues[i]+"]==["+other.acrossClues[i]+"]");
            return false;
        }

        if (!Arrays.equals(acrossCluesLookup, other.acrossCluesLookup)) {
            System.out.println("acrossCluesLookup");

            return false;
        }

        if (author == null) {
            if (other.author != null) {
                return false;
            }
        } else if (!author.equals(other.author)) {
            System.out.println("author");

            return false;
        }

        Box[][] b1 = boxes;
        Box[][] b2 = other.boxes;
        boolean boxEq = true;

        for (int x = 0; x < b1.length; x++) {
            for (int y = 0; y < b1[x].length; y++) {
                boxEq = boxEq
                    ? ((b1[x][y] == b2[x][y]) || b1[x][y].equals(b2[x][y]))
                    : boxEq;
            }
        }

        if (!boxEq) {
            System.out.println("boxes");

            return false;
        }

        if (copyright == null) {
            if (other.copyright != null) {
                System.out.println("copyright");

                return false;
            }
        } else if (!copyright.equals(other.copyright)) {
            return false;
        }

        if (!Arrays.equals(downClues, other.downClues)) {
            System.out.println("downClues");

            return false;
        }

        if (!Arrays.equals(downCluesLookup, other.downCluesLookup)) {
            System.out.println("downCluesLookup");

            return false;
        }

        if (height != other.height) {
            System.out.println("height");

            return false;
        }

        if (notes == null) {
            if (other.notes != null) {
                return false;
            }
        } else if (!notes.equals(other.notes)) {
            System.out.println("notes");

            return false;
        }

        if (getNumberOfClues() != other.getNumberOfClues()) {
            System.out.println("numberOfClues");

            return false;
        }

        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            System.out.println("title");

            return false;
        }

        if (width != other.width) {
            return false;
        }
        
        if (version == null) {
	        if (other.version != null) {
	            return false;
	        }
        } else if (!version.equals(other.version)) {
            return false;
        }
        
        if (scrambled != other.scrambled) {
        	return false;
        }
        
        if (solutionChecksum != other.solutionChecksum) {
        	return false;
        }

        return true;
    }

    public String findAcrossClue(int clueNumber) {
        return this.acrossClues[Arrays.binarySearch(this.acrossCluesLookup,
            clueNumber)];
    }

    public String findDownClue(int clueNumber) {
        return this.downClues[Arrays.binarySearch(this.downCluesLookup,
            clueNumber)];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + Arrays.hashCode(acrossClues);
        result = (prime * result) + Arrays.hashCode(acrossCluesLookup);
        result = (prime * result) + ((author == null) ? 0 : author.hashCode());
        result = (prime * result) + Arrays.hashCode(boxes);
        result = (prime * result) +
            ((copyright == null) ? 0 : copyright.hashCode());
        result = (prime * result) + Arrays.hashCode(downClues);
        result = (prime * result) + Arrays.hashCode(downCluesLookup);
        result = (prime * result) + height;
        result = (prime * result) + ((notes == null) ? 0 : notes.hashCode());
        result = (prime * result) + getNumberOfClues();
        result = (prime * result) + ((title == null) ? 0 : title.hashCode());
        result = (prime * result) + ((version == null) ? 0 : version.hashCode());
        result = (prime * result) + width;

        return result;
    }

    @Override
    public String toString() {
        return "Puzzle " + boxes.length + " x " + boxes[0].length + " " +
        this.title;
    }
}