import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Paths;

public class IndexViewer {
    public static void main(String[] args) throws Exception {
        Directory directory = FSDirectory.open(Paths.get("index"));
        IndexReader reader = DirectoryReader.open(directory);
        System.out.println(reader.numDocs());

        IndexSearcher searcher = new IndexSearcher(reader);

        QueryParser parser = new QueryParser("body", new JapaneseAnalyzer());
        Query query = parser.parse("千葉");
        TopDocs results = searcher.search(query, 10);
        System.out.println(results.totalHits);
    }
}
