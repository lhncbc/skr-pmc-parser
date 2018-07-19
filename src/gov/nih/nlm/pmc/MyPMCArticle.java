package gov.nih.nlm.pmc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.uwm.pmcarticleparser.structuralelements.PMCArticleAbstract;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticleAuthor;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticleFigure;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticleFullText;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticlePublicationDate;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticleReference;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticleSentence;
import edu.uwm.pmcarticleparser.structuralelements.PMCArticleTable;
import generalutils.SentenceTokenizer;

/**
 * A representation of a PMC article and contains methods to parse it.  Based on Agarwal's work (PMCArticle class) and has some additional methods.
 *  
 * @author Shashank Agarwal
 * @author Halil Kilicoglu
 */
public class MyPMCArticle {
    public static final String TITLE_XPATH = "//title-group/article-title";
    public static final String JOURNAL_XPATH = "//journal-title";
    public static final String AUTHORS_XPATH = "//contrib-group/contrib[@contrib-type='author']";
//    public static final String AUTHORS_NAME_XPATH = "//contrib-group/contrib[@contrib-type='author']/name";
//    public static final String AUTHORS_ADDRESS_XPATH = "//contrib-group/contrib[@contrib-type='author']/address";
//    public static final String ABSTRACT_XPATH = "//abstract";
    public static final String ABSTRACT_XPATH = "//abstract[not(@abstract-type='toc' or @abstract-type='graphical' or @abstract-type='teaser' or @abstract-type='author-highlights' or @abstract-type='short')]";
    public static final String PUBLICATION_DATE_DAY_XPATH = "//pub-date/day";
    public static final String PUBLICATION_DATE_MONTH_XPATH = "//pub-date/month";
    public static final String PUBLICATION_DATE_YEAR_XPATH = "//pub-date/year";
    public static final String PMC_ID_XPATH = "//article-id[@pub-id-type='pmc']";
    public static final String PUBMED_ID_XPATH = "//article-id[@pub-id-type='pmid']";

    public static final String VOLUME_XPATH = "//volume";
    public static final String FIRST_PAGE_XPATH = "//fpage";
    public static final String LAST_PAGE_XPATH = "//lpage";
    public static final String REFERENCES_XPATH = "//ref-list/ref";
    public static final String FULL_TEXT_XPATH = "//body";
    public static final String FULL_TEXT_TEXT_XPATH = "//body";
    public static final String FIGURE_XPATH = "//fig";
    public static final String TABLE_XPATH = "//table-wrap";
    public static final String FORMULA_PATH = "//disp-formula";
    
    public static final String BACK_MATTER_XPATH = "//back/*[not(self::ref-list)]";
    public static final String FLOATS_GROUP_XPATH = "//floats-group";
    
    public static final String CIT_XPATH = "//xref[@ref-type='bibr']";

    public static final String NO_TITLE_DEFAULT = "No Title Found";
    public static final String NO_JOURNAL_NAME_DEFAULT = "No Journal Name Found";
    public static final String NO_DAY_DEFAULT = "No Publication Day Found";
    public static final String NO_MONTH_DEFAULT = "No Publication Month Found";
    public static final String NO_YEAR_DEFAULT = "No Publication Year Found";
    public static final String NO_PMC_ID_DEFAULT = "No PMC ID Found";
    public static final String NO_PUBMED_ID_DEFAULT = "No Pubmed ID Found";
    public static final String NO_VOLUME_DEFAULT = "No Volume Found";
    public static final String NO_FIRST_PAGE_DEFAULT = "No First Page Found";
    public static final String NO_LAST_PAGE_DEFAULT = "No Last Page Found";

    public static final String DEFAULT_ABSTRACT_SECTION = "";
    public static final String DEFAULT_FULL_TEXT_SECTION = "";
    public static final String DEFAULT_FULL_TEXT_SUBSECTION = "";
    public static final String DEFAULT_BACK_MATTER_SECTION = "";
    public static final String DEFAULT_BACK_MATTER_SUBSECTION = "";

    public static final int INDEX_FROM = 0; // Eg. Array index starts from 0.
    public static String citationReplacement = "citation";
    public static final Pattern CITATION_PATTERN = 
    		Pattern.compile("<xref +?ref-type=\"bibr\".*?>.*?</xref>");
    


    private Document document;
    private int abstractSentenceIndex;
    private int fullTextSentenceIndex;
    

    /**
     * Constructs an instance of PMCArticle.
     * @param articleLocation the location of the article xml file
     */
    public MyPMCArticle(String articleLocation) {
        try {
            DOMParser parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parser.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE, false);
            parser.parse(articleLocation);
            document = parser.getDocument();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Constructs an instance of PMCArticle with the pmc id of the article.
     * This requires accessing the article at the PMC server, hence by using
     * this constructor, you are allowing this program to connect to the
     * internet.
     * @param pmcId the PMC id of the article
     * @param timeToWaitBeforeConnecting the time to wait in ms before
     * connecting to PMC server. If you are accessing a single article, 0 will
     * be fine. If you are doing a batch job, then read the guidelines at -
     * http://www.pubmedcentral.nih.gov/about/oai.html (accessed: 07/18/2009)
     * for high-volume retrievals and set time to wait in milli-seconds
     * accordingly.
     */
    public MyPMCArticle(String pmcId, int timeToWaitBeforeConnecting) {
        try {
            Thread.sleep(timeToWaitBeforeConnecting);
            DOMParser parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parser.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE, false);
//            parser.parse("http://www.pubmedcentral.nih.gov/oai/oai.cgi?verb=GetRecord&metadataPrefix=pmc&identifier=oai:pubmedcentral.nih.gov:" + pmcId);
            parser.parse("https://www.ncbi.nlm.nih.gov/pmc/oai/oai.cgi?verb=GetRecord&identifier=oai:pubmedcentral.nih.gov:" + pmcId + "&metadataPrefix=pmc");
            document = parser.getDocument();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * The default constructor is not available
     */
    private MyPMCArticle() {
    }
    
    public Document getDocument() {
    	return document;
    }

    /**
     * Gets the title of the article. 
     * @return the title of the article
     */
    public String getTitle() {
        String title = commonElementParser(TITLE_XPATH, NO_TITLE_DEFAULT);
        return title;
    }

    /**
     * Gets the pubmed central id of the article
     * @return the pubmed central id of the article
     */
    public String getPmcId() {
        return commonElementParser(PMC_ID_XPATH, NO_PMC_ID_DEFAULT);
    }

    /**
     * Gets the pubmed id of the article
     * @return pubmed id of the article
     */
    public String getPubmedId() {
        return commonElementParser(PUBMED_ID_XPATH, NO_PUBMED_ID_DEFAULT);
    }

    /**
     * Gets the journal name of the article
     * @return the name of the journal
     */
    public String getJournal() {
        return commonElementParser(JOURNAL_XPATH, NO_JOURNAL_NAME_DEFAULT);
    }

    /**
     * Gets the journal volume of the article
     * @return volume of the journal in which the article was published.
     */
    public String getVolume() {
        return commonElementParser(VOLUME_XPATH, NO_VOLUME_DEFAULT);
    }

    /**
     * Gets the number of the first page of the article in the journal
     * @return the first page number
     */
    public String getFirstPage() {
        return commonElementParser(FIRST_PAGE_XPATH, NO_FIRST_PAGE_DEFAULT);
    }

    /**
     * Gets the number of the first page of the article in the journal
     * @return the last page number
     */
    public String getLastPage() {
        return commonElementParser(LAST_PAGE_XPATH, NO_LAST_PAGE_DEFAULT);
    }

    /**
     * Gets the publication date of the article
     * @return the PMCArticlePublicationDate object for this article
     */
    public PMCArticlePublicationDate getPublicationDate() {
        String day = commonElementParser(PUBLICATION_DATE_DAY_XPATH, NO_DAY_DEFAULT);
        String month = commonElementParser(PUBLICATION_DATE_MONTH_XPATH, NO_MONTH_DEFAULT);
        String year = commonElementParser(PUBLICATION_DATE_YEAR_XPATH, NO_YEAR_DEFAULT);
        return new PMCArticlePublicationDate(day, month, year);
    }

    /**
     * Gets a list of authors of this article
     * @return a list of PMCArticleAuthors
     */
    public List<PMCArticleAuthor> getAuthors() {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(AUTHORS_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            if (sectionNodes.getLength() == 0) {
                return new ArrayList<PMCArticleAuthor>();
            }

            List<PMCArticleAuthor> authorList = new ArrayList<PMCArticleAuthor>();
            Node node, childNode, grandChildNode;
            String firstName;
            String lastName;
            String email;
            NodeList childNodes;
            NodeList grandChildNodes;

            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                firstName = "";
                lastName = "";
                email = "";
                node = sectionNodes.item(i);
                childNodes = node.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); ++j) {
                    childNode = childNodes.item(j);
                    if ("name".equalsIgnoreCase(childNode.getNodeName())) {
                        grandChildNodes = childNode.getChildNodes();
                        for (int k = 0; k < grandChildNodes.getLength(); ++k) {
                            grandChildNode = grandChildNodes.item(k);
                            if ((grandChildNode.getNodeName() != null) && "given-names".equalsIgnoreCase(grandChildNode.getNodeName())) {
                                firstName = grandChildNode.getTextContent();
                            } else if ((grandChildNode.getNodeName() != null) && "surname".equalsIgnoreCase(grandChildNode.getNodeName())) {
                                lastName = grandChildNode.getTextContent();
                            }
                        }
                    }
                    if ("email".equalsIgnoreCase(childNode.getNodeName())) {
                        email = childNode.getTextContent();
                    }
                }

                PMCArticleAuthor author = new PMCArticleAuthor(firstName, lastName);
                author.setEmail(email);
                authorList.add(author);
            }
            return authorList;
        } catch (Exception ex) {
            return new ArrayList<PMCArticleAuthor>();
        }
    }

    /**
     * Gets a list of figures of the article
     * @return List of figures in this article
     */
    public List<PMCArticleFigure> getFigures() {
        List<PMCArticleFigure> figureList = new ArrayList<PMCArticleFigure>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(FIGURE_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                String label = "";
                String caption = "";
                String graphicLocation = "";
                Node node = sectionNodes.item(i);
                String id = getNodeId(node);
                NodeList nodeChildren = node.getChildNodes();
                for (int j = 0; j < nodeChildren.getLength(); ++j) {
                    Node childNode = nodeChildren.item(j);
                    if ((childNode.getNodeName() != null) && "caption".equalsIgnoreCase(childNode.getNodeName())) {
                        caption = childNode.getTextContent();
                    } else if ((childNode.getNodeName() != null) && "label".equalsIgnoreCase(childNode.getNodeName())) {
                        label = childNode.getTextContent();
                    } else if ((childNode.getNodeName() != null) && "graphic".equalsIgnoreCase(childNode.getNodeName())) {
                        NamedNodeMap graphicNodeAttributes = childNode.getAttributes();
                        Node nodeGraphicLocation = graphicNodeAttributes.getNamedItem("xlink:href");
                        if (nodeGraphicLocation != null) {
                            graphicLocation = nodeGraphicLocation.getTextContent();
                        }
                    }
                }
                figureList.add(new PMCArticleFigure(id, label, caption, graphicLocation));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return figureList;
    }

    /**
     * Gets a list of tables in this article
     * @return the list of tables in this article. Does not get the actual 
     * content in the table
     */
    public List<PMCArticleTable> getTables() {
        List<PMCArticleTable> tableList = new ArrayList<PMCArticleTable>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(TABLE_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                String label = "";
                String caption = "";
                Node node = sectionNodes.item(i);
                String id = getNodeId(node);
                NodeList nodeChildren = node.getChildNodes();
                for (int j = 0; j < nodeChildren.getLength(); ++j) {
                    Node childNode = nodeChildren.item(j);
                    if ((childNode.getNodeName() != null) && "caption".equalsIgnoreCase(childNode.getNodeName())) {
                        caption = childNode.getTextContent();
                    } else if ((childNode.getNodeName() != null) && "label".equalsIgnoreCase(childNode.getNodeName())) {
                        label = childNode.getTextContent();
                    }
                }
                tableList.add(new PMCArticleTable(id, label, caption));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return tableList;
    }

    /**
     * Gets the text of the abstract.
     * @return the abstract text.
     */
    public String getAbstractText() {
        return commonElementParserClean(ABSTRACT_XPATH, DEFAULT_ABSTRACT_SECTION);
    }

    /**
     * Gets the text from the full text of the article. No distiction is made
     * between section and subsection headings, or figures and tables within
     * the article.
     * @return the text from the full text of the article
     */
    public String getFullTextText() {
        return commonElementParserClean(FULL_TEXT_TEXT_XPATH, DEFAULT_FULL_TEXT_SECTION);
    }
    
    /**
     * Gets the text from the full text of the article. No distiction is made
     * between section and subsection headings, or figures and tables within
     * the article.
     * @return the text from the full text of the article
     */
    public String getBackMatterText() {
        return commonElementParserClean(BACK_MATTER_XPATH, DEFAULT_BACK_MATTER_SECTION);
    }

    /**
     * Gets the PMCArticleAbstract object of this article. Individual sentences
     * can be extracted using this format.
     * @return the abstract object
     */
    public PMCArticleAbstract getAbstract() {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(ABSTRACT_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            abstractSentenceIndex = INDEX_FROM;
            PMCArticleAbstract articleAbstract = new PMCArticleAbstract();
            // Use only the first node because subsequent "abstract" nodes might be editor's summary
            if (sectionNodes.getLength() != 0) {
                getAbstractHelper(articleAbstract, sectionNodes.item(0), DEFAULT_ABSTRACT_SECTION);
            }
            return articleAbstract;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new PMCArticleAbstract();
        }
    }

    /**
     * Returns the full text object of this article. A list of sentences can be
     * obtained from this object, which has information about the sections, 
     * subsections, refering references, figures and tables.
     * @return the full text of the article
     */
    public PMCArticleFullText getFullText() {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(FULL_TEXT_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            fullTextSentenceIndex = INDEX_FROM;
            PMCArticleFullText articleFullText = new PMCArticleFullText();
            getFullTextHelper(articleFullText, sectionNodes, DEFAULT_FULL_TEXT_SECTION, DEFAULT_FULL_TEXT_SUBSECTION);
            return articleFullText;
        } catch (Exception ex) {
            ex.printStackTrace();
            return new PMCArticleFullText();
        }
    }

    /**
     * Gets the list of references cited by the article
     * @return list of references in the article
     */
    public List<PMCArticleReference> getReferences() {
        List<PMCArticleReference> references = new ArrayList<PMCArticleReference>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(REFERENCES_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                String id = "";
                Node node = sectionNodes.item(i);
                NamedNodeMap refAttributes = node.getAttributes();
                Node idNode = refAttributes.getNamedItem("id");
                if (idNode != null) {
                    id = idNode.getTextContent();
                }
                references.add(new PMCArticleReference(id, node.getTextContent()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return references;
    }
    
    public List<Reference> getMyReferences() {
        List<Reference> references = new ArrayList<Reference>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(REFERENCES_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                String id = "";
                Node node = sectionNodes.item(i);
                NamedNodeMap refAttributes = node.getAttributes();
                Node idNode = refAttributes.getNamedItem("id");
                if (idNode != null) {
                    id = idNode.getTextContent();
                }
                NodeList children = node.getChildNodes();
                for (int j=0; j < children.getLength(); j++) {
                	Node child = children.item(j);
                	if (child.getNodeName().equals("citation") || child.getNodeName().equals("element-citation"))                 
                		references.add(new Reference(child));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return references;
    }
    
    public List<Citation> getMyCitations() {
        List<Citation> citations = new ArrayList<Citation>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(REFERENCES_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                Node node = sectionNodes.item(i);
                Citation cit = new Citation(node,this);
                citations.add(cit);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return citations;
    }
    
    public List<CitationMention> getMyCitationMentions(List<Citation> citations) {
        List<CitationMention> mentions = new ArrayList<CitationMention>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(REFERENCES_XPATH);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < sectionNodes.getLength(); ++i) {
                String id = "";
                Node node = sectionNodes.item(i);
                Citation cit = new Citation(node,this);
                
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return mentions;
    }

    /**
     * Private recursive function to get abstract text.
     * @param articleAbstract the article's abstract object
     * @param nodes
     * @param section
     */
    private void getAbstractHelper(PMCArticleAbstract articleAbstract, Node node, String section) {
        int indexInParagraph;
        String[] sentences;

        if ((node.getNodeName() != null) && "p".equalsIgnoreCase(node.getNodeName())) {
            indexInParagraph = INDEX_FROM;
            String paragraphText = getTextHelper(node.getChildNodes()).toString();
            sentences = SentenceTokenizer.getSentences(paragraphText);
            int ind = 0;
            for (String sentence : sentences) {
            	String fixed = fixSentence(paragraphText.substring(ind),sentence);
                articleAbstract.addSentence(postProcessSentence(fixed, indexInParagraph, sentences.length, abstractSentenceIndex, section, ""));
                ++abstractSentenceIndex;
                ++indexInParagraph;
                ind += fixed.length();
            }
        } else if ((node.getNodeName() != null) && "sec".equalsIgnoreCase(node.getNodeName())) {
            NodeList children = node.getChildNodes();
            int numChildren = children.getLength();
            for (int i = 0; i < numChildren; ++i) {
                getAbstractHelper(articleAbstract, children.item(i), section);
            }
        } else if ((node.getNodeName() != null) && "title".equalsIgnoreCase(node.getNodeName())) {
            section = node.getTextContent();
        } else if ((node.getNodeName() != null) && "abstract".equalsIgnoreCase(node.getNodeName())) {
            NodeList children = node.getChildNodes();
            int numChildren = children.getLength();
            for (int i = 0; i < numChildren; ++i) {
                getAbstractHelper(articleAbstract, children.item(i), section);
            }
        }
    }
    
    
    private String fixSentence(String paragraph, String sentence) {
		int beg = paragraph.indexOf(sentence);
		int end= 0;
		int parInd = 0;
		if (beg == -1) {
			Pattern p = Pattern.compile("\\w+");
			Matcher pm = p.matcher(sentence);
			int sentend=0;
			while (pm.find()) {
				String match = sentence.substring(pm.start(),pm.end());
				sentend = pm.end();
				int matchInd = paragraph.indexOf(match,parInd);
				if (matchInd == -1) {
				} else {
					if (beg == -1) beg = matchInd;
					end = matchInd + match.length();
					parInd = end;
				}
			}
			if (sentend < sentence.length()) {
				end += sentence.length() - sentend;
				parInd = end;
			}	
		} else {
			end = beg + sentence.length();
			parInd = end;
		}
		return paragraph.substring(beg, parInd);
    }

    private PMCArticleSentence postProcessSentence(String s, int indexInParagraph,
            int totalSentencesInParagraph, int indexInDocument,
            String section, String subsection) {
        String text = s.replaceAll("</?xref.*?>", "");
        Matcher m = CITATION_PATTERN.matcher(s);
        int ind =0;
        StringBuffer repBuf = new StringBuffer();
        while (m.find()) {
        	int beg = m.start();
        	int end = m.end();
        	String sub = s.substring(beg,end);
        	int startInd = sub.indexOf("rid")+5;
        	String citid = sub.substring(startInd, sub.indexOf("\"",startInd));
        	int startInd2 = sub.indexOf(">")+1;
        	String refid = sub.substring(startInd2,sub.indexOf("<",startInd2));
        	Pattern pt = Pattern.compile("^([0-9]+)\\p{Pd}+([0-9]+)");
        	Matcher mpt = pt.matcher(refid);
        	if (mpt.find()) {
        		refid = mpt.group(1);
        	}
        	repBuf.append(s.substring(ind, beg));
        	repBuf.append(citationReplacement + "(" + refid +"!" + citid + ")");
        	ind = end;
        }
        repBuf.append(s.substring(ind,s.length()));
        String citationReplacedText = repBuf.toString();
//        String citationReplacedText = s.replaceAll("<xref +?ref-type=\"bibr\".*?>.*?</xref>", citationReplacement);
        citationReplacedText = citationReplacedText.replaceAll("</?xref.*?>", "");
        s = s.replaceAll("</xref>", "");

        PMCArticleSentence sentence = new PMCArticleSentence(text);
        sentence.setCitationReplacedText(citationReplacedText);
        sentence.setInParagraphIndex(indexInParagraph);
        sentence.setTotalSentencesInContainingParagraph(totalSentencesInParagraph);
        sentence.setIndexInDocument(indexInDocument);
        sentence.setSectionName(section);
        sentence.setSubSectionName(subsection);

        Pattern xrefPattern = Pattern.compile("<xref ref-type=\"([^\"]+)\" rid=\"([^\"]+)\">");
        Matcher xrefMatcher = xrefPattern.matcher(s);
        while (xrefMatcher.find()) {
            if ("fig".equalsIgnoreCase(xrefMatcher.group(1))) {
                sentence.addReferedFigureId(xrefMatcher.group(2));
            } else if ("table".equalsIgnoreCase(xrefMatcher.group(1))) {
                sentence.addReferedTableId(xrefMatcher.group(2));
            } else if ("bibr".equalsIgnoreCase(xrefMatcher.group(1))) {
                sentence.addReferedCitationId(xrefMatcher.group(2));
            }
        }
        return sentence;
    }
    
    public String replaceCitation(String s) {
       return  s.replaceAll("</?xref.*?>", "").replaceAll("</xref>", "");
    }

    private StringBuffer getTextHelper(NodeList nodes) {
        StringBuffer text = new StringBuffer();
        Node node;
        for (int i = 0; i < nodes.getLength(); ++i) {
            node = nodes.item(i);
            if ("#text".equalsIgnoreCase(node.getNodeName())) {
                text.append(node.getTextContent());
            } else if ((node.getNodeName() != null) && ("xref".equalsIgnoreCase(node.getNodeName()))) {
                NamedNodeMap nodeAttributes = node.getAttributes();
                String refType, rid;
                try {
                    refType = nodeAttributes.getNamedItem("ref-type").getTextContent();
                } catch (Exception ex) {
                    refType = "";
                }
                try {
                    rid = nodeAttributes.getNamedItem("rid").getTextContent();
                } catch (Exception ex) {
                    rid = "";
                }
                text.append("<xref ref-type=\"").append(refType).append("\" rid=\"").append(rid).append("\">").append(node.getTextContent()).append("</xref>");
            } else if ((node.getNodeName() != null) && ("fig".equalsIgnoreCase(node.getNodeName()))) {
            	text.append(getCaption(node));
            } else if ((node.getNodeName() != null) && ("table-wrap".equalsIgnoreCase(node.getNodeName()))) {
            	text.append(getCaption(node));
            } else if ((node.getNodeName() != null) && ("disp-formula".equalsIgnoreCase(node.getNodeName()))) {
            	System.err.println("Formula... skipping..");
                //Ignore
            } else if ((node.getNodeName() != null) && ("inline-formula".equalsIgnoreCase(node.getNodeName()))) {
            	System.err.println("Inline formula... skipping..");
                //Ignore
            } else {
                text.append(getTextHelper(node.getChildNodes()));
            }
        }
        return text;
    }
    
    public StringBuffer getTextHelper(Node node) {
        StringBuffer text = new StringBuffer();
 //       System.out.println("NODE " + node.getNodeName());
        if ("#text".equalsIgnoreCase(node.getNodeName())) {
        	text.append(node.getTextContent());
        } else if ((node.getNodeName() != null) && ("xref".equalsIgnoreCase(node.getNodeName()))) {
        	NamedNodeMap nodeAttributes = node.getAttributes();
        	String refType, rid;
        	try {
        		refType = nodeAttributes.getNamedItem("ref-type").getTextContent();
        	} catch (Exception ex) {
        		refType = "";
            }
            try {
                rid = nodeAttributes.getNamedItem("rid").getTextContent();
            } catch (Exception ex) {
                rid = "";
            }
 //           text.append("<xref ref-type=\"").append(refType).append("\" rid=\"").append(rid).append("\">").append(node.getTextContent()).append("</xref>");
            text.append(node.getTextContent());
        } else if ((node.getNodeName() != null) && ("fig".equalsIgnoreCase(node.getNodeName()))) {
        	text.append(getCaption(node));
        } else if ((node.getNodeName() != null) && ("table-wrap".equalsIgnoreCase(node.getNodeName()))) {
        	text.append(getCaption(node));
        } else if ((node.getNodeName() != null) && ("disp-formula".equalsIgnoreCase(node.getNodeName()))) {
        	System.err.println("Formula... skipping..");
            //Ignore
        } else if ((node.getNodeName() != null) && ("inline-formula".equalsIgnoreCase(node.getNodeName()))) {
        	System.err.println("Inline formula... skipping..");
            //Ignore
        } else {
        	for (int i = 0; i < node.getChildNodes().getLength(); ++i) {
                Node n = node.getChildNodes().item(i);
                StringBuffer subbuf = getTextHelper(n);
                // not sure about this but meant to remove newline characters in a paragraph for better sentence splitting
                if ("p".equalsIgnoreCase(n.getNodeName())) {
                	String t = subbuf.toString().replaceAll("(?m)[ \t]*\r?\n", " ");
                	text.append(t);
                } else {
                	text.append(subbuf);
                }
        	}
        }
        return text;
    }

    private void getFullTextHelper(PMCArticleFullText articleFullText, NodeList nodes, String section, String subSection) {
        Node node;
        int indexInParagraph;
        String[] sentences;

        for (int i = 0; i < nodes.getLength(); ++i) {
            node = nodes.item(i);
            if ((node.getNodeName() != null) && "p".equalsIgnoreCase(node.getNodeName())) {
                indexInParagraph = INDEX_FROM;
                String paragraphText = getTextHelper(node.getChildNodes()).toString();
//                paragraphText = paragraphText.replaceAll("\\^", "");
                sentences = SentenceTokenizer.getSentences(paragraphText);
//                sentences = fixSentences(paragraphText,sentencesX);
        		int ind = 0;
                for (String sentence : sentences) {
                	String fixed = fixSentence(paragraphText.substring(ind),sentence);
                    articleFullText.addSentence(postProcessSentence(fixed, indexInParagraph, sentences.length, fullTextSentenceIndex, section, subSection));
                    ++fullTextSentenceIndex;
                    ++indexInParagraph;
                    ind += fixed.length();
                }
            } else if ((node.getNodeName() != null) && "sec".equalsIgnoreCase(node.getNodeName())) {
                getFullTextHelper(articleFullText, node.getChildNodes(), section, subSection);
            } else if ((node.getNodeName() != null) && "body".equalsIgnoreCase(node.getNodeName())) {
                getFullTextHelper(articleFullText, node.getChildNodes(), section, subSection);
            } else if ((node.getNodeName() != null) && "title".equalsIgnoreCase(node.getNodeName())) {
                if (DEFAULT_FULL_TEXT_SECTION.equalsIgnoreCase(section)) {
                    section = node.getTextContent();
                } else if (DEFAULT_FULL_TEXT_SUBSECTION.equalsIgnoreCase(subSection)) {
                    subSection = node.getTextContent();
                }
            } else if ((node.getNodeName() != null) && "fig".equalsIgnoreCase(node.getNodeName())) {
                indexInParagraph = INDEX_FROM;
                String figText = getCaption(node);
                sentences = SentenceTokenizer.getSentences(figText);
                int ind = 0;
                for (String sentence : sentences) {
                	String fixed = fixSentence(figText.substring(ind),sentence);
                	articleFullText.addSentence(postProcessSentence(fixed, indexInParagraph, sentences.length, fullTextSentenceIndex, section, subSection));
                 	++fullTextSentenceIndex;
                 	++indexInParagraph;
                 	ind += fixed.length();
                }
            } else if ((node.getNodeName() != null) && "table-wrap".equalsIgnoreCase(node.getNodeName())) {
                indexInParagraph = INDEX_FROM;
                String tableText = getCaption(node);
                sentences = SentenceTokenizer.getSentences(tableText);
                int ind = 0;
                for (String sentence : sentences) {
                	String fixed = fixSentence(tableText.substring(ind),sentence);
                	articleFullText.addSentence(postProcessSentence(fixed, indexInParagraph, sentences.length, fullTextSentenceIndex, section, subSection));
                 	++fullTextSentenceIndex;
                 	++indexInParagraph;
                 	ind += fixed.length();
                }
              
            } else if ((node.getNodeName() != null) && ("disp-formula".equalsIgnoreCase(node.getNodeName()))) {
            	System.err.println("Formula... skipping..");
                //Ignore
            } else if ((node.getNodeName() != null) && ("inline-formula".equalsIgnoreCase(node.getNodeName()))) {
            	System.err.println("Inline formula... skipping..");
                //Ignore
            } 
        }
    }
    
    	
    private String commonElementParser(String xPathString, String defaultText) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(xPathString);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            if (sectionNodes.getLength() == 0) {
                return defaultText;
            }
            return sectionNodes.item(0).getTextContent();
        } catch (Exception ex) {
            System.err.println("Exception");
            ex.printStackTrace();
            return defaultText;
        }
    }
    
    private String commonElementParserClean(String xPathString, String defaultText) {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(xPathString);
            NodeList sectionNodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            if (sectionNodes.getLength() == 0) {
                return defaultText;
            }
            StringBuffer buf = new StringBuffer();
            for (int i=0; i < sectionNodes.getLength(); i++) {
            	Node n = sectionNodes.item(i);
            	buf.append(getTextHelper(n).toString() + "\n");
            }
            return buf.toString();
        } catch (Exception ex) {
            System.err.println("Exception");
            ex.printStackTrace();
            return defaultText;
        }
    }

    private String getCaption(Node tableNode) {
        NodeList nodeChildren = tableNode.getChildNodes();
        String caption = "";
        String label ="";
        for (int j = 0; j < nodeChildren.getLength(); ++j) {
            Node childNode = nodeChildren.item(j);
            if ((childNode.getNodeName() != null) && "caption".equalsIgnoreCase(childNode.getNodeName())) {
                caption = childNode.getTextContent();
            } else if ((childNode.getNodeName() != null) && "label".equalsIgnoreCase(childNode.getNodeName())) {
                label = childNode.getTextContent();
            } 
        }
        return label.trim() + " " + caption.trim();
    }
    
    public Map<String,String> getFloatsGroupText() {
    	Map<String,String> nodesOfInterest = new HashMap<>();
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(FLOATS_GROUP_XPATH);
            NodeList nodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); ++i) {
            	Node n = nodes.item(i);
            	NodeList children = n.getChildNodes();
            	for (int j=0; j < children.getLength(); j++) {
            		Node c = children.item(j);
            		String id = getNodeId(c);
            		if (id.equals("") == false) {
            			if ((c.getNodeName() != null) && ("fig".equalsIgnoreCase(c.getNodeName()))) {
            				nodesOfInterest.put(id, getCaption(c));
                        } else if ((c.getNodeName() != null) && ("table-wrap".equalsIgnoreCase(c.getNodeName()))) {
                        	nodesOfInterest.put(id, getCaption(c));
                        } else {
                        	nodesOfInterest.put(id, getTextHelper(c.getChildNodes()).toString());
                        }
            		}
            	}
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nodesOfInterest;
    }
        
        
      public String getNodeId(Node node) {
          NamedNodeMap attributes = node.getAttributes();
          if (attributes == null) return "";
          Node nodeId = attributes.getNamedItem("id");
          if (nodeId != null) {
              return nodeId.getTextContent();
          }
          return "";
      }
    
    
    
//    public static void main(String args[]) {
//        PMCArticle pa = new PMCArticle("C:/Users/shashank/Documents/data/biocreative3/iat/2519078.nxml");
//        PMCArticleFullText ft = pa.getFullText();
//        List<PMCArticleSentence> sentences = ft.getFullTextSentences();
//        for (PMCArticleSentence sentence : sentences) {
//            if (sentence.getInParagraphIndex() == 0) {
//                System.out.println("");
//            }
//            System.out.print(" " + sentence.getText());
//        }
//
//        for (PMCArticleAuthor author : pa.getAuthors()) {
//            System.out.println(author.getFirstName() + " - " + author.getLastName() + " - " + author.getEmail());
//        }
//
//        PMCArticleAbstract abs = pa.getAbstract();
//        for (PMCArticleSentence s : abs.getAbstractSentences()) {
//            System.out.println(s.getText());
//        }
//
//        PMCArticleFullText f = pa.getFullText();
//        System.out.println("Number of sentences: " + f.getFullTextSentences().size());
//        int i = 0;
//        for (PMCArticleSentence s : f.getFullTextSentences()) {
//            System.out.println(i + ": " + s.getText());
//            ++i;
//            System.out.println(s.getInParagraphIndex() + "/" + s.getTotalSentencesInContainingParagraph());
//            System.out.println(s.getSectionName());
//            System.out.println(s.getSubSectionName());
//            if (s.isRefersCitation()) {
//                List<String> citations = s.getReferedCitationId();
//                for (String citation : citations) {
//                    System.out.println("  Citation ID: " + citation);
//                }
//            }
//            System.out.println("");
//        }

//        List<PMCArticleFigure> figs = pa.getFigures();
//        for (PMCArticleFigure fig : figs) {
//            System.out.println("ID: " + fig.getId());
//            System.out.println("Label: " + fig.getLabel());
//            System.out.println("Caption: " + fig.getCaption());
//            System.out.println("Graphic Location: " + fig.getGraphicLocation());
//            System.out.println("");
//        }
//        List<PMCArticleTable> tables = pa.getTables();
//        for (PMCArticleTable tab : tables) {
//            System.out.println("ID: " + tab.getId());
//            System.out.println("Label: " + tab.getLabel());
//            System.out.println("Caption: " + tab.getCaption());
//            System.out.println("");
//        }

//        List<PMCArticleReference> refs = pa.getReferences();
//        PMCArticleReference ref = refs.get(11);
//        String refID = ref.getId();
//
//        PMCArticleFullText paft = pa.getFullText();
//        for (Sentence sentence : paft.getFullTextSentences()) {
//            if (sentence.getReferedCitationId().contains(refID)) {
//                System.out.println(sentence.getText());
//            }
//        }

//        for (PMCArticleReference ref : refs) {
//            System.out.println(ref.getId());
//            System.out.println(ref.getText());
//            System.out.println();
//        }
//    }
}
