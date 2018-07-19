package gov.nih.nlm.pmc;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParserFactory;


/**
 * A class that uses E-utils facility to retrieve PMC articles.
 * 
 * @author  Halil Kilicoglu
 */
public class PMCSearch {
	private static Logger log = Logger.getLogger(PMCSearch.class.getName());
		
//	private final static String searchURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed";
	private final static String fetchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pmc&id=";
	private final static String citedURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pmc&linkname=pmc_refs_pubmed&id=";
	private final static String tooltag = "&tool=chqa&email=kilicogluh@mail.nih.gov";
	
/*	private static final Pattern[] pmidInTextPatterns = new Pattern[] {
		Pattern.compile("http://.+nih\\.gov/pubmed/(\\?term=)?([1-9][0-9]{5,9})([^\\s]+)*",Pattern.CASE_INSENSITIVE), 
		Pattern.compile("(PMID)s*\\s*[:\\-\\./]?\\s*\\b([1-9][0-9]{5,9})\\b([0-9,;\\s]+)*\\b", Pattern.CASE_INSENSITIVE),
		Pattern.compile("([a-zA-Z]+)[0-9/\\.:\\-]+\\b([1-9][0-9]{7})\\b([0-9,;\\s]+)*\\b", Pattern.CASE_INSENSITIVE),
		Pattern.compile("(article|pubmed id|pubmed identifier|pubmed|pmid|citation|paper|publication|reference).{0,5}\\b([1-9][0-9]{5,9})\\b([0-9,;\\s]+)*\\b", Pattern.CASE_INSENSITIVE),
		Pattern.compile("(.+?)([1-9][0-9]{5,9})\\s*PMID", Pattern.CASE_INSENSITIVE),
	};
	
	private static final Pattern titlePattern = Pattern.compile("TITLE\\s*:?\\s*(.+?)[\\.|\\\n|$]", Pattern.CASE_INSENSITIVE);
	private static final Pattern authorPattern = Pattern.compile("(([\\p{L}\\-' ]+ )([\\p{Lu}\\-]{1,3})[,|\\.|\\\n|$])+");
	private final static Pattern pmidPattern = Pattern.compile("<Id>(.*?)</Id>");
	private final static Pattern totalPattern = Pattern.compile("<Count>(\\d+)</Count>");
	
	private final static Pattern blankLinePattern = Pattern.compile("(\\r?\\n){2,}", Pattern.MULTILINE);*/
	
	// PubMed stopwords
/*	private final static List<String> STOPWORDS = Arrays.asList(
			"a", "about", "again", "all", "almost", "also", "although", "always", "among", "an", "and", 
			"another", "any", "are", "as", "at", "be", "because", "been", "before", "being", "between", 
			"both", "but", "by", "can", "could", "did", "do", "does", "done", "due", "during", "each", 
			"either", "enough", "especially", "etc", "for", "found", "from", "further", "had", "has", 
			"have", "having", "here", "how", "however", "i", "if", "in", "into", "is", "it", "its", "itself",
			"just", "kg", "km", "made", "mainly", "make", "may", "mg", "might", "ml", "mm", "most", "mostly", 
			"must", "nearly", "neither", "no", "nor", "obtained", "of", "often", "on", "our", "overall", 
			"perhaps", "pmid", "quite", "rather", "really", "regarding", "seem", "seen", "several", "should", 
			"show", "showed", "shown", "shows", "significantly", "since", "so", "some", "such", "than", "that", 
			"the", "their", "theirs", "them", "then", "there", "therefore", "these", "they", "this", "those", 
			"through", "thus", "to", "upon", "use", "used", "using", "various", "very", "was", "we", "were", 
			"what", "when", "which", "while", "with", "within", "without", "would"
	); */
	
/*	private static List<String> removePubMedStopwords(String in) {
		List<String> tokens = Arrays.asList(in.split("[ ]+"));
		List<String> outTokens = new ArrayList<String>();
		for (int i=0; i< tokens.size(); i++) {
			String token = tokens.get(i).toLowerCase();
			if (!(STOPWORDS.contains(token))) outTokens.add(token);
		}
		return outTokens;
	}*/

/*	private static String formQueryTerm(String text) {
		String query = "";
		query = extractPMIDSearch(text);
		if (query.equals("")) {
			query = extractAuthorSearch(text);
			if (query.equals("")) {
				query = extractTitleSearch(text);
			}
		} 
		return query.trim();
	}*/
	
/*	public static int runSearch(String text, List<String> lPMIDs) {
		log.warning("Text for PMC search: " + text);
		String query = formQueryTerm(text);
		log.debug("P query: " + query);
		int count  = -1;
		if (query.equals("")) return count;
		String search = searchURL + "&retmax=10" + "&term=" + query.replaceAll("\\s+", "%20") + tooltag;
		String line = "";
		log.debug("PubMed search string: " + search);
		try {
			URL url = new URL(search);
			BufferedReader in = new BufferedReader(new InputStreamReader(url
					.openStream(), "UTF-8"));
			while ((line = in.readLine()) != null) {
				System.out.println(line);
				Matcher m = totalPattern.matcher(line);
				if(count ==-1 && m.find()) {
					try{
					count = Integer.parseInt((m.group(1)));
					}catch(Exception ne){
						count = 0;
					}
				}
				m = pmidPattern.matcher(line);
				while (m.find()) 
					lPMIDs.add(m.group(1));
			}
		} catch (Exception e) {
			log.error("PubMed search error: " + e.getMessage());
			e.printStackTrace();
		}
		return count;
	}*/
	
	public static String parse(String uri) throws Exception {
		URL url = new URL(uri);
		//	System.out.println("in parser : "+ uri);
		URLConnection pc = url.openConnection();
		BufferedReader reader = new BufferedReader(
		           new InputStreamReader(pc.getInputStream(), "UTF-8")); 
		StringBuffer buf = new StringBuffer();
		String inputLine;
		while ((inputLine = reader.readLine()) != null) {
	            System.out.println(inputLine);
	            buf.append(inputLine);
	            buf.append(System.lineSeparator());
		}
		return buf.toString();

	}
	
	public static String runFetch(String pmcId) throws Exception {
		String fetch = fetchURL + pmcId;	
		log.warning("PubMed fetch string: " + fetch);
		String out=  parse(fetch);
		return out;
	}
	
	public static String runCited(String pmcId) throws Exception {
		String cited = citedURL + pmcId;	
		log.warning("PubMed cited string: " + cited);
		String out=  parse(cited);
		return out;
	}
	
}
