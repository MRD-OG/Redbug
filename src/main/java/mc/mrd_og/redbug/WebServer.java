package mc.mrd_og.redbug;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class WebServer {

    private final ProbeManager probeManager;
    private final int port;
    private HttpServer server;

    public WebServer(ProbeManager probeManager, int port) {
        this.probeManager = probeManager;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", this::handleIndex);
        server.createContext("/api/probes", this::handleSSE);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        try (var is = getClass().getResourceAsStream("/web/index.html")) {
            if (is == null) {
                String msg = "index.html not found";
                exchange.sendResponseHeaders(404, msg.length());
                exchange.getResponseBody().write(msg.getBytes(StandardCharsets.UTF_8));
            } else {
                byte[] response = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            }
        }
        exchange.close();
    }

    private void handleSSE(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                String json = probeManager.getLatestJson();
                os.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
                Thread.sleep(200);
            }
        } catch (InterruptedException | IOException e) {
            // client disconnected or server shutting down
        } finally {
            exchange.close();
        }
    }
}
