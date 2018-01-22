package net.unit8.rotom.tool;

import enkan.system.Repl;
import enkan.system.repl.SystemCommandRegister;
import net.unit8.rotom.search.IndexManager;

public class LuceneCommandRegister implements SystemCommandRegister {
    @Override
    public void register(Repl repl) {
        repl.registerCommand("recreate-index", (system, transport, args) -> {
            IndexManager index = system.getComponent("index");
            index.deleteAll();
            return true;
        });
    }
}
