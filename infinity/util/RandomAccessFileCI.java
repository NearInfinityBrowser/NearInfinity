package infinity.util;

import java.io.*;

public class RandomAccessFileCI extends RandomAccessFile {
	public RandomAccessFileCI (String s1, String s2) throws FileNotFoundException {
		super(s1.toLowerCase(),s2.toLowerCase());
	}
	
	public RandomAccessFileCI (File f, String s) throws FileNotFoundException {
		super(f,s.toLowerCase());
	}
}
