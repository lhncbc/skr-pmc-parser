package gov.nih.nlm.pmc;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Sentence;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.process.ComponentLoader;
import gov.nih.nlm.ling.process.SentenceSegmenter;
import gov.nih.nlm.ling.util.FileUtils;
import gov.nih.nlm.ling.wrappers.CoreNLPWrapper;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * A class to parse PMC XML files. 
 * 
 * @author Halil Kilicoglu
 *
 */
public class PMCArticleParser {
	private static Logger log = Logger.getLogger(PMCArticleParser.class.getName());	
	
	private static SentenceSegmenter segmenter = null;
	
    private static final String XREF_PATH="//xref";
	   
	private static Document parseArticle(String id, String filename) throws Exception {
		MyPMCArticle article = new MyPMCArticle(filename);
		String title = article.getTitle();
		String abstText = article.getAbstractText();
		String fullText = article.getFullTextText();
		String backMatter = article.getBackMatterText();
		
		Map<String,String> floatTexts = article.getFloatsGroupText();
		
//		PMCArticleAbstract abst = article.getAbstract();
//		PMCArticleFullText full = article.getFullText();
		
/*		log.info("Title:" + title);
		log.info("Abstract: " + abstText);
		log.info("Full-text: " + fullText);
		log.info("Back matter: " + backMatter);*/
		
		boolean hasAbstractTitle = hasAbstractTitle(article);
		String allText = "Title\n" + title + "\n" + (hasAbstractTitle ? "" :  "Abstract\n" ) + abstText + "\n" +  fullText + "\nBack matter\n" + backMatter;
		Document doc = new Document(id, allText);
		PMCSectionSegmenter sectSegmenter = new PMCSectionSegmenter(article);
		sectSegmenter.segment(doc);
		addBackMatterAsSection(allText,backMatter,doc);
		if (!hasAbstractTitle) {
			List<Section> sections = doc.getSections();
			if (sections != null & sections.get(0).getTitleSpan() == null) {
				int absInd = allText.indexOf("Abstract\n");
				Section abs = new Section(new Span(absInd,absInd+8),new Span(absInd,absInd + 9 + abstText.length()),doc);
				sections.set(0, abs);
			}
		}
		addTitleAsSection(allText,title,doc);

		
		Document nDoc = null;
		if (floatTexts.size() > 0) {
			nDoc = incorporateFloatTexts(doc,article, floatTexts);
		}
		
		if (nDoc == null) {
			nDoc = doc;
		}
		
		
		List<Sentence> sentences = new ArrayList<>();
		segmenter.segment(nDoc.getText(), sentences);
		log.log(Level.INFO,"Number of sentences {0}: {1}.", new Object[]{id,sentences.size()});
		int i = 1;
		for (Sentence sentence: sentences) {
			System.out.println("Sentence " + i++ + ": " + sentence.getText());
			CoreNLPWrapper.coreNLP(sentence);
			nDoc.addSentence(sentence);
			sentence.setDocument(nDoc);
		}
		return nDoc;
	}
	
	private static void addTitleAsSection(String text, String title,Document doc) {
		List<Section> sects = doc.getSections();
		int titleOffset = text.indexOf("Title\n");
		sects.add(0, new Section(new Span(titleOffset,titleOffset+5),new Span(titleOffset,titleOffset+6+title.length()),doc));
		doc.setSections(sects);
	}
	
	private static void addBackMatterAsSection(String text, String backMatter, Document doc) {
		int ind = text.indexOf("Back matter\n");
		Section bm = new Section(new Span(ind,ind+11),new Span(ind,ind+12 + backMatter.length()),doc);
		doc.addSection(bm);
	}
	
	private static Document incorporateFloatTexts(Document in, MyPMCArticle article, Map<String,String> floatTexts) {
		String inText = in.getText();
		String outText = new String(inText);
		List<Section> inSections = in.getSections();
		List<Section> outSections = new ArrayList<>(inSections);
		for (String id: floatTexts.keySet()) {
			String text = floatTexts.get(id);
			String textWoCits = article.replaceCitation(text);
			int offset = identifyInsertionOffset(id,outText, article);
			if (offset >= 0) {
				outText = outText.substring(0,offset) + textWoCits +"\n" + outText.substring(offset);
				outSections = updateSectionInfo(offset, textWoCits.length()+1,outSections);
			}
		}
		Document nDoc = new Document(in.getId(),outText);
		List<Section> updatedSections = updateSectionDocInfo(outSections,nDoc);
		nDoc.setSections(updatedSections);
		return nDoc;
	}
	
	private static int identifyInsertionOffset(String id, String currText, MyPMCArticle article) {
		String idref = XREF_PATH + "[@rid='" + id + "']";
		String parentPath = idref +"/parent::p";
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			XPathExpression expression = xPath.compile(parentPath);
			NodeList nodes = (NodeList) expression.evaluate(article.getDocument(), XPathConstants.NODESET);
			if (nodes.getLength() ==0) return -1;
			Node pn = nodes.item(0);			
			String ptext = article.getTextHelper(pn).toString();
			return currText.indexOf(ptext);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	private static boolean hasAbstractTitle(MyPMCArticle article) {
		String path = "//abstract/title";
		try {
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			XPathExpression expression = xPath.compile(path);
			NodeList nodes = (NodeList) expression.evaluate(article.getDocument(), XPathConstants.NODESET);
			if (nodes.getLength() ==0) return false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private static List<Section> updateSectionInfo(int offset, int length, List<Section> inSections) {
		if (inSections == null || inSections.size() == 0) {
			return new ArrayList<>();
		}
		List<Section> out = new ArrayList<>();
		for (Section s: inSections) {
			List<Section> subs = s.getSubSections();
			List<Section> outSubs = updateSectionInfo(offset,length,subs);
			Span titleSpan = s.getTitleSpan();
			Span textSpan = s.getTextSpan();
			int textS = textSpan.getBegin();
			int textE = textSpan.getEnd();
			Span nTitleSpan = null;
			Span nTextSpan = null;
			if (titleSpan != null) {
				int titleS = titleSpan.getBegin();
				int titleE = titleSpan.getEnd();
				if (titleS >= offset && titleE > offset) nTitleSpan = new Span(titleS + length, titleE+length);
				else if (titleS < offset && titleE >offset) nTitleSpan = new Span(titleS,titleE+length);
				else nTitleSpan = new Span(titleS,titleE);
			}
			if (textS >= offset && textE > offset) nTextSpan = new Span(textS + length, textE+length);
			else if (textS < offset && textE >offset) nTextSpan = new Span(textS,textE+length);
			else nTextSpan = new Span(textS,textE);
			
			Section thSect = new Section(nTitleSpan,nTextSpan,s.getDocument());
			for (Section os: outSubs) {
				thSect.addSubsection(os);
			}
			out.add(thSect);
		}
		return out;
	}
	
	private static List<Section> updateSectionDocInfo(List<Section> sections, Document doc) {
		if (sections == null || sections.size() == 0) {
			return new ArrayList<>();
		}
		List<Section> out = new ArrayList<>();
		for (Section s: sections) {
			List<Section> subs = s.getSubSections();
			List<Section> outSubs = updateSectionDocInfo(subs,doc);
			Section thSect = new Section(s.getTitleSpan(),s.getTextSpan(),doc);
			for (Section os: outSubs) {
				thSect.addSubsection(os);
			}
			out.add(thSect);
		}
		return out;
	}
	

	
	/** 
	 * Processes a single file and returns its representation in internal XML format.
	 * 
	 * @param id			the id for the document
	 * @param corpusFile	the text file of the document
	 * @param annFilename  the annotation file
	 * 
	 * @return the XML representation of the document
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static Element processSingleFile(String id, String articleFile) throws IOException {
		Document articleDoc = null;
		Element articleXml = null;
		try {
			articleDoc = parseArticle(id,articleFile);
			articleXml = articleDoc.toXml();
		} catch (Exception e) {
			log.severe("Cannot parse " + id);
			e.printStackTrace();
		}
		return articleXml;
	}
	
		
	/**
	 * Processes a directory.
	 * 
	 * @param in	the input directory
	 * @param out  	the output directory
	 * @throws IOException if there is a problem with file reading/writing
	 */
	public static void processDirectory(String article, String out) throws IOException {
		File articleDir = new File(article);
		if (articleDir.isDirectory() == false) return;
		File outDir = new File(out);
		if (outDir.isDirectory() == false) return;
		int fileNum = 0;
		List<String> files = FileUtils.listFiles(article,false, "xml");

		for (String filename: files) {
			String id = filename.substring(filename.lastIndexOf(File.separator)+1).replace(".xml", "");
			log.log(Level.INFO,"Processing {0}: {1}.", new Object[]{id,++fileNum});
			String outFilename = outDir.getAbsolutePath() + File.separator + id + ".xml";
			PrintWriter pw = new PrintWriter(outFilename);
			try {
				Element docEl = processSingleFile(id, filename);
				nu.xom.Document xmlDoc = new nu.xom.Document(docEl);
			    Serializer serializer = new Serializer(new FileOutputStream(outFilename));
			    serializer.setIndent(4);
			    serializer.write(xmlDoc); 
			} catch (Exception e) {
				log.severe("ERROR PROCESSING FILE. SKIPPING.. " + id);
			}
			pw.flush();
			pw.close();
		}
	}
	
	/**
	 * Initializes CoreNLP and the sentence segmenter from properties.
	 * 
	 * @param props	the properties to use for initialization
	 * 
	 * @throws ClassNotFoundException	if the sentence segmenter class cannot be found
	 * @throws IllegalAccessException	if the sentence segmenter cannot be accessed
	 * @throws InstantiationException	if the sentence segmenter cannot be initializaed
	 */
	public static void init(Properties props) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		CoreNLPWrapper.getInstance(props);
		segmenter = ComponentLoader.getSentenceSegmenter(props);
	}
		 
	public static void main(String[] args) 
			throws IOException, InstantiationException, 
			IllegalAccessException, ClassNotFoundException, Exception {
		if (args.length < 2) {
			System.err.print("Usage: articleDirectory outputDirectory");
		}
		String articleIn = args[0];
		String out = args[1];
		File articleDir = new File(articleIn);
		if (articleDir.isDirectory() == false) {
			System.err.println("First argument is required to be an input directory:" + articleIn);
			System.exit(1);
		}
		File outDir = new File(out);
		if (outDir.isDirectory() == false) {
			System.err.println("The directory " + outDir + " doesn't exist. Creating a new directory..");
			outDir.mkdir();
		}
		// add processing properties
		Properties props = new Properties();
		props.put("sentenceSegmenter","gov.nih.nlm.pmc.PMCSentenceSegmenter");
//		props.put("annotators","tokenize,ssplit,pos,lemma,parse");
		props.put("annotators","tokenize,ssplit,pos,lemma");	
		props.put("tokenize.options","invertible=true");
		props.put("ssplit.isOneSentence","true");
		init(props);
		processDirectory(articleIn,out);
	}
}
