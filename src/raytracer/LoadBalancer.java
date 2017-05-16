package raytracer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
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
	}

	public static void main(String[] args) throws Exception {

		System.out.println("===========================================");
		System.out.println("Welcome to the AWS Java SDK!");
		System.out.println("===========================================");

		init();

		try {
			DescribeAvailabilityZonesResult availabilityZonesResult = ec2.describeAvailabilityZones();
			System.out.println("You have access to " + availabilityZonesResult.getAvailabilityZones().size()
					+ " Availability Zones.");
			/*
			 * using AWS Ireland. TODO: Pick the zone where you have your AMI,
			 * sec group and keys
			 */
			DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
			List<Reservation> reservations = describeInstancesRequest.getReservations();
			Set<Instance> instances = new HashSet<Instance>();

			for (Reservation reservation : reservations) {
				instances.addAll(reservation.getInstances());
			}

			System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
			System.out.println("Starting a new instance.");
			RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

			/* TODO: configure to use your AMI, key and security group */
			runInstancesRequest.withImageId("ami-bf73e4df")
							   .withInstanceType("t2.micro")
							   .withMinCount(1)
							   .withMaxCount(1)
							   .withKeyName("CNV-lab-AWS")
							   .withSecurityGroups("CNV-ssh+http");
			RunInstancesResult runInstancesResult = ec2.runInstances(runInstancesRequest);
			String newInstanceId = runInstancesResult.getReservation().getInstances().get(0).getInstanceId();
			describeInstancesRequest = ec2.describeInstances();
			reservations = describeInstancesRequest.getReservations();
			instances = new HashSet<Instance>();

			for (Reservation reservation : reservations) {
				instances.addAll(reservation.getInstances());
			}

			System.out.println("You have " + instances.size() + " Amazon EC2 instance(s) running.");
			System.out.println("Waiting 1 minute. See your instance in the AWS console...");
			Thread.sleep(60000);
			System.out.println("Terminating the instance.");
			TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
			termInstanceReq.withInstanceIds(newInstanceId);
			ec2.terminateInstances(termInstanceReq);

		} catch (AmazonServiceException ase) {
			System.out.println("Caught Exception: " + ase.getMessage());
			System.out.println("Reponse Status Code: " + ase.getStatusCode());
			System.out.println("Error Code: " + ase.getErrorCode());
			System.out.println("Request ID: " + ase.getRequestId());
		}
	}
}
