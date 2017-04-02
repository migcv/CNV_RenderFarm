package worker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import raytracer.*;

public class WebServer {

	// http://<load-balancer-DNS-name>/r.html?
	// f=<model-filename>&
	// sc=<scene-columns>&sr=<scene-rows>&
	// wc=<window-columns>&wr=<window-rows>&
	// coff=<column-offset>&roff=<row-offset>

	public static void main(String[] args) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
		server.createContext("/r.html", new MyHandler());
		// server.setExecutor(null); // creates a default executor
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		System.out.println("Server up!");
	}

	static class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String response = "This was the query:" + t.getRequestURI().getQuery() + "##";
			System.out.println("Received Resquest!");
			Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
			String concat1 = new String();

			for (String key : params.keySet()) {
				concat1.concat(key + " " + params.get(key) + "\n");
			}
			String[] args = { params.get("f"), "nomeFicheiroOutput.bmp", params.get("sc"), params.get("sr"),
					params.get("wc"), params.get("wr"), params.get("coff"), params.get("roff") };
			
			try {
				Main.main(args);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			t.sendResponseHeaders(200, concat1.length());
			OutputStream os = t.getResponseBody();
			os.write(concat1.getBytes());
			os.close();
		}
	}

	static Map<String, String> queryToMap(String query) {
		Map<String, String> result = new HashMap<String, String>();
		for (String param : query.split("&")) {

			String pair[] = param.split("=");
			if (pair.length > 1) {
				result.put(pair[0], pair[1]);
				System.out.println(pair[0] + " : " + pair[1]);
			} else {
				result.put(pair[0], "");
			}
		}
		return result;
	}

}
