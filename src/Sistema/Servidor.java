package Sistema;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import Escalonador.*;

/** Servidor HTTP embutido: serve web/index.html e expõe o Kernel em /api/*. */
public final class Servidor {
    private static volatile Kernel kernel;
    // ação -> {CPU total (s), E/S após (s, 0 = sem), duração da E/S (s)}
    private static final Map<String, int[]> ACOES = Map.of(
        "Atacar", new int[]{6, 0, 0}, "Pular", new int[]{2, 0, 0}, "Andar", new int[]{4, 0, 0},
        "Seguir", new int[]{3, 1, 4}, "Curtir", new int[]{2, 1, 3},
        "Pesquisar", new int[]{5, 1, 4}, "Calcular", new int[]{2, 0, 0});

    private Servidor() {}

    public static void iniciar(int porta) throws Exception {
        var srv = HttpServer.create(new InetSocketAddress("localhost", porta), 0);
        srv.createContext("/", ex -> {
            String tipo = "application/json; charset=utf-8", corpo = "{}";
            try {
                String path = ex.getRequestURI().getPath();
                var q = params(ex.getRequestURI().getRawQuery());
                var k = kernel;
                switch (path) {
                    case "/api/estado" -> corpo = k == null ? "{\"on\":false}" : k.estadoJson();
                    case "/api/iniciar" -> {
                        if (k != null) k.parar();
                        int quantum = Integer.parseInt(q.getOrDefault("q", "4"));
                        protocolEscalonador e = switch (q.getOrDefault("alg", "rr")) {
                            case "fcfs" -> new Fcfs();
                            case "edf" -> new Edf();
                            default -> new RoundRobin(quantum);
                        };
                        var novo = new Kernel(e, Integer.parseInt(q.getOrDefault("ms", "1000")));
                        kernel = novo;
                        new Thread(novo::iniciar).start();
                    }
                    case "/api/acao" -> {
                        var a = ACOES.get(q.get("acao"));
                        if (k != null && a != null)
                            k.lancar(q.get("app") + " / " + q.get("acao"), a[0], a[1], a[2]);
                    }
                    case "/api/pausar" -> { if (k != null) k.pausar("1".equals(q.get("v"))); }
                    case "/api/passo" -> { if (k != null) k.passo(); }
                    case "/api/velocidade" -> { if (k != null) k.setTickMs(Integer.parseInt(q.get("ms"))); }
                    case "/api/fechar" -> { if (k != null) k.encerrarProcesso(Integer.parseInt(q.get("n"))); }
                    case "/api/parar" -> { if (k != null) k.parar(); kernel = null; }
                    default -> {
                        tipo = "text/html; charset=utf-8";
                        var f = Files.exists(Path.of("web/index.html")) ? Path.of("web/index.html")
                                                                         : Path.of("../web/index.html");
                        corpo = Files.readString(f);
                    }
                }
            } catch (Exception e) { corpo = "{\"erro\":\"" + e + "\"}"; }
            byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", tipo);
            ex.sendResponseHeaders(200, b.length);
            try (var o = ex.getResponseBody()) { o.write(b); }
        });
        srv.start();
        System.out.println("Interface gráfica em http://localhost:" + porta);
    }

    private static Map<String, String> params(String raw) {
        var m = new HashMap<String, String>();
        if (raw == null) return m;
        for (var kv : raw.split("&")) {
            var p = kv.split("=", 2);
            m.put(java.net.URLDecoder.decode(p[0], StandardCharsets.UTF_8),
                  p.length > 1 ? java.net.URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }
}
