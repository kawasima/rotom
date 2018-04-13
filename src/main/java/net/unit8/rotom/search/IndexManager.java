package net.unit8.rotom.search;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import net.unit8.rotom.model.Page;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IndexManager extends SystemComponent {
    private Directory directory;
    private IndexWriter writer;
    private DirectoryReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private QueryParser parser;
    private SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();

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

                    IndexWriterConfig config = new IndexWriterConfig(analyzer);
                    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                    c.writer = new IndexWriter(c.directory, config);
                    c.writer.commit();

                    c.reader = DirectoryReader.open(c.writer);
                    c.searcher = new IndexSearcher(c.reader);
                    c.parser = new MultiFieldQueryParser(new String[]{"body", "name", "modified"}, c.analyzer);

                    c.ctx = new ZContext();
                    ZMQ.Socket commitSocket = ZThread.fork(c.ctx, (args, ctx, pipe) -> {
                        while(true) {
                            pipe.recv();
                            // Reader re-open
                            try {
                                c.writer.commit();
                                c.reader = DirectoryReader.openIfChanged(c.reader, c.writer);
                                c.searcher = new IndexSearcher(reader);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    });
                    c.socket = ZThread.fork(c.ctx, new LuceneTaskRunner(), writer, commitSocket);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public void stop(IndexManager c) {
                if (c.reader != null) {
                    try {
                        c.reader.close();
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
                TokenStream tokenStream = analyzer.tokenStream(null, doc.get("body"));

                TextFragment[] fragments = highlighter.getBestTextFragments(tokenStream, doc.get("body"), false, 10);
                foundPages.add(
                        new FoundPage(doc.get("urlPath"),
                                doc.get("name"),
                                Arrays.stream(fragments)
                                        .filter(f -> f != null && f.getScore() > 0)
                                        .map(TextFragment::toString)
                                        .collect(Collectors.joining()),
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

    public void save(Page page) {
        ZMsg msg = new ZMsg();
        msg.add("update");
        msg.add(page.getUrlPath());
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
