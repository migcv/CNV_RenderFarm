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
import sun.awt.image.ImageWatched;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutoScaler {

    public final static int HIGH_CPU_USAGE = 80;
    public final static int LOW_CPU_USAGE = 20;
    public final static long METRICS_OFFSET = 24 * 60 * 1000 * 60; // to get metrics within the last 24h
    public final static long LAUNCH_INSTANCE_OFFSET = 3 * 1000 * 60; // 3 min for instance launch time offset
    public final static long UNHEALTHY_THRESHOLD = 3;

    static AmazonCloudWatch cloudWatch;

    public static ConcurrentHashMap<String, Instance> pendingInstances = new ConcurrentHashMap<>();

    private static void init() throws Exception {

        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion("us-west-2").withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
    }

    public static void main(String[] args) throws Exception {
        init();
     }

    public static Instance startInstance() throws InterruptedException {

        try {
            System.out.println("Starting a new instance.");
            RunInstancesRequest runInstancesRequest =
                    new RunInstancesRequest();

            /* TODO: configure to use your AMI, key and security group */
            runInstancesRequest.withImageId("ami-b82d4bd8")
                    .withInstanceType("t2.micro")
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withKeyName("CNV-lab-AWS")
                    .withSecurityGroups("launch-wizard-1");
            RunInstancesResult runInstancesResult =  LoadBalancer.ec2.runInstances(runInstancesRequest);

            Instance newInstance = runInstancesResult.getReservation().getInstances().get(0);
            pendingInstances.put(newInstance.getInstanceId(),newInstance);

            // wait for instance to launch
            Thread.sleep(LAUNCH_INSTANCE_OFFSET);
            while (newInstance.getPublicDnsName().isEmpty()) {}

            if(!healthCheck(newInstance)) { // instance terminated
                pendingInstances.remove(newInstance);
                return null;
            }

            // if passed health check, add to current and remove from pending
            LoadBalancer.currentInstances.put(newInstance.getInstanceId(),newInstance);
            pendingInstances.remove(newInstance);

            System.out.println("Started a new instance with id " + newInstance.getInstanceId());

            return newInstance;

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
                        .withStartTime(new Date(new Date().getTime() - METRICS_OFFSET))
                        .withNamespace("AWS/EC2")
                        .withPeriod(60)
                        .withMetricName("CPUUtilization")
                        .withStatistics("Average")
                        .withDimensions(instanceDimension)
                        .withEndTime(new Date());

                GetMetricStatisticsResult getMetricStatisticsResult = cloudWatch.getMetricStatistics(request);
                List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();

                for (Datapoint dp : datapoints) {
                    double cpuAverageInstance = dp.getAverage();
                    if(cpuAverageInstance < LOW_CPU_USAGE) {
                        nrInstancesLowCpu++;
                    } else if(cpuAverageInstance > HIGH_CPU_USAGE) {
                        existsInstancesHighCpu = true;
                    }
                }
            }

            if((existsInstancesHighCpu || nrInstancesLowCpu >= 2) && timeSinceInstanceStart > LAUNCH_INSTANCE_OFFSET) {
                canRemoveInstance = true;
                instanceToRemove = instance;
                break;
            }
        }

        if(canRemoveInstance) {
            removeInstance(instanceToRemove);
        } else { // no need to remove any instance
            return;
        }

    }

    public static void removeInstance(Instance instance) {
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instance.getInstanceId());
        LoadBalancer.ec2.terminateInstances(termInstanceReq);
        LoadBalancer.currentInstances.remove(instance.getInstanceId());
    }

    public boolean isAnyInstancePending() {
        return pendingInstances.size() > 0;
    }


    public static boolean healthCheck(Instance instance) {
        String response = new String();
        int healthy = 0;
        int unhealthy = 0;
        URLConnection connection;
        URL url;

        while (healthy < 4) {
            try {
                url = new URL("http://" + instance.getPublicDnsName() + ":8000/r.html?");
                connection = url.openConnection();
                Scanner s = new Scanner(connection.getInputStream());
                while (s.hasNext()) {
                    response += s.next();
                }
                s.close();

                if (response.equals("OK")) {
                    unhealthy = 0;
                    healthy += 1;
                    response = "";
                }

            } catch (IOException e) {
                healthy = 0;
                unhealthy += 1;
                if (unhealthy == UNHEALTHY_THRESHOLD) {
                    System.out.println("Terminating the new instance with id: " + instance.getInstanceId());
                    removeInstance(instance);
                    return false;
                }
            }
        }
        return true;
    }

}
