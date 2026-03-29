package net.unit8.rotom.search;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import net.unit8.rotom.model.Page;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZThread;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IndexManager extends SystemComponent<IndexManager> {
    private Directory directory;
    private IndexWriter writer;
    private SearcherManager searcherManager;
    private Analyzer analyzer;

    private ZContext ctx;
    private ZMQ.Socket socket;
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

                    c.ctx = new ZContext();
                    c.socket = ZThread.fork(c.ctx, new LuceneTaskRunner(), c.writer, c.searcherManager);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void stop(IndexManager c) {
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

                if (c.ctx != null) {
                    c.ctx.destroy();
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
        ZMsg msg = new ZMsg();
        msg.add("update");
        msg.add(page.getDir() + "/" + page.getName());
        msg.add(page.getName());
        msg.add(page.getFormattedData());
        msg.add(String.valueOf(page.getModifiedTime()));
        msg.send(socket);
    }

    public void deleteAll() {
        ZMsg msg = new ZMsg();
        msg.push("deleteAll");
        msg.send(socket);
    }

    public void setIndexPath(Path indexPath) {
        this.indexPath = indexPath;
    }
}
