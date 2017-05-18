import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoScaler {

	public final static int HIGH_CPU_USAGE = 80;
	public final static int LOW_CPU_USAGE = 20;
	public final static long METRICS_OFFSET = 24 * 60 * 1000 * 60; // to get
																	// metrics
																	// within
																	// the last
																	// 24h
	public final static long LAUNCH_INSTANCE_OFFSET = 3 * 1000 * 60; // 3 min
																		// for
																		// instance
																		// launch
																		// time
																		// offset
	public final static long UNHEALTHY_THRESHOLD = 3;

	static AmazonCloudWatch cloudWatch;

	public static ConcurrentHashMap<String, Instance> pendingInstances = new ConcurrentHashMap<>();
	static int pendingInstance = 0;

	public static Instance startInstance() throws InterruptedException {

		try {
			System.out.println("Starting a new instance.");

			DescribeInstancesResult describeInstancesRequest = LoadBalancer.ec2.describeInstances();
			List<Reservation> reservations = describeInstancesRequest.getReservations();
			Set<Instance> instances = new HashSet<Instance>();

			RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

			/* TODO: configure to use your AMI, key and security group */
			runInstancesRequest.withImageId("ami-a85731c8").withInstanceType("t2.micro").withMinCount(1).withMaxCount(1)
					.withKeyName("CNV-lab-AWS").withSecurityGroups("launch-wizard-1");
			RunInstancesResult runInstancesResult = LoadBalancer.ec2.runInstances(runInstancesRequest);

			Instance newInstance = runInstancesResult.getReservation().getInstances().get(0);

			String newInstanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
			describeInstancesRequest = LoadBalancer.ec2.describeInstances();
			reservations = describeInstancesRequest.getReservations();
			instances = new HashSet<Instance>();

			for (Reservation reservation : reservations) {
				instances.addAll(reservation.getInstances());
			}

			pendingInstance++;
			// wait for instance to launch
			Thread.sleep(LAUNCH_INSTANCE_OFFSET);

			String dns = "";
			String instanceID = "";
			Instance inst = null;
			for (Reservation reservation : reservations) {
				for (Instance instance : reservation.getInstances()) {
					if (instance.getInstanceId().equals(newInstanceId)) {
						// while (instance.getPublicDnsName().isEmpty()) {}
						System.out.println("DNS: " + instance.getPublicDnsName() + " ID:" + instance.getInstanceId());
						dns = instance.getPublicDnsName();
						instanceID = instance.getInstanceId();
						inst = instance;
						break;
					}
				}
			}

			pendingInstances.put(instanceID, inst);
			
			System.out.println("PENDING INSTANCES SIZE: " + pendingInstances.size()  + " "+pendingInstance);

			// while (newInstance.getPublicDnsName().isEmpty()) {}

			System.out.println("DNS:" + dns + " ID: " + instanceID);

			if (inst != null && !initalHealthCheck(inst)) { // instance terminated
				pendingInstance--;											
				pendingInstances.remove(instanceID);

			} else if (inst != null) {
				// if passed health check, add to current and remove from
				// pending
				LoadBalancer.currentInstances.put(instanceID, inst);
				pendingInstance--;
				pendingInstances.remove(instanceID);
				System.out.println("PENDING INSTANCES SIZE AFTER DELETE: " + pendingInstances.size() + " " + pendingInstance);
				System.out.println("Started a new instance with id " + inst.getInstanceId());
			}
			return inst;

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
		}

		return null;
	}

	public static void removeUnusedInstance() {

		Dimension instanceDimension = new Dimension();
		List<Dimension> dims = new ArrayList<Dimension>();
		instanceDimension.setName("InstanceId");
		dims.add(instanceDimension);
		boolean existsInstancesHighCpu = false;
		int nrInstancesLowCpu = 0;
		boolean canRemoveInstance = false;
		Instance instanceToRemove = null;

		for (Map.Entry<String, Instance> entry : LoadBalancer.currentInstances.entrySet()) {
			Instance instance = entry.getValue();
			String instanceId = entry.getKey();
			String instanceState = instance.getState().getName();

			// used check if instance wasn't started less than 3 minutes ago
			// in order to not remove an instance that just started
			long timeSinceInstanceStart = new Date().getTime() - instance.getLaunchTime().getTime();

			if (instanceState.equals("running")) {
				instanceDimension.setValue(instanceId);
				GetMetricStatisticsRequest request = new GetMetricStatisticsRequest()
						.withStartTime(new Date(new Date().getTime() - METRICS_OFFSET)).withNamespace("AWS/EC2")
						.withPeriod(60).withMetricName("CPUUtilization").withStatistics("Average")
						.withDimensions(instanceDimension).withEndTime(new Date());

				GetMetricStatisticsResult getMetricStatisticsResult = cloudWatch.getMetricStatistics(request);
				List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();

				for (Datapoint dp : datapoints) {
					double cpuAverageInstance = dp.getAverage();
					if (cpuAverageInstance < LOW_CPU_USAGE) {
						nrInstancesLowCpu++;
					} else if (cpuAverageInstance > HIGH_CPU_USAGE) {
						existsInstancesHighCpu = true;
					}
				}
			}

			if ((existsInstancesHighCpu || nrInstancesLowCpu >= 2) && timeSinceInstanceStart > LAUNCH_INSTANCE_OFFSET) {
				canRemoveInstance = true;
				instanceToRemove = instance;
				break;
			}
		}

		if (canRemoveInstance && instanceToRemove != null) {
			LoadBalancer.removeInstanceInfo(instanceToRemove.getInstanceId());
			removeInstance(instanceToRemove);
		} else { // no need to remove any instance
			return;
		}
	}

	public static void removeUnusedInstances() {
		ConcurrentHashMap<String, Instance> currentInstances = LoadBalancer.currentInstances;
		ConcurrentHashMap<String, Integer> currentInstancesRanks = LoadBalancer.currentInstancesRanks;
		int numberInstances = 0;

		for (String instanceID : currentInstancesRanks.keySet()) {
			if (currentInstancesRanks.get(instanceID) != null && currentInstancesRanks.get(instanceID) == 0) {
				numberInstances++;
			}
			if (currentInstancesRanks.get(instanceID) != null && currentInstancesRanks.get(instanceID) == 0 && numberInstances > 2) {
				System.out.println("--------------------------------VOU REMOVER "+ instanceID);
				removeInstance(currentInstances.get(instanceID));
				LoadBalancer.removeInstanceInfo(instanceID);
			}
		}

	}

	public static void removeInstance(Instance instance) {
		TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
		termInstanceReq.withInstanceIds(instance.getInstanceId());
		LoadBalancer.ec2.terminateInstances(termInstanceReq);
		System.out.println("Removi " + instance.getInstanceId());
	}

	public static boolean isAnyInstancePending() {
		return pendingInstance > 0;
	}

	public static boolean initalHealthCheck(Instance instance) {
		String response = new String();
		URLConnection connection;
		URL url;

		try {
			System.out.println("ENTREI no health check");
			url = new URL("http://" + instance.getPublicDnsName() + ":8000/r.html?");
			connection = url.openConnection();
			Scanner s = new Scanner(connection.getInputStream());
			while (s.hasNext()) {
				response += s.next();
			}
			s.close();

			if (response.equals("OK")) {
				return true;
			}

		} catch (IOException e) {
			System.out.println("INITIAL HEALTH_CHECK: Terminating the new instance with id: " + instance.getInstanceId());
			removeInstance(instance);
			return false;
		}

		return true;
	}

	static boolean healthCheck(Instance instance) throws Exception {
		String response = new String();
		URLConnection connection;
		URL url;

		try {
			url = new URL("http://" + instance.getPublicDnsName() + ":8000/r.html?");
			connection = url.openConnection();
			Scanner s = new Scanner(connection.getInputStream());
			while (s.hasNext()) {
				response += s.next();
			}
			s.close();

			if (response.equals("OK")) {
				return true;
			}

		} catch (IOException e) {
			System.out.println("Terminating the new instance with id: " + instance.getInstanceId());
			removeInstance(instance);
			return false;
		}

		return true;
	}

}
