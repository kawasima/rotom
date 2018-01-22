package net.unit8.rotom.model;

import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Arrays;

public class LuceneTest {
    @Test
    public void test() throws Exception {
        Directory directory = new RAMDirectory();
        IndexWriter writer = new IndexWriter(directory,
            new IndexWriterConfig(new JapaneseAnalyzer()));
        try {
            writer.deleteAll();
            Document doc1 = new Document();
            doc1.add(new TextField("title", "hoge", Field.Store.YES));
            doc1.add(new TextField("body", "東京特許許可局", Field.Store.NO));

            Document doc2 = new Document();
            doc2.add(new TextField("title", "fuga", Field.Store.YES));
            doc2.add(new TextField("body", "かきくけこ", Field.Store.NO));

            writer.addDocuments(Arrays.asList(doc1, doc2));
            writer.commit();
        } finally {
            writer.close();
        }

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
        Query query = new TermQuery(new Term("body", "東京"));
        TopDocs results = searcher.search(query, 10);
        System.out.println(results.scoreDocs.length);
        Arrays.stream(results.scoreDocs)
            .forEach(scoreDoc -> {
                try {
                    Document doc = searcher.doc(scoreDoc.doc);
                    System.out.println(doc.getField("title").stringValue());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });


    }
}
