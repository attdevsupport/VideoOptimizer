package com.att.aro.core.upload;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompressorTest {
	private static final String FILE1 = "temp";

	private static Path TEMP_DIR;
	private static Path TEMP_FILE;

	private Compressor compressor;
	
	@BeforeClass
	public static void setup() throws IOException {
		TEMP_DIR = Files.createTempDirectory("compressionTest");
		TEMP_FILE = Files.createTempFile(TEMP_DIR, FILE1, null);
	}
	
	@Before
	public void init() {
		compressor = new Compressor();
	}
	
	@Test
	public void testEncode() throws Exception {
		Files.write(TEMP_FILE, new byte[] {'1', '2', '3', '4', '5', '6'});
		String zipPath = TEMP_FILE.toString();
		String z64Path = zipPath + "64";
		compressor.encode(zipPath);
		assertEquals("MTIzNDU2", Files.readAllLines(Paths.get(z64Path)).get(0));
	}
	
	@Test
	public void testEncodeEmpty() throws Exception {
		Files.write(TEMP_FILE, new byte[] {});
		String zipPath = TEMP_FILE.toString();
		String z64Path = zipPath + "64";
		compressor.encode(zipPath);
		assertEquals(0, Files.readAllLines(Paths.get(z64Path)).size());		
	}
	
	@AfterClass
	public static void cleanup() {
		FileUtils.deleteQuietly(TEMP_DIR.toFile());
	}

}
