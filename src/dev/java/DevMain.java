import enkan.system.devel.DevelCommandRegister;
import enkan.system.repl.JShellRepl;
import enkan.system.repl.ReplBoot;
import kotowari.system.KotowariCommandRegister;

public class DevMain {
    public static void main(String[] args) throws Exception {
        JShellRepl repl = new JShellRepl("net.unit8.rotom.RotomSystemFactory");

        new ReplBoot(repl)
                .register(new KotowariCommandRegister())
                .register(new DevelCommandRegister())
                .onReady("/start")
                .start();
    }
}
