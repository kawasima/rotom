package net.unit8.rotom.search;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import net.unit8.rotom.model.Page;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class IndexManager extends SystemComponent<IndexManager> {
    private Directory directory;
    private IndexWriter writer;
    private SearcherManager searcherManager;
    private Analyzer analyzer;
    private ExecutorService executor;
    private Path indexPath;

    @Override
    protected ComponentLifecycle<IndexManager> lifecycle() {
        return new ComponentLifecycle<IndexManager>() {
            @Override
            public void start(IndexManager c) {
                try {
                    c.directory = FSDirectory.open(c.indexPath);
                    c.analyzer = new JapaneseAnalyzer();

                    IndexWriterConfig config = new IndexWriterConfig(c.analyzer);
                    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    config.setCommitOnClose(true);
                    c.writer = new IndexWriter(c.directory, config);

                    c.searcherManager = new SearcherManager(c.writer, new SearcherFactory());
                    c.executor = Executors.newSingleThreadExecutor(r -> {
                        Thread t = new Thread(r, "lucene-index-worker");
                        t.setDaemon(true);
                        return t;
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void stop(IndexManager c) {
                if (c.executor != null) {
                    c.executor.shutdown();
                    try {
                        c.executor.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (c.searcherManager != null) {
                    try {
                        c.searcherManager.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                if (c.writer != null) {
                    try {
                        c.writer.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
                if (c.directory != null) {
                    try {
                        c.directory.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        };
    }

    public Pagination<FoundPage> search(String queryStr, int offset, int limit) {
        if (queryStr == null || queryStr.isBlank()) {
            return new Pagination<>(List.of(), 0, offset, limit, true);
        }

        IndexSearcher searcher = null;
        try {
            QueryParser parser = new QueryParser("body", new JapaneseAnalyzer());
            Query query = parser.parse(QueryParser.escape(queryStr));
            int upper = offset + limit;

            searcher = searcherManager.acquire();
            TopDocs results = searcher.search(query, upper);

            UnifiedHighlighter highlighter = UnifiedHighlighter.builder(searcher, analyzer)
                    .withMaxLength(Integer.MAX_VALUE)
                    .build();
            String[] bodyHighlights = highlighter.highlight("body", query, results);

            StoredFields storedFields = searcher.storedFields();
            List<FoundPage> foundPages = new ArrayList<>(limit);
            long count = Math.min(results.scoreDocs.length, Math.min(upper, results.totalHits.value));
            for (int i = offset; i < count; i++) {
                var doc = storedFields.document(results.scoreDocs[i].doc);
                String summary = (bodyHighlights != null && i < bodyHighlights.length && bodyHighlights[i] != null)
                        ? bodyHighlights[i]
                        : "";
                foundPages.add(
                        new FoundPage(doc.get("path"),
                                doc.get("name"),
                                summary,
                                results.scoreDocs[i].score));
            }

            boolean exact = results.totalHits.relation == TotalHits.Relation.EQUAL_TO;
            return new Pagination<>(
                    foundPages,
                    results.totalHits.value,
                    offset,
                    limit,
                    exact);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            if (searcher != null) {
                try {
                    searcherManager.release(searcher);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    public void save(Page page) {
        String path = page.getDir() + "/" + page.getName();
        String name = page.getName();
        String data = page.getFormattedData();
        String modified = String.valueOf(page.getModifiedTime());
        executor.submit(() -> {
            updateDocument(path, name, data, modified);
            refreshSearcher();
        });
    }

    public void deleteAll() {
        executor.submit(() -> {
            try {
                writer.deleteAll();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            refreshSearcher();
        });
    }

    private void updateDocument(String path, String name, String data, String modified) {
        try {
            Document doc = new Document();
            try (var reader = new BufferedReader(new HTMLStripCharFilter(new StringReader(data)))) {
                doc.add(new Field("body",
                        reader.lines().collect(Collectors.joining("\n")),
                        TextField.TYPE_STORED));
            }
            doc.add(new StringField("path", path, Field.Store.YES));
            doc.add(new TextField("name", name, Field.Store.YES));
            doc.add(new LongPoint("modified", Long.valueOf(modified)));
            writer.updateDocument(new Term("path", path), doc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void refreshSearcher() {
        try {
            searcherManager.maybeRefresh();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void setIndexPath(Path indexPath) {
        this.indexPath = indexPath;
    }
}
