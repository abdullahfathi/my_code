package spud.searching;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.special.Gamma;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.SPUDLMSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

import spud.logging.LoggerInitializer;
import spud.searching.Evaluator.RankedList;
import spud.utils.Utils;

/**
 *
 * @author ronanc
 */
public class QuerySearch {
    
    private final static Logger logger = Logger.getLogger( QuerySearch.class.getName());
 
    public static final int title = 1;
    public static final int title_desc = 2;
    public static final int title_desc_narr = 3;
    
    public static final int desc = 4;
    public static final int narr = 5;
    public static int query_type = 1;
    public static void main(String[] args) throws ParseException, IOException{
        
        LoggerInitializer.setup();
        
        if (args.length >2){
            
            //set this to true to do the esimation of the DCM background parameter
            //else it will be set to the average document length
            SPUDLMSimilarity.b0est = true;

            // Open the lucene index dir in args[0]
            QuerySearch i = new QuerySearch(args[0]);
            
            i.loadTopics(args[1]);
            i.loadQrels(args[2]);
            
            //set to run title queries
            i.setQuerySet(QuerySearch.title);
            //run the set of queries
            i.runQuerySet();
            
            
            
            
            
        }else{
            logger.info("QueryIndex (index) (topics_file) (qrels_file) \n\n"
                    + "\t\"index\" is the lucene index directory\n"
                    + "\t\"topics_file\" is the trec topics file\n"
                    + "\t\"qrels\" is the qrels file (binary relevance is assumed)\n"
                    + "\n\n\tNote: The estimate of the background model is calculated each time. "
                    + "\n\t      It could be stored in the index once its calculated to save time.\n"
                    + "\t      Also note that the query effectiveness metrics are written to stdout\n"
                    + "\t      so they can be redirected to a file for analysis.");
        }
    }
    private final IndexSearcher searcher;

    private final IndexReader reader;
    private final Analyzer analyzer;
    private final HashMap<String, String[]> title_queries;
    private final HashMap<String, String[]> desc_queries;
    private final HashMap<String, String[]> narr_queries;
    
    private final HashMap<String, String[]> title_desc_queries;
    private final HashMap<String, String[]> title_desc_narr_queries;
    private final TreeMap<String,Double> AP_values;
    
    
    private final TreeMap<String,Double> NDCG10_values;
    
    private final TreeMap<String,Double> NDCG20_values;
    
    private Evaluator eval;
    
    private HashMap<String, String[]> current_set;
    
    private int max_iterations = 10;
    
    
    public QuerySearch(String location) throws IOException{

        
        Path p = Paths.get(location);
        
        
        Directory dir = new NIOFSDirectory(p);
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        analyzer = new EnglishAnalyzer();
        
        title_queries = new HashMap();
        desc_queries = new HashMap();
        narr_queries = new HashMap();        
        title_desc_queries = new HashMap();
        title_desc_narr_queries = new HashMap();
        
        AP_values = new TreeMap<>();
        NDCG10_values = new TreeMap<>();
        NDCG20_values = new TreeMap<>();
               
    }
    
    
    
    /**
     * 
     * This is called if SPUDLMSimilarity.b0Set is false
     * Estimates the background DCM mass
     * @return
     * @throws IOException 
     */
    
    public double estimateB0() throws IOException {

        
        // now estimate mass of background model (DCM)
        //
        logger.info("estimate background DCM mass...");
        double denom;
        double s = 250;
        for (int i = 0; i < max_iterations; i++) {
            logger.log(Level.INFO, "iteration " + i + " estimated mu value is " + s );
            denom = 0;
            
            for (int j = 0; j < reader.maxDoc(); j++) {
                
                Document doc = reader.document(j);
                
                String[] str_dl =  doc.getValues("TotalTerms");
                //logger.info(str_dl[0] + "");
                Double dl = Double.parseDouble(str_dl[0]);
                denom += Gamma.digamma(s + dl);
            }
            
            
            denom =  (denom - (reader.getDocCount("text") * Gamma.digamma(s)));
            
            s = reader.getSumDocFreq("text")/denom;

        }        
        logger.info("done.");
        
        
        
        return s;
    }
    
    /**
     * prels is not implemented here. 
     * Just use qrels
     * @param _qrels
     * @throws IOException 
     */
    public void loadQrels(String _qrels) throws IOException{
        eval = new Evaluator(_qrels);
    }

    
    
    /**
     * 
     * This just loads queries, for iterating through later
     * 
     * @param fname
     * @throws FileNotFoundException
     * @throws IOException 
     */
    
    public void loadTopics(String fname) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(fname));
        String line;

        StringBuilder sb = new StringBuilder();
        StringBuilder sb_desc = new StringBuilder();
        StringBuilder sb_narr = new StringBuilder();
        StringBuilder sb_desc_only = new StringBuilder();
        StringBuilder sb_desc_narr = new StringBuilder();
        String[] toks;
        String cur_topic_num = null;
        while ((line = br.readLine()) != null) {

            if (line.startsWith("<num>")) {

                //add to hash
                if (cur_topic_num != null) {
                    title_queries.put(cur_topic_num, sb.toString().split(" "));
                    title_desc_queries.put(cur_topic_num, sb_desc.toString().split(" "));
                    desc_queries.put(cur_topic_num, sb_desc_only.toString().split(" "));
                    title_desc_narr_queries.put(cur_topic_num, sb_desc_narr.toString().split(" "));
                    narr_queries.put(cur_topic_num, sb_desc_narr.toString().split(" "));
                }
                cur_topic_num = line.split(" ")[2];
                //logger.log(Level.INFO, cur_topic_num);
                sb = new StringBuilder();
                sb_desc = new StringBuilder();
                sb_desc_only = new StringBuilder();
                sb_desc_narr = new StringBuilder();
                sb_narr = new StringBuilder();
            }

            //titles
            if (line.startsWith("<title>")) {
                toks = line.split(" ");
                for (int i = 1; i < toks.length; i++) {
                    String word = Utils.tidyWord(toks[i]);

                    if ((word == null) || (word.length() < 2) || (word.equals("topic"))) {
                        continue;
                    }
                    sb.append(word).append(" ");
                    sb_desc.append(word).append(" ");
                    sb_desc_narr.append(word).append(" ");
                }

                while (!(line = br.readLine()).startsWith("<desc>")) {
                    toks = line.split(" ");
                    for (int i = 0; i < toks.length; i++) {
                        String word = Utils.tidyWord(toks[i]);
                        if (word == null || (word.length() < 2)) {
                            continue;
                        }
                        sb.append(word).append(" ");
                        sb_desc.append(word).append(" ");
                        sb_desc_narr.append(word).append(" ");
                    }
                }
            }

            //descs
            if (line.startsWith("<desc>")) {
                toks = line.split(" ");
                for (int i = 1; i < toks.length; i++) {

                    if (toks[i].equals("Description:")) {
                        continue;
                    }

                    String word = Utils.tidyWord(toks[i]);
                    if (word == null || (word.length() < 2)) {
                        continue;
                    }
                    sb_desc.append(word).append(" ");
                    sb_desc_only.append(word).append(" ");
                    sb_desc_narr.append(word).append(" ");
                }

                if (!fname.contains("ohsu")) {
                    while (!(line = br.readLine()).startsWith("<narr>")) {
                        toks = line.split(" ");
                        for (int i = 0; i < toks.length; i++) {

                            if (toks[i].equals("Description:")) {
                                continue;
                            }

                            String word = Utils.tidyWord(toks[i]);
                            if (word == null || (word.length() < 2)) {
                                continue;
                            }
                            sb_desc.append(word).append(" ");
                            sb_desc_only.append(word).append(" ");
                            sb_desc_narr.append(word).append(" ");
                        }
                    }
                } else {
                    while (!(line = br.readLine()).startsWith("</top>")) {
                        toks = line.split(" ");
                        for (int i = 0; i < toks.length; i++) {

                            if (toks[i].equals("Description:")) {
                                continue;
                            }

                            String word = Utils.tidyWord(toks[i]);
                            if (word == null || (word.length() < 2)) {
                                continue;
                            }
                            sb_desc.append(word).append(" ");
                            sb_desc_only.append(word).append(" ");
                            sb_desc_narr.append(word).append(" ");
                        }
                    }

                }
            }

            if (!fname.contains("ohsu")) {

                //narr
                if (line.startsWith("<narr>")) {
                    toks = line.split(" ");
                    for (int i = 1; i < toks.length; i++) {

                        if (toks[i].equals("Narrative:")) {
                            continue;
                        }

                        String word = Utils.tidyWord(toks[i]);
                        if (word == null || (word.length() < 2)) {
                            continue;
                        }

                        sb_desc_narr.append(word).append(" ");
                        sb_narr.append(word).append(" ");
                    }

                    while (!(line = br.readLine()).startsWith("</top>")) {
                        toks = line.split(" ");
                        for (int i = 0; i < toks.length; i++) {

                            if (toks[i].equals("Narrative:")) {
                                continue;
                            }

                            String word = Utils.tidyWord(toks[i]);
                            if (word == null || (word.length() < 2)) {
                                continue;
                            }

                            sb_desc_narr.append(word).append(" ");
                            sb_narr.append(word).append(" ");
                        }
                    }
                }
            }
            
            

        }

        //put last query into map
        if (cur_topic_num != null) {
            title_queries.put(cur_topic_num, sb.toString().split(" "));
            title_desc_queries.put(cur_topic_num, sb_desc.toString().split(" "));
            title_desc_narr_queries.put(cur_topic_num, sb_desc_narr.toString().split(" "));
            desc_queries.put(cur_topic_num, sb_desc_only.toString().split(" "));
            narr_queries.put(cur_topic_num, sb_narr.toString().split(" "));
        }

        logger.log(Level.INFO, title_queries.size() + " title queries loaded ... ");
        logger.log(Level.INFO, title_desc_queries.size() + " title_desc queries loaded ... ");
        logger.log(Level.INFO, title_desc_narr_queries.size() + " title_desc_narr queries loaded ... ");
        logger.log(Level.INFO, desc_queries.size() + " desc queries loaded ... ");
        logger.log(Level.INFO, narr_queries.size() + " narr queries loaded ... ");
        br.close();

    }
    
    
    /**
     * Get the mean of the results
     * @param map
     * @return 
     */
    public double Mean(TreeMap<String,Double> map) {
        double mean = 0.0;

        
        for(String key: map.keySet()){
            mean += (Double)map.get(key);
        }
        mean = mean / map.size();

        return mean;
    }    
    
    
    
    /**
     * Run a query 
     * @param key
     * @throws ParseException
     * @throws IOException 
     */
    
    public void runQuery(String key) throws ParseException, IOException {
        
        String[] query_array = current_set.get(key);
        if (query_array.length == 1){
            if (query_array[0].trim().equals("")){
                return;
            }
        }
        StringBuilder query_str = new StringBuilder();
        for (String s : query_array) {
            query_str.append(s).append(" ");
        }
        
        logger.info(key + "\t" + query_str.toString());
        
        Query query = new QueryParser("text", analyzer).parse(query_str.toString().trim());
        
        
        //run the SPUD dir method
        Query lmnorm_query = new LMNormQuery(query);
        searcher.setSimilarity(new SPUDLMSimilarity());
        TopDocs ret = searcher.search(lmnorm_query, 1000);
        
        
        //can run other lucene ranking functions without the lmnorm part
        //searcher.setSimilarity(new LMDirichletSimilarity());
        //searcher.setSimilarity(new BM25Similarity());
        //TopDocs ret = searcher.search(query, 1000);
        
        
        
        
        RankedList[] ranking = new RankedList[ret.scoreDocs.length];
        
        for (int i=0;i<ret.scoreDocs.length;i++){
            ranking[i] = new RankedList();
            
            ranking[i].doc_id = searcher.doc(ret.scoreDocs[i].doc).get("doc_id").toString();
            ranking[i].score = ret.scoreDocs[i].score;
        }
        
        double ap = eval.AP(key, ranking);
        double ndcg10 = eval.NDCG10(key, ranking);
        double ndcg20 = eval.NDCG20(key, ranking);
        
        //logger.info(ap + "\t" + ndcg10 + "\t" + ndcg20);
        if (!Double.isNaN(ap)){
            AP_values.put(key, ap);
            NDCG10_values.put(key, ndcg10);
            NDCG20_values.put(key, ndcg20);
            System.out.println(ap + "\t" + ndcg10 + "\t" + ndcg20);
        }else{
            //logger.info(key + " query does not have qrels");
        }
        
    }
    
    
    /**
     * run a set of queries
     * 
     * @throws ParseException
     * @throws IOException 
     */
    public void runQuerySet() throws ParseException, IOException{
        
        
        
        if (SPUDLMSimilarity.b0est == true){
            SPUDLMSimilarity.b0 = estimateB0();
        }else{
            //else set to average unique terms doc length as a basic estimate
            SPUDLMSimilarity.b0 = reader.getSumDocFreq("text")/reader.maxDoc();
            logger.info(SPUDLMSimilarity.b0 + "");
        }
        
        for (String key : this.current_set.keySet()){
            if (eval.getQrels().containsKey(key)){
                runQuery(key);
            }else{
                //logger.info("No qrels for Query " + key);
            }
        }
        
        logger.log(Level.INFO, "MAP " + Mean(this.AP_values) + " NDCG10 " + 
                Mean(this.NDCG10_values) + " NDCG20 " + Mean(this.NDCG20_values) 
                + " for " + this.NDCG20_values.size() + " queries");
        
        reader.close();
        
        
    }
    
    
    public void setQuerySet(int type) {

        QuerySearch.query_type = type;
        if (QuerySearch.query_type == QuerySearch.title) {
            this.current_set = title_queries;
        } else if (QuerySearch.query_type == QuerySearch.title_desc) {
            this.current_set = title_desc_queries;
        } else if (QuerySearch.query_type == QuerySearch.title_desc_narr) {
            this.current_set = title_desc_narr_queries;
        } else if (QuerySearch.query_type == QuerySearch.desc) {
            this.current_set = desc_queries;
        } else if (QuerySearch.query_type == QuerySearch.narr) {
            this.current_set = narr_queries;
        } else {
            this.current_set = title_queries;
        }

        int avg_len = 0;
        for (String key : current_set.keySet()) {

            String[] val = current_set.get(key);
            avg_len += val.length;

        }

        logger.info("Average query length: " + (double) avg_len / (double) current_set.size());
    }

    
}
