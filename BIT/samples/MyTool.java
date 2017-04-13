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
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;

import BIT.highBIT.*;


public class MyTool {
    private static PrintStream out = null;
    
    /*
     * 	ICount
     */
    private static HashMap<Long, Long> i_count = new HashMap<Long, Long>();
    private static HashMap<Long, Long> b_count = new HashMap<Long, Long>();
    private static HashMap<Long, Long> m_count = new HashMap<Long, Long>();
    
    /*
     * 	StatisticTool -alloc
     */
	private static HashMap<Long, Integer> newcount = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> newarraycount = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> anewarraycount = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> multianewarraycount = new HashMap<Long, Integer>();
    
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        File file_out = new File(argv[1]);
        
        String infilenames[] = file_in.list();
        
        for (int i = 0; i < infilenames.length; i++) {
            String filename = infilenames[i];
            if (filename.endsWith(".class")) {
				// create class info object
            	String in_filename = file_in.getAbsolutePath() + System.getProperty("file.separator") + filename;
				String out_filename = file_out.getAbsolutePath() + System.getProperty("file.separator") + filename;
				ClassInfo ci = new ClassInfo(in_filename);
				
                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    
                    /*
                     * 	ICount
                     */
					routine.addBefore("MyTool", "mcount", new Integer(1));
                    
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("MyTool", "count", new Integer(bb.size()));
                    }
                    
                    /*
                     * 	StatisticTool -alloc
                     */
                    InstructionArray instructions = routine.getInstructionArray();
	  
					for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements(); ) {
						Instruction instr = (Instruction) instrs.nextElement();
						int opcode=instr.getOpcode();
						if ((opcode==InstructionTable.NEW) ||
							(opcode==InstructionTable.newarray) ||
							(opcode==InstructionTable.anewarray) ||
							(opcode==InstructionTable.multianewarray)) {
							instr.addBefore("MyTool", "allocCount", new Integer(opcode));
						}
					}
                    
                }
                /*
                 * 	ICount
                 */
                ci.addAfter("MyTool", "printICount", ci.getClassName());
                /*
                 * 	StatisticTool -alloc
                 */
                ci.addAfter("MyTool", "printAlloc", ci.getClassName());
                
                ci.write(out_filename);
            }
        }
    }    
    /*
     * 	ICount methods
     */
    public static synchronized void printICount(String foo) {
    	try {
    		FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
			log.write(foo + " - ICOUNT: " + "\n"
					+ "Number of methods:           " + m_count.get(getThreadId()) + "\n"
					+ "Number of basic blocks:      " + b_count.get(getThreadId()) + "\n" 
					+ "Number of instructions:      " + i_count.get(getThreadId()) + "\n");
			log.close();
			i_count.put(getThreadId(), new Long(0));
    		b_count.put(getThreadId(), new Long(0));
    		m_count.put(getThreadId(), new Long(0));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static synchronized void count(int incr) {
    	if(i_count.get(getThreadId()) != null && b_count.get(getThreadId()) != null) {
    		i_count.put(getThreadId(), i_count.get(getThreadId()) + incr);
    		b_count.put(getThreadId(), b_count.get(getThreadId()) + 1);
    	} else {
    		i_count.put(getThreadId(), new Long(incr));
    		b_count.put(getThreadId(), new Long(1));
    	}
    }

    public static synchronized void mcount(int incr) {
    	if(m_count.get(getThreadId()) != null) {
    		m_count.put(getThreadId(), m_count.get(getThreadId()) + incr);
    	} else {
    		m_count.put(getThreadId(), new Long(incr));
    	}
    }
    /*
     * 	StatisticTool -alloc methods
     */
    public static synchronized void printAlloc(String foo) {
    	try {
			FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
			log.write(foo + " - ALLOC: " + "\n"
					+ "new:            " + (newcount.get(getThreadId()) != null ? newcount.get(getThreadId()) : "0") + "\n"
					+ "newarray:       " + (newarraycount.get(getThreadId()) != null ? newarraycount.get(getThreadId()) : "0") + "\n"
					+ "anewarray:      " + (anewarraycount.get(getThreadId()) != null ? anewarraycount.get(getThreadId()) : "0") + "\n"
					+ "multianewarray: " + (multianewarraycount.get(getThreadId()) != null ? multianewarraycount.get(getThreadId()) : "0") + "\n");
			log.close();
			
			newcount.put(getThreadId(), 0);
			newarraycount.put(getThreadId(), 0);
			anewarraycount.put(getThreadId(), 0);
			multianewarraycount.put(getThreadId(), 0);
    	} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    public static synchronized void allocCount(int type) {
    	
		switch(type) {
			case InstructionTable.NEW:
				//newcount++;
				if(newcount.get(getThreadId()) != null) {
					newcount.put(getThreadId(), newcount.get(getThreadId()) + 1);
		    	} else {
		    		newcount.put(getThreadId(), 1);
		    	}
				break;
			case InstructionTable.newarray:
				//newarraycount++;
				if(newarraycount.get(getThreadId()) != null) {
					newarraycount.put(getThreadId(), newarraycount.get(getThreadId()) + 1);
		    	} else {
		    		newarraycount.put(getThreadId(), 1);
		    	}
				break;
			case InstructionTable.anewarray:
				//anewarraycount++;
				if(anewarraycount.get(getThreadId()) != null) {
					anewarraycount.put(getThreadId(), anewarraycount.get(getThreadId()) + 1);
		    	} else {
		    		anewarraycount.put(getThreadId(), 1);
		    	}
				break;
			case InstructionTable.multianewarray:
				//multianewarraycount++;
				if(multianewarraycount.get(getThreadId()) != null) {
					multianewarraycount.put(getThreadId(), multianewarraycount.get(getThreadId()) + 1);
		    	} else {
		    		multianewarraycount.put(getThreadId(), 1);
		    	}
				break;
		}
	}
    /*
     * 	Auxiliar methods
     */
    public static synchronized Long getThreadId() {
    	return Thread.currentThread().getId();
    }
}


