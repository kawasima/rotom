package net.unit8.rotom.tool;

import enkan.system.Repl;
import enkan.system.repl.SystemCommandRegister;
import net.unit8.rotom.model.Page;
import net.unit8.rotom.model.Wiki;
import net.unit8.rotom.search.IndexManager;

import java.util.List;

public class LuceneCommandRegister implements SystemCommandRegister {
    @Override
    public void register(Repl repl) {
        repl.registerCommand("recreate-index", (system, transport, args) -> {
            IndexManager index = system.getComponent("index");
            index.deleteAll();

            Wiki wiki = system.getComponent("wiki");
            List<Page> pages = wiki.getPages("");
            for (Page page : pages) {
                index.save(page);
            }
            return true;
        });
    }
}
