package net.unit8.rotom.search;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import net.unit8.rotom.model.Page;
import net.unit8.rotom.model.Wiki;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.lucene.document.Field.Store;

public class IndexManager extends SystemComponent {
    private Directory directory;
    private IndexWriter writer;
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private QueryParser parser;
    private SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();

    private Path indexPath;

    @Override
    protected ComponentLifecycle<IndexManager> lifecycle() {
        return new ComponentLifecycle<IndexManager>() {
            @Override
            public void start(IndexManager component) {
                try {
                    directory = FSDirectory.open(indexPath);
                    analyzer = new JapaneseAnalyzer();

                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    writer = new IndexWriter(directory, config);
                    writer.commit();

                    reader = DirectoryReader.open(directory);
                    searcher = new IndexSearcher(reader);
                    parser = new MultiFieldQueryParser(new String[]{"body", "name", "modified"}, new JapaneseAnalyzer());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void stop(IndexManager component) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                if (directory != null) {
                    try {
                        directory.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        };
    }

    public void save(Page page) {
        try {
            String path = Wiki.fullpath(page.getPath(), page.getName());
            Term term = new Term("path", path);
            Document doc = new Document();
            doc.add(new TextField("body", page.getFormattedData(), Store.NO));
            doc.add(new StringField("path", path, Store.YES));
            doc.add(new TextField("name", page.getName(), Store.YES));
            doc.add(new LongPoint("modified", page.getLastVersion().getCommitTime()));
            writer.updateDocument(term, doc);
            writer.commit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Pagination<FoundPage> search(String queryStr, int offset, int limit) {
        try {
            QueryParser parser = new QueryParser("body", new JapaneseAnalyzer());
            Query query = parser.parse(queryStr);
            int upper = offset + limit;
            TopDocs results = searcher.search(query, upper);
            Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));

            List<FoundPage> foundPages = new ArrayList<>(limit);
            long count = Math.min(upper, results.totalHits);
            for (int i = offset; i < count; i++) {
                Document doc = reader.document(results.scoreDocs[i].doc);
                foundPages.add(
                        new FoundPage(doc.getField("path").stringValue(),
                                doc.getField("name").stringValue(),
                                /*
                                Arrays.stream(fragments)
                                        .filter(f -> f != null && f.getScore() > 0)
                                        .map(TextFragment::toString)
                                        .collect(Collectors.joining()),
                                        */ "",
                                results.scoreDocs[i].score));
            }
            return new Pagination<>(
                    foundPages,
                    results.totalHits,
                    offset,
                    limit);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    public void setIndexPath(Path indexPath) {
        this.indexPath = indexPath;
    }
}
