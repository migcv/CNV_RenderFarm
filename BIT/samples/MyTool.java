/* ICount.java
 * Sample program using BIT -- counts the number of instructions executed.
 *
 * Copyright (c) 1997, The Regents of the University of Colorado. All
 * Rights Reserved.
 * 
 * Permission to use and copy this software and its documentation for
 * NON-COMMERCIAL purposes and without fee is hereby granted provided
 * that this copyright notice appears in all copies. If you wish to use
 * or wish to have others use BIT for commercial purposes please contact,
 * Stephen V. O'Neil, Director, Office of Technology Transfer at the
 * University of Colorado at Boulder (303) 492-5647.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import BIT.highBIT.*;

public class MyTool {
    /*
     *  ICount
     */
    private static ConcurrentHashMap<Long, Long> i_count = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, Long> b_count = new ConcurrentHashMap<Long, Long>();
    /*
     *  StatisticTool -load_store
     */
    private static ConcurrentHashMap<Long, Long> loadcount = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, Long> storecount = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, Long> fieldloadcount = new ConcurrentHashMap<Long, Long>();
    private static ConcurrentHashMap<Long, Long> fieldstorecount = new ConcurrentHashMap<Long, Long>();

    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        File file_out = new File(argv[1]);

        String infilenames[] = file_in.list();
        for (int i = 0; i < infilenames.length; i++) {
            String filename = infilenames[i];
            //if (filename.equals("Main.class") || filename.equals("Camera.class") || filename.equals("Matrix.class") || filename.equals("Vector.class")) {
            if (filename.equals("Main.class") || filename.equals("RayTracer.class")) {
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
                        for(int address = bb.getStartAddress(); address <= bb.getEndAddress(); address++) {
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
                        //if(loadCounter >= 10)
                        //if(loadCounter >= 6)
                        System.out.println(bb.getEndAddress() - bb.getStartAddress());
                        if((bb.getEndAddress() - bb.getStartAddress()) > 2)
                            bb.addBefore("MyTool", "count", new Integer(bb.size()));
                            
                        
                    }

                }
                /*
                 *  ICount
                 */
                ci.addAfter("MyTool", "printICount", ci.getClassName());
                /*
                 *  StatisticTool -load_store
                 */
                ci.addAfter("MyTool", "printLoadStore", ci.getClassName());
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
    public static synchronized Long getThreadId() {
        return Thread.currentThread().getId();
    }
}

