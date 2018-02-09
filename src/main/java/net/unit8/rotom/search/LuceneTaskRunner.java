package net.unit8.rotom.search;

import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import org.zeromq.ZThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

public class LuceneTaskRunner implements ZThread.IAttachedRunnable{

    @Override
    public void run(Object[] args, ZContext ctx, ZMQ.Socket pipe) {
        boolean shutdown = false;
        IndexWriter writer = (IndexWriter) args[0];
        ZMQ.Socket commitSocket = (ZMQ.Socket) args[1];
        while(!shutdown) {
            ZMsg msg = ZMsg.recvMsg(pipe);
            String type = msg.popString();
            switch(type) {
                case "deleteAll":
                    deleteAll(writer);
                    commitSocket.send("commit");
                    break;
                case "update":
                    update(writer,
                        /* urlPath */msg.pop().getString(ZMQ.CHARSET),
                        /* name */msg.pop().getString(ZMQ.CHARSET),
                        /* data */msg.pop().getString(ZMQ.CHARSET),
                        /* modified*/msg.popString());
                    commitSocket.send("commit");
                    break;
                case "shutdown":
                    shutdown = true;
                    break;
            }
        }
    }

    public void update(IndexWriter writer, String urlPath, String name, String data, String modified) {
        Term term = new Term("urlPath", urlPath);
        Document doc = new Document();

        try {
            try (BufferedReader reader = new BufferedReader(new HTMLStripCharFilter(new StringReader(data)))) {
                doc.add(new Field("body",
                    reader.lines().collect(Collectors.joining("\n")),
                    TextField.TYPE_STORED));
            }
            doc.add(new StringField("urlPath", urlPath, Field.Store.YES));
            doc.add(new TextField("name", name, Field.Store.YES));
            doc.add(new LongPoint("modified", Long.valueOf(modified)));
            writer.updateDocument(term, doc);
        } catch (IOException e) {

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
