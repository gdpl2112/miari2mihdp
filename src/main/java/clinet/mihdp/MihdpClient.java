package clinet.mihdp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.kloping.MySpringTool.h1.impl.LoggerImpl;
import io.github.kloping.MySpringTool.interfaces.Logger;
import io.github.kloping.common.Public;
import io.github.kloping.judge.Judge;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author github.kloping
 */
public class MihdpClient extends WebSocketClient {
    public static MihdpClient INSTANCE;
    private Logger logger = new LoggerImpl();
    private Gson gson;

    public Map<String, MihdpClientMessageListener> listeners = new HashMap<>();

    public MihdpClient() throws URISyntaxException {
        super(new URI("ws://localhost:6034"));
        INSTANCE = this;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(GeneralData.class, new GeneralData.GeneralDataDeserializer());
        gson = gsonBuilder.create();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("============MihdpClient OPEN===========");
        send("123456");
    }

    @Override
    public void onMessage(String message) {
        logger.log(message);
        ResDataPack dataPack = gson.fromJson(message, ResDataPack.class);
        if (dataPack == null || dataPack.getAction() == null) return;
        String bid = dataPack.getBot_id();
        if (Judge.isEmpty(bid)) {
            listeners.forEach((k, v) -> v.onMessage(dataPack));
        } else if (listeners.containsKey(bid)) {
            listeners.get(bid).onMessage(dataPack);
        }
    }

    @Override
    public void send(String text) {
        if (!this.isOpen()) return;
        super.send(text);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Public.EXECUTOR_SERVICE.submit(() -> {
            try {
                logger.error("=========MihdpClient ==reconnect=====");
                TimeUnit.SECONDS.sleep(30);
                reconnect();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void onError(Exception ex) {

    }

    public interface MihdpClientMessageListener {
        void onMessage(ResDataPack pack);
    }
}
