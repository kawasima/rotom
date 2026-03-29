package net.unit8.rotom.model;

import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class LuceneTest {

    @Test
    void japaneseTokenizationAndSearch() throws Exception {
        Directory directory = new ByteBuffersDirectory();
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new JapaneseAnalyzer()))) {
            Document doc1 = new Document();
            doc1.add(new TextField("title", "hoge", Field.Store.YES));
            doc1.add(new TextField("body", "東京特許許可局", Field.Store.NO));

            Document doc2 = new Document();
            doc2.add(new TextField("title", "fuga", Field.Store.YES));
            doc2.add(new TextField("body", "かきくけこ", Field.Store.NO));

            writer.addDocuments(Arrays.asList(doc1, doc2));
        }

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
        TopDocs results = searcher.search(new TermQuery(new Term("body", "東京")), 10);

        assertEquals(1, results.scoreDocs.length, "Should find exactly one document matching '東京'");

        StoredFields storedFields = searcher.storedFields();
        Document found = storedFields.document(results.scoreDocs[0].doc);
        assertEquals("hoge", found.getField("title").stringValue());
    }

    @Test
    void noResultsForUnmatchedQuery() throws Exception {
        Directory directory = new ByteBuffersDirectory();
        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new JapaneseAnalyzer()))) {
            Document doc = new Document();
            doc.add(new TextField("body", "東京特許許可局", Field.Store.NO));
            writer.addDocument(doc);
        }

        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
        TopDocs results = searcher.search(new TermQuery(new Term("body", "大阪")), 10);
        assertEquals(0, results.scoreDocs.length);
    }
}
