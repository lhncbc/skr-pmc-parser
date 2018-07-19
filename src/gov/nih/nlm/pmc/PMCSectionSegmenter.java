package gov.nih.nlm.pmc;


import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.nih.nlm.ling.core.Document;
import gov.nih.nlm.ling.core.Section;
import gov.nih.nlm.ling.core.Span;
import gov.nih.nlm.ling.process.SectionSegmenter;

/**
 * A class that uses section information in PMC XML to sectionize the document. 
 * 
 * TODO: Likely needs more work.
 * 
 * @author Halil Kilicoglu
 *
 */
public class PMCSectionSegmenter implements SectionSegmenter {
	
	public static final String TOP_XPATH = "//abstract[not(@abstract-type='toc' or @abstract-type='graphical' or @abstract-type='teaser' or @abstract-type='author-highlights' or @abstract-type='short')]|//body";
//    public static final String SECTION_XPATH = "//abstract|//body/sec";
 //   public static final String SUBSECTION_XPATH = "//body/sec/sec";
    public static final String CHILD_XPATH = ".//sec";
    public static final String ANCESTOR_XPATH = "ancestor::sec";
    
	private org.w3c.dom.Document xmlDoc;
	private MyPMCArticle article;

	public PMCSectionSegmenter(String filename) {
        try {
            DOMParser parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parser.setFeature(Constants.SAX_FEATURE_PREFIX + Constants.NAMESPACES_FEATURE, false);
            parser.parse(filename);
            xmlDoc = parser.getDocument();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
	
	public PMCSectionSegmenter(MyPMCArticle article) {
        try {
            this.article = article;
            xmlDoc = article.getDocument();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
    
	@Override
/*	public void segment(Document document) {	
		List<Section> sects = new ArrayList<>();
	    try {
	        XPathFactory xPathFactory = XPathFactory.newInstance();
	        XPath xPath = xPathFactory.newXPath();
	        XPathExpression expression = xPath.compile(SECTION_XPATH);
	        NodeList sectionNodes = (NodeList) expression.evaluate(xmlDoc, XPathConstants.NODESET);
	        XPathExpression subExpression = xPath.compile(SUBSECTION_XPATH);
	        NodeList subsectionNodes = (NodeList) subExpression.evaluate(xmlDoc, XPathConstants.NODESET);
	        for (int i=0; i < sectionNodes.getLength(); i++) {
	        	Node sn = sectionNodes.item(i);
	        	boolean found = false;
		        for (int j=0; j < subsectionNodes.getLength(); j++) {
		        	if (sn.equals(subsectionNodes.item(j))) {
		        		found = true;
		        		break;
		        	}
		        }
		        if (!found) {
		        	segment(sn,0,document,sects);
		        }
	        }
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }  
	    document.setSections(sects);
	}*/
	
/*	public void segment(Document document) {	
		List<Section> sects = new ArrayList<>();
	    try {
	        XPathFactory xPathFactory = XPathFactory.newInstance();
	        XPath xPath = xPathFactory.newXPath();
	        XPathExpression expression = xPath.compile(SECTION_XPATH);
	        NodeList sectionNodes = (NodeList) expression.evaluate(xmlDoc, XPathConstants.NODESET);
	        XPathExpression subExpression = xPath.compile(SUBSECTION_XPATH);
	        NodeList subsectionNodes = (NodeList) subExpression.evaluate(xmlDoc, XPathConstants.NODESET);
	        for (int i=0; i < sectionNodes.getLength(); i++) {
	        	Node sn = sectionNodes.item(i);
	        	boolean found = false;
		        for (int j=0; j < subsectionNodes.getLength(); j++) {
		        	if (sn.equals(subsectionNodes.item(j))) {
		        		found = true;
		        		break;
		        	}
		        }
		        if (!found) {
		        	segment(sn,0,document,sects);
		        }
	        }
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }  
	    document.setSections(sects);
	}*/
	public void segment(Document document) {	
		List<Section> sects = new ArrayList<>();
	    try {
	        XPathFactory xPathFactory = XPathFactory.newInstance();
	        XPath xPath = xPathFactory.newXPath();
	        XPathExpression expression = xPath.compile(TOP_XPATH);
	        NodeList topNodes = (NodeList) expression.evaluate(xmlDoc,XPathConstants.NODESET);
	        for (int i=0; i < topNodes.getLength(); i++) {
	        	Node t = topNodes.item(i);
	        	segment(t,0,document,sects);
	        }
/*	        NodeList sectionNodes = (NodeList) expression.evaluate(xmlDoc, XPathConstants.NODESET);
	        XPathExpression subExpression = xPath.compile(SUBSECTION_XPATH);
	        NodeList subsectionNodes = (NodeList) subExpression.evaluate(xmlDoc, XPathConstants.NODESET);
	        for (int i=0; i < sectionNodes.getLength(); i++) {
	        	Node sn = sectionNodes.item(i);
	        	boolean found = false;
		        for (int j=0; j < subsectionNodes.getLength(); j++) {
		        	if (sn.equals(subsectionNodes.item(j))) {
		        		found = true;
		        		break;
		        	}
		        }
		        if (!found) {
		        	segment(sn,0,document,sects);
		        }
	        }*/
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }  
	    document.setSections(sects);
	}
	
	
	private void segment(Node node, int index, Document document, List<Section> sect) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression expression = xPath.compile(CHILD_XPATH);
        NodeList sectionNodes = (NodeList) expression.evaluate(node, XPathConstants.NODESET);
        List<Section> allSubSects = new ArrayList<>();
     	for (int i=0; i < sectionNodes.getLength(); i++) {
        	Node sn = sectionNodes.item(i);
        	if (sn.equals(node)) continue;
        	if (containsAncestor(sectionNodes,sn)) continue;
    		String content = article.getTextHelper(sn).toString();
    		int nind = document.getText().indexOf(content, index);
    		if (nind == -1) return;
            List<Section> subsects = new ArrayList<>();
        	segment(sn,nind,document,subsects);
        	allSubSects.add(subsects.get(0));
        }
     	if (node.getNodeName().equals("body") && sectionNodes.getLength() > 0) {
     		sect.addAll(allSubSects);
     		return;
     	}
/*     	if (node.getNodeName().equals("abstract") && node.getTextContent().contains("Supplemental Digital Content is available in the text")) {
     		return;
     	}*/
		String title = null;
		String content = article.getTextHelper(node).toString();
		int nind = document.getText().indexOf(content, index);
		int tind = -1;
		if (node instanceof Element) {
			Element enode = (Element)node;
			if (enode.getElementsByTagName("title").getLength() == 0) {
				title = "";
				tind = nind;
			}
			else {
				title = enode.getElementsByTagName("title").item(0).getTextContent();
				tind = document.getText().indexOf(title,nind);
			}
		}
		Section th = null;
		if (tind == -1 || title.equals("")) {
			th = new Section(null,new Span(nind,nind+content.length()),document);
		} else {
			th = new Section(new Span(tind,tind+title.length()),new Span(nind,nind+content.length()),document);
		}
		th.setSubSections(allSubSects);
        sect.add(th);
	}
	
	private boolean containsAncestor(NodeList nl, Node n) throws Exception {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        XPathExpression expression = xPath.compile(ANCESTOR_XPATH);
        NodeList nodes = (NodeList) expression.evaluate(n, XPathConstants.NODESET);
        if (nodes.getLength() ==0) return false;
    	for (int i=0; i <nl.getLength(); i++) {
    		Node l = nl.item(i);
    		for (int j=0; j < nodes.getLength(); j++) {
    			Node k = nodes.item(j);
    			if (k.equals(l)) return true;
    		}
    	}
    	return false;
	}
	
}

