import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;

public class LoadBalancer {

	/*
	 * Before running the code: Fill in your AWS access credentials in the
	 * provided credentials file template, and be sure to move the file to the
	 * default location (~/.aws/credentials) where the sample code will load the
	 * credentials from.
	 * https://console.aws.amazon.com/iam/home?#security_credential
	 *
	 * WARNING: To avoid accidental leakage of your credentials, DO NOT keep the
	 * credentials file in your source directory.
	 */

	static AmazonEC2 ec2;
	static AmazonElasticLoadBalancingClient elb;
	
	static ConcurrentHashMap<String,Instance> currentInstances = new ConcurrentHashMap<>();
	static ConcurrentHashMap<String,Integer> currentInstancesRanks = new ConcurrentHashMap<>();
	static ConcurrentHashMap<String, ConcurrentHashMap<String,Request>> currentInstancesResquests = new ConcurrentHashMap<>();
	
	static AmazonDynamoDBClient dynamoDB;
	
	static String OUTPUT_FILE_NAME = "output.bmp";
	
	static final String TABLE_NAME = "mss";
	
	static final int INSTANCE_MAX_RANK = 20;

	/**
	 * The only information needed to create a client are security credentials
	 * consisting of the AWS Access Key ID and Secret Access Key. All other
	 * configuration, such as the service endpoints, are performed
	 * automatically. Client parameters, such as proxies, can be specified in an
	 * optional ClientConfiguration object when constructing a client.
	 *
	 * @see com.amazonaws.auth.BasicAWSCredentials
	 * @see com.amazonaws.auth.PropertiesCredentials
	 * @see com.amazonaws.ClientConfiguration
	 */
	private static void init() throws Exception {

		/*
		 * The ProfileCredentialsProvider will return your [default] credential
		 * profile by reading from the credentials file located at
		 * (~/.aws/credentials).
		 */
		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is in valid format.", e);
		}
		ec2 = AmazonEC2ClientBuilder.standard().withRegion("us-west-2")
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).build();

		elb = new AmazonElasticLoadBalancingClient(credentials);
		
        dynamoDB = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDB.setRegion(usWest2);
		
	}

	public static void main(String[] args) throws Exception {

		System.out.println("===========================================");
		System.out.println("Welcome to the AWS Java SDK!");
		System.out.println("===========================================");

		init();
		setupServer();
		
		new HealthCheckThread();

	}

	public static void setupServer() {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(8000), 0);
			server.createContext("/r.html", new LoadBalancer.MyHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
			System.out.println("Load Balancer ready");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static class MyHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange t) {

			String response = new String();

			try {
				System.out.println("Connection from: " + t.getRemoteAddress());

				if (t.getRequestURI().getQuery().isEmpty()) {
					response = "OK";
					t.sendResponseHeaders(200, response.length());
					OutputStream os = t.getResponseBody();
					os.write(response.getBytes());
					os.close();
					return;
				}
				
				Request request = setResquest(t.getRequestURI().getQuery());
				int rank = getRequestRank(request);
				
				if(rank == -1) {
					// NEW REQUEST
				} else {
					String instanceID = getFreeInstance(request.getRank());
					if(instanceID != null) {
						currentInstancesRanks.put(instanceID, currentInstancesRanks.get(instanceID) + request.getRank());
						currentInstancesResquests.get(instanceID).put(request.getId(), request);
						redirectRequest(t, currentInstances.get(instanceID).getPublicDnsName());
						currentInstancesRanks.put(instanceID, currentInstancesRanks.get(instanceID) - request.getRank());
						currentInstancesResquests.get(instanceID).remove(request.getId());
					}
				}

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
	
	static void startNewInstance() {
		
		Thread thread = new Thread() {
			
			public void run() {
				
				//Instance instance AutoScaler.startInstance();
				Instance instance = null;
				
				currentInstances.put(instance.getInstanceId(), instance);
				currentInstancesRanks.put(instance.getInstanceId(), 0);
				currentInstancesResquests.put(instance.getInstanceId(), new ConcurrentHashMap<String, Request>());
				
			}
		};
		thread.start();
		
	}
	
	static void removeInstance(final String instaceID) {
		
		Thread thread = new Thread() {
			
			public void run() {
				
				currentInstances.remove(instaceID);
				currentInstancesRanks.remove(instaceID);
				currentInstancesResquests.remove(instaceID);
				
				//Instance instance AutoScaler.startInstance();
				
			}
		};
		thread.start();
		
	}
	
	public static int getRequestRank(Request request) {
		
		HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
		Condition condition = new Condition()
	            .withComparisonOperator(ComparisonOperator.EQ.toString())
	            .withAttributeValueList(new AttributeValue().withN(request.getFilename()));
	    scanFilter.put("filename", condition);
        condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(request.getScols()));
        scanFilter.put("scols", condition);
        condition = new Condition()
            .withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue().withN(request.getSrows()));
        scanFilter.put("srows", condition);
        condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(request.getWcols()));
        scanFilter.put("wcols", condition);
        condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(request.getWrows()));
        scanFilter.put("wrows", condition);
        condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(request.getCoff()));
        scanFilter.put("coff", condition);
        condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue().withN(request.getRoff()));
        scanFilter.put("roff", condition);
        ScanRequest scanRequest = new ScanRequest(TABLE_NAME).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        System.out.println("Result: " + scanResult);
		if(scanResult.getItems().size() > 0) {
			return Integer.parseInt(scanResult.getItems().get(0).get("rank").getN());
		}
		return -1;
	}
	
	public static String getFreeInstance(int requestRank) {
		for(String instanceID : currentInstancesRanks.keySet()) {
			int instanceRank = currentInstancesRanks.get(instanceID);
			if((instanceRank + requestRank) <= INSTANCE_MAX_RANK) {
				return instanceID;
			}
		}
		return null;
	}
	
	public static void redirectRequest(HttpExchange t, String dns) throws IOException {
		
		System.out.println("QUERY: " + t.getRequestURI().getQuery());

		URL url = new URL("http://" + dns + ":8000/r.html?" + t.getRequestURI().getQuery());
		// URL url = new URL("http://" + dns + ":8000/r.html?");
		System.out.println("URL");
		URLConnection connection = url.openConnection();
		System.out.println("URLConnection");


		InputStream is = connection.getInputStream();

		String saveFilePath = Thread.currentThread().getId() + OUTPUT_FILE_NAME;

		// opens an output stream to save into file
		FileOutputStream outputStream = new FileOutputStream(saveFilePath);

		int bytesRead = -1;
		byte[] buffer = new byte[2048];
		while ((bytesRead = is.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}

		outputStream.close();
		is.close();

		System.out.println("File downloaded");

		File image = new File(saveFilePath);

		t.sendResponseHeaders(200, image.length());

		OutputStream os = t.getResponseBody();
		Files.copy(image.toPath(), os);
		os.close();
	}
	
	/*static void startNewInstance() {
		DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
		List<Reservation> reservations = describeInstancesRequest.getReservations();
		Set<Instance> instances = new HashSet<Instance>();

		for (Reservation reservation : reservations) {
			instances.addAll(reservation.getInstances());
		}

		System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
		System.out.println("Starting a new instance.");
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

		runInstancesRequest.withImageId("ami-b82d4bd8").withInstanceType("t2.micro").withMinCount(1)
				.withMaxCount(1).withKeyName("CNV-lab-AWS").withSecurityGroups("launch-wizard-1");
		RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
		String newInstanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
		describeInstancesRequest = ec2.describeInstances();
		reservations = describeInstancesRequest.getReservations();
		instances = new HashSet<Instance>();

		for (Reservation reservation : reservations) {
			instances.addAll(reservation.getInstances());
		}

		System.out.println("Started instance with id > " + newInstanceId);

		System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
		System.out.println("Waiting 2 minute. See your instance in the AWS console...");
		try {
			Thread.sleep(150000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// System.out.println("Terminating the instance.");
		// TerminateInstancesRequest termInstanceReq = new
		// TerminateInstancesRequest();
		// termInstanceReq.withInstanceIds(newInstanceId);
		// ec2.terminateInstances(termInstanceReq);

		Long time = System.currentTimeMillis();

		String dns = "";
		String instanceID = "";
		Instance inst = null;
		for (Reservation reservation : reservations) {
			for (Instance instance : reservation.getInstances()) {
				if (instance.getInstanceId().equals(newInstanceId)) {
					while (instance.getPublicDnsName().isEmpty()) {}
					System.out.println("DNS: " + instance.getPublicDnsName() + " ID:" + instance.getInstanceId());
					dns = instance.getPublicDnsName();
					instanceID = instance.getInstanceId();
					inst = instance;
					break;
				}
			}
		}

		if(!healthCheck(dns, instanceID)){
			return;
		}
		
		currentInstances.add(inst);
	}*/

	static boolean healthCheck(String dns, String instanceID) {
		String response = new String();
		int healthy = 0;
		int unhealthy = 0;
		URLConnection connection;
		URL url;
		// URL url = new URL("http://" + dns + ":8000/r.html?" +
		// t.getRequestURI().getQuery());
		while (healthy < 3) {
			try {
				url = new URL("http://" + dns + ":8000/r.html?");
				connection = url.openConnection();
				Scanner s = new Scanner(connection.getInputStream());
				while (s.hasNext()) {
					response += s.next();
					System.out.println("Response: " + response);
				}
				s.close();

				if (response.equals("OK")) {
					System.out.println("HEALTY");
					unhealthy = 0;
					healthy += 1;
					response = "";
				}
				System.out.println("ACABEI");

			} catch (IOException e) {
				healthy = 0;
				unhealthy += 1;
				url = null;
				connection = null;
				System.out.println("UNHEALTY");
				if (unhealthy > 2) {
					System.out.println("Terminating the instance.");
					TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
					termInstanceReq.withInstanceIds(instanceID);
					ec2.terminateInstances(termInstanceReq);
					return false;
				}
				// e.printStackTrace();
			}
		}
		return true;
	}

	static void threadToHealthCheck(String newInstanceId) throws Exception {

		DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
		List<Reservation> reservations = describeInstancesRequest.getReservations();
		Set<Instance> instances = new HashSet<Instance>();

		for (Reservation reservation : reservations) {
			instances.addAll(reservation.getInstances());
		}

		for (Reservation reservation : reservations) {
			for (Instance instance : reservation.getInstances()) {
				if (instance.getInstanceId().equals(newInstanceId)) {
					String response = new String();
					// URL url = new URL("http://" + dns + ":8000/r.html?" +
					// t.getRequestURI().getQuery());
					URL url = new URL("http://" + instance.getPublicDnsName() + ":8000/r.html?");
					System.out.println("URL");
					URLConnection connection = url.openConnection();
					System.out.println("URLConnection");
					Scanner s = new Scanner(connection.getInputStream());
					System.out.println("Scanner");
					while (s.hasNext()) {
						response += s.next();
						System.out.println("Response: " + response);
					}
					s.close();
					break;
				}
			}
		}

	}

	static class HealthCheckThread extends Thread {

		@Override
		public void run() {
			try {
				while (true) {
					for (String instanceID : currentInstances.keySet()) {
						//AutoScaler.healthCheck(currentInstances.get(instanceID));
						boolean healthy = true;
						if(healthy) {
							//REMOVER DAS HASHES E FAZER DELETE
						}
					}
					sleep(30000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	static Request setResquest(String query) {
		String [] params = new String[7];
		int i = 0;
		for (String param : query.split("&")) {
			String pair[] = param.split("=");
			if (pair.length > 1) {
				params[i++] = pair[1];
			}
		}
		return new Request(params[0], params[1], params[2], params[3], params[4], params[5], params[6]);
	}

}
