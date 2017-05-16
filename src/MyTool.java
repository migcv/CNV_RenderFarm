

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import BIT.highBIT.*;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class MyTool {
    /*
     *  ICount
     */
    private static ConcurrentHashMap<Long, Long> i_count = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, Long> b_count = new ConcurrentHashMap<Long, Long>();
    /*
     *  StatisticTool -load_store
     */
    private static ConcurrentHashMap<Long, Long> fieldloadcount = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, Long> fieldstorecount = new ConcurrentHashMap<Long, Long>();
    
    private static ConcurrentHashMap<Long, Map<String, String>> request = new ConcurrentHashMap<Long, Map<String, String>>();
    
    private static AmazonDynamoDBClient dynamoDB;
    
    private static String tableName = "mss";

    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        File file_out = new File(argv[1]);

        String infilenames[] = file_in.list();
        for (int i = 0; i < infilenames.length; i++) {
            String filename = infilenames[i];
            //if (filename.equals("Main.class") || filename.equals("Camera.class") || filename.equals("Matrix.class") || filename.equals("Vector.class")) {
            if (filename.equals("Main.class") || filename.equals("RayTracer.class") || filename.equals("Matrix.class") || filename.equals("Vector.class")) {
		System.out.println("filename-> " + filename);
                // create class info object
                String in_filename = file_in.getAbsolutePath() + System.getProperty("file.separator") + filename;
                String out_filename = file_out.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    /*
                     *  ICount
                     */
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();

                        int loadCounter = 0;
                        for(int address = bb.getStartAddress(); address <= bb.getEndAddress(); address += 2) {
                            InstructionArray instructionArray = routine.getInstructionArray();
                            Instruction instruction = instructionArray.elementAt(address);
                            int opcode = instruction.getOpcode();
                            if (opcode == InstructionTable.getfield){
                                loadCounter++;
                                instruction.addBefore("MyTool", "LSFieldCount", new Integer(0));
                            }
                            else if (opcode == InstructionTable.putfield){
                                instruction.addBefore("MyTool", "LSFieldCount", new Integer(1));
                            }

                        }

                        // only instrument a basic block if it has 10 or more load instructions
                        //System.out.println(bb.getEndAddress() - bb.getStartAddress());
                        //if((bb.getEndAddress() - bb.getStartAddress()) > 3)
                    	//if(loadCounter >= 10)
                        if(loadCounter >= 7)
                            bb.addBefore("MyTool", "count", new Integer(bb.size()));
                    }

                }
                /*
                 *  Write on File
                 */
                //ci.addAfter("MyTool", "printICount", ci.getClassName());
                //ci.addAfter("MyTool", "printLoadStore", ci.getClassName());
                /*
                 * Write on DataBase
                 */
                ci.addAfter("MyTool", "writeDB",  ci.getClassName());
                ci.write(out_filename);
            }
        }
    }
    /*
     *  ICount methods
     */
    public static synchronized void printICount(String foo) {
        try {
            FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
            log.write(foo + " - ICOUNT: " + "\n"
                    + "Number of basic blocks:      " + (b_count.get(getThreadId()) != null ? b_count.get(getThreadId()) : "0") + "\n"
                    + "Number of instructions:      " + (i_count.get(getThreadId()) != null ? i_count.get(getThreadId()) : "0") + "\n");
            float instr_per_bb = 0;
            if(i_count.get(getThreadId()) != null && b_count.get(getThreadId()) != null){
                instr_per_bb = (float) i_count.get(getThreadId()) / (float) b_count.get(getThreadId());
            }
            log.write("Average number of instructions per basic block: " + instr_per_bb + "\n");
            System.out.println("ICOUNT: " + "\n"
                    + "Number of basic blocks:      " + (b_count.get(getThreadId()) != null ? b_count.get(getThreadId()) : "0") + "\n"
                    + "Number of instructions:      " + (i_count.get(getThreadId()) != null ? i_count.get(getThreadId()) : "0") + "\n"
                    + "Average number of instructions per basic block: " + instr_per_bb + "\n");
            log.close();

            //reset hashmaps for next request
            i_count.put(getThreadId(), new Long(0));
            b_count.put(getThreadId(), new Long(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void count(int incr) {
        if(fieldloadcount.get(getThreadId()) != null && fieldloadcount.get(getThreadId()) > 50000) {


            if(i_count.get(getThreadId()) != null && b_count.get(getThreadId()) != null) {
                i_count.put(getThreadId(), i_count.get(getThreadId()) + incr);
                b_count.put(getThreadId(), b_count.get(getThreadId()) + 1);
            } else {
                i_count.put(getThreadId(), new Long(incr));
                b_count.put(getThreadId(), new Long(1));
            }
        }
    }
    /*
     *  StatisticTool -load_store methods
     */
    public static synchronized void printLoadStore(String foo) {
        try {
            FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
            log.write(foo + " - LOAD_STORE: " + "\n"
                    + "Field load:    " + (fieldloadcount.get(getThreadId()) != null ? fieldloadcount.get(getThreadId()) : "0") + "\n"
                    + "Field store:  " + (fieldstorecount.get(getThreadId()) != null ? fieldstorecount.get(getThreadId()) : "0") + "\n");

            System.out.println("LOAD_STORE: " + "\n"
                    + "Field load:    " + (fieldloadcount.get(getThreadId()) != null ? fieldloadcount.get(getThreadId()) : "0") + "\n"
                    + "Field store:  " + (fieldstorecount.get(getThreadId()) != null ? fieldstorecount.get(getThreadId()) : "0") + "\n");

            //reset hashmaps for next request
            fieldloadcount.put(getThreadId(), new Long(0));
            fieldstorecount.put(getThreadId(), new Long(0));
            log.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void LSFieldCount(int type) {
        if (type == 0) {
            if(fieldloadcount.get(getThreadId()) != null) {
                fieldloadcount.put(getThreadId(), fieldloadcount.get(getThreadId()) + 1);
            } else {
                fieldloadcount.put(getThreadId(), new Long(1));
            }
        }
        else {
            if(fieldstorecount.get(getThreadId()) != null) {
                fieldstorecount.put(getThreadId(), fieldstorecount.get(getThreadId()) + 1);
            } else {
                fieldstorecount.put(getThreadId(), new Long(1));
            }
        }
    }

    /*
     *  Auxiliar methods
     */
    public static synchronized void writeDB(String foo) {
    	init();
    	if(request.get(getThreadId()) == null) {
    		System.out.println("NULL");
    	}
    	System.out.println("THREAD > " + getThreadId() + " | Writting on " + tableName);
    	Map<String, AttributeValue> item = newItem(request.get(getThreadId()).get("f"), request.get(getThreadId()).get("sc"), request.get(getThreadId()).get("sr"), request.get(getThreadId()).get("wc"), request.get(getThreadId()).get("wr"), request.get(getThreadId()).get("coff"), request.get(getThreadId()).get("roff"), 2);
        PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
        PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
        

        fieldloadcount.put(getThreadId(), new Long(0));
        fieldstorecount.put(getThreadId(), new Long(0));
        i_count.put(getThreadId(), new Long(0));
        b_count.put(getThreadId(), new Long(0));
        
        System.out.println("Result: " + putItemResult);
    }
    
    public static synchronized Long getThreadId() {
        return Thread.currentThread().getId();
    }
    
    public static synchronized void writeRequest(Map<String, String> r) {
    	System.out.println("THREAD > " + getThreadId() + " | " + r.get("f"));
    	request.put(getThreadId(), r);
    }
    
    private static void init() {
        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
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
        dynamoDB = new AmazonDynamoDBClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        dynamoDB.setRegion(usWest2);
    }
    
    private static Map<String, AttributeValue> newItem(String filename, String scols, String srows, String wcols, String wrows, String coff, String roff, int rank) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("id", new AttributeValue(UUID.randomUUID().toString()));
        item.put("filename", new AttributeValue(filename));
        item.put("scols", new AttributeValue(scols));
        item.put("srows", new AttributeValue(srows));
        item.put("wcols", new AttributeValue(wcols));
        item.put("wrows", new AttributeValue(wrows));
        item.put("coff", new AttributeValue(coff));
        item.put("roff", new AttributeValue(roff));
        
        item.put("rank", new AttributeValue().withN(Integer.toString(rank)));

        return item;
    }
}

