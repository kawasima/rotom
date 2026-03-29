package net.unit8.rotom.search;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.SearcherManager;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

public class LuceneTaskRunner implements ZThread.IAttachedRunnable {

    @Override
    public void run(Object[] args, ZContext ctx, ZMQ.Socket pipe) {
        boolean shutdown = false;
        IndexWriter writer = (IndexWriter) args[0];
        SearcherManager searcherManager = (SearcherManager) args[1];
        while (!shutdown) {
            ZMsg msg = ZMsg.recvMsg(pipe);
            String type = msg.popString();
            switch (type) {
                case "deleteAll":
                    deleteAll(writer);
                    refreshSearcher(searcherManager);
                    break;
                case "update":
                    update(writer,
                        msg.pop().getString(ZMQ.CHARSET),
                        msg.pop().getString(ZMQ.CHARSET),
                        msg.pop().getString(ZMQ.CHARSET),
                        msg.popString());
                    refreshSearcher(searcherManager);
                    break;
                case "shutdown":
                    shutdown = true;
                    break;
            }
        }
    }

    private void refreshSearcher(SearcherManager searcherManager) {
        try {
            searcherManager.maybeRefresh();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void update(IndexWriter writer, String path, String name, String data, String modified) {
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

    public void deleteAll(IndexWriter writer) {
        try {
            writer.deleteAll();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
