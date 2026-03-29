package net.unit8.rotom.search;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

public class LuceneTaskRunner {

    private LuceneTaskRunner() {}

    public static void update(IndexWriter writer, String path, String name, String data, String modified) {
        Term term = new Term("path", path);
        Document doc = new Document();

        try {
            try (BufferedReader reader = new BufferedReader(new HTMLStripCharFilter(new StringReader(data)))) {
                doc.add(new Field("body",
                    reader.lines().collect(Collectors.joining("\n")),
                    TextField.TYPE_STORED));
            }
            doc.add(new StringField("path", path, Field.Store.YES));
            doc.add(new TextField("name", name, Field.Store.YES));
            doc.add(new LongPoint("modified", Long.valueOf(modified)));
            writer.updateDocument(term, doc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void deleteAll(IndexWriter writer) {
        try {
            writer.deleteAll();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
