package raytracer;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

	public static String OUTPUT_FILE_NAME = "output.bmp";
	
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
			Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
			String response = new String();
			if(params.get("f") == null){
				response = "OK";
				t.sendResponseHeaders(200, response.length());
				OutputStream os = t.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
			}

			System.out.println(Thread.currentThread().getId() + " - Received Resquest: <" + t.getRequestURI().getQuery() + ">");
			writeRequest(t.getRequestURI().getQuery());
				
			String[] args = { params.get("f"), OUTPUT_FILE_NAME, params.get("sc"), params.get("sr"),
					params.get("wc"), params.get("wr"), params.get("coff"), params.get("roff") };
			
			try {
				Main.main(args);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			File image = new File(OUTPUT_FILE_NAME);
			
			// System.out.println("HTTP_Response: " + response);
			t.sendResponseHeaders(200, image.length());
			OutputStream os = t.getResponseBody();
			Files.copy(image.toPath(), os);
			// os.write(image.getBytes());
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

	static void writeRequest(String query) {
		try {
			File file = new File("/home/ec2-user/CNV_RenderFarm/log/" + Thread.currentThread().getId() +".txt");
			FileWriter log = new FileWriter(file, true);
    		//FileWriter log = new FileWriter("/home/ec2-user/CNV_RenderFarm/log/" + Thread.currentThread().getId() +".txt", true);
    		//DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    		//Calendar cal = Calendar.getInstance();
			log.write("###############################\n" + query + "\n");
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
