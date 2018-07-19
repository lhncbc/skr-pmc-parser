package gov.nih.nlm.pmc;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

public class Citation {

	private MyPMCArticle article;
	private String id; 
	private Reference reference;
	public Citation(String id, MyPMCArticle article, Reference reference) {
		this.id = id;
		this.article = article;
		this.reference = reference;
	}
	public Citation(Element el, MyPMCArticle article) {
		if (el.getLocalName().equals("ref") == false) 
			throw new IllegalArgumentException("Not a citation XML element.");
		this.id = el.getAttributeValue("id");
		Elements refEls = el.getChildElements("citation");
		if (refEls == null) refEls = el.getChildElements("element-citation");
		if (refEls == null) refEls = el.getChildElements("mixed-citation");
		Element refEl = refEls.get(0);
		this.reference = new Reference(refEl);
        if (article != null) this.article = article;
	}
	
	public Citation(Node el, MyPMCArticle article) {
		if (el.getNodeName().equals("ref") == false) 
			throw new IllegalArgumentException("Not a citation XML element.");
        NamedNodeMap refAttributes = el.getAttributes();
        Node idNode = refAttributes.getNamedItem("id");
        if (idNode != null) {
            this.id = idNode.getTextContent();
        }
        NodeList children = el.getChildNodes();
        for (int j=0; j < children.getLength(); j++) {
        	Node child = children.item(j);
        	if (child.getNodeName().equals("citation") || child.getNodeName().equals("element-citation") ||
        			child.getNodeName().equals("mixed-citation"))  {               
        		 this.reference = new Reference(child);
        	}
        }
        if (article != null) this.article = article;
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public MyPMCArticle getArticle() {
		return article;
	}
	public void setArticle(MyPMCArticle article) {
		this.article = article;
	}
	public Reference getReference() {
		return reference;
	}
	public void setReference(Reference reference) {
		this.reference = reference;
	}
	
	public String getType() {
		return reference.getType();
	}

	public boolean referenceEquals(Object obj) {
		if (obj instanceof Citation == false) return false;
		Citation cit = (Citation)obj;
		return (cit.getReference().equals(reference));
	}
	
	@Override
	public int hashCode() {
		return 
	    ((id == null ? 89 : id.hashCode()) ^
	     (article  == null ? 97 : article.hashCode()) ^
	     (reference == null ? 103: reference.hashCode()));
	}
	
	/**
	 * Equality on the basis of type and mention equality.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (this == obj) return true;
		if (getClass() != obj.getClass()) return false;
		Citation at = (Citation)obj;
		return (id.equals(at.getId()) &&
				article.equals(at.getArticle()) &&
				reference.equals(at.getReference()));
	}
	
	public Element toXml() {
		Element el = new Element("Citation");
		el.addAttribute(new Attribute("id",id));
		if (reference != null) 
			el.appendChild(reference.toXml());
		return el;
	}

}
