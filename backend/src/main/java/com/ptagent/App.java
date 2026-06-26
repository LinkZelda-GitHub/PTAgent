package com.ptagent;

import com.ptagent.repository.AppRepository;
import com.ptagent.web.ApiRouter;
import com.ptagent.web.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = port(args);
        AppRepository repository = new AppRepository();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new ApiRouter(repository));
        server.createContext("/", new StaticFileHandler("public"));
        server.setExecutor(Executors.newFixedThreadPool(12));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        server.start();
        System.out.println("PTAgent 家教资源整合平台已启动：http://localhost:" + port);
        new CountDownLatch(1).await();
    }

    private static int port(String[] args) {
        if (args.length > 0) {
            return Integer.parseInt(args[0]);
        }
        String env = System.getenv("PORT");
        if (env != null && !env.isBlank()) {
            return Integer.parseInt(env);
        }
        return 8080;
    }
}
