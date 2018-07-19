package gov.nih.nlm.pmc;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nih.nlm.ling.util.FileUtils;

/**
 * A class to download PMC articles. Input is either a file of PMC IDs (one on each line) or 
 * a directory of files whose corresponding PMC XML articles are to be downloaded.
 * 
 * @author Halil Kilicoglu
 *
 */

public class PubMedCentralDownload {
	private static Logger log = Logger.getLogger(PubMedCentralDownload.class.getName());	
		
	public static void processDirectory(String in, String out) 
			throws Exception {
		File inDir = new File(in);
		if (inDir.isDirectory() == false) return;
		File outDir = new File(out);
		if (outDir.isDirectory() == false) return;
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(in, false, "xml");
		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".xml", "");
			log.log(Level.INFO,"Processing {0}: {1}.", new Object[]{id,++fileNum});
			String outFile = outDir.getAbsolutePath() + File.separator + id + ".xml";
			if (new File(outFile).exists()) continue;
			PrintWriter pw = new PrintWriter(outFile);
			String xml = PMCSearch.runFetch(id);
			pw.write(xml);
			pw.flush();
			pw.close();
//			String cited = PMCSearch.runCited(id);
//			System.out.println("CITED: " + cited);
		}
	}
	
	public static void processPmcIds(String in, String out) 
			throws Exception {
		File inFile = new File(in);
		if (inFile.exists() == false) return;
		File outDir = new File(out);
		if (outDir.isDirectory() == false) return;
		int fileNum = 0;
		List<String> ids = FileUtils.linesFromFile(in, "UTF-8");
		for (String id: ids) {
			String sid = id.substring(3);
			log.log(Level.INFO,"Processing {0}: {1}.", new Object[]{id,++fileNum});
			String outFile = outDir.getAbsolutePath() + File.separator + id + ".xml";
			if (new File(outFile).exists()) continue;
			PrintWriter pw = new PrintWriter(outFile);
			String xml = PMCSearch.runFetch(id);
			pw.write(xml);
			pw.flush();
			pw.close();
		}
	}
		 
	public static void main(String[] args) 
			throws Exception {
		if (args.length < 2) {
			System.err.print("Usage: PMCIdFile_Or_XMLDirectory outputDirectory");
		}
		String in = args[0];
		String out = args[1];
		File inDir = new File(in);
		if (inDir.exists() == false) {
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + outDir + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		if (inDir.isDirectory())
			processDirectory(in,out);
		else 
			processPmcIds(in,out);
	}
}
