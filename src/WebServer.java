

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import raytracer.Main;

public class WebServer {

	// http://<load-balancer-DNS-name>/r.html?
	// f=<model-filename>&
	// sc=<scene-columns>&sr=<scene-rows>&
	// wc=<window-columns>&wr=<window-rows>&
	// coff=<column-offset>&roff=<row-offset>

	public static String OUTPUT_FILE_NAME = "output.bmp";

	public static void main(String[] args) {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

			server.createContext("/r.html", new MyHandler());
			// server.setExecutor(null); // creates a default executor
			server.setExecutor(Executors.newCachedThreadPool());
			server.start();
			System.out.println("Server up!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class MyHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) {
			
			String response = new String();
			
			try {
				Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
				System.out.println("Connection from: " + t.getRemoteAddress());
				
				if (params.get("f") == null) {
					response = "OK";
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
					return;
				}
				
				MyTool.writeRequest(params);

				System.out.println(Thread.currentThread().getId() + " - Received Resquest: <"
						+ t.getRequestURI().getQuery() + ">");

				String[] args = { params.get("f"), OUTPUT_FILE_NAME, params.get("sc"), params.get("sr"),
						params.get("wc"), params.get("wr"), params.get("coff"), params.get("roff") };

				Main.main(args);
				
				File image = new File(OUTPUT_FILE_NAME);

				// System.out.println("HTTP_Response: " + response);

				t.sendResponseHeaders(200, image.length());

				OutputStream os = t.getResponseBody();
				Files.copy(image.toPath(), os);
				// os.write(image.getBytes());
				os.close();
			} catch (Exception e) {
				try {
					response = "ERROR: " + e.getMessage();
					System.out.println("ERROR: " + e.getMessage());
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
					e.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	static  Map<String, String> queryToMap(String query) {
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