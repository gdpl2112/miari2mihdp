package clinet;

import clinet.mihdp.MihdpClient;
import io.github.kloping.common.Public;
import io.github.kloping.rand.RandomUtils;
import net.mamoe.mirai.console.terminal.MiraiConsoleImplementationTerminal;
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader;
import net.mamoe.mirai.event.GlobalEventChannel;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author github.kloping
 */
public class MiraiStarter {
    public static ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(5, 5, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), r -> new Thread(r));

    public static void main(String[] args) throws URISyntaxException {
        EXECUTOR_SERVICE.submit(() -> {
            MiraiConsoleImplementationTerminal terminal = new MiraiConsoleImplementationTerminal(Paths.get("works"));
            MiraiConsoleTerminalLoader.INSTANCE.startAsDaemon(terminal);
        });
        EXECUTOR_SERVICE.submit(new MihdpClient());
        GlobalEventChannel.INSTANCE.registerListenerHost(MihdpConnect.INSTANCE);
    }
}
