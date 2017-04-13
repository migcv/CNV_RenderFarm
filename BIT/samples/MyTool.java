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
    /*
     * 	StatisticTool -load_store
     */
	private static HashMap<Long, Integer> loadcount = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> storecount = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> fieldloadcount = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> fieldstorecount = new HashMap<Long, Integer>();
    /*
     * 	StatisticTool -branch
     */
    private static HashMap<Long, StatisticsBranch[]> branch_info = new HashMap<Long, StatisticsBranch[]>();
	private static HashMap<Long, Integer> branch_number = new HashMap<Long, Integer>();
	private static HashMap<Long, Integer> branch_pc = new HashMap<Long, Integer>();
	private static HashMap<Long, String> branch_class_name = new HashMap<Long, String>();
	private static HashMap<Long, String> branch_method_name = new HashMap<Long, String>();
    
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
					/*
                     * 	StatisticTool -load_store
                     */
					for (Enumeration instrs = (routine.getInstructionArray()).elements(); instrs.hasMoreElements(); ) {
						Instruction instr = (Instruction) instrs.nextElement();
						int opcode=instr.getOpcode();
						if (opcode == InstructionTable.getfield)
							instr.addBefore("MyTool", "LSFieldCount", new Integer(0));
						else if (opcode == InstructionTable.putfield)
							instr.addBefore("MyTool", "LSFieldCount", new Integer(1));
						else {
							short instr_type = InstructionTable.InstructionTypeTable[opcode];
							if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
								instr.addBefore("MyTool", "LSCount", new Integer(0));
							}
							else if (instr_type == InstructionTable.STORE_INSTRUCTION) {
								instr.addBefore("MyTool", "LSCount", new Integer(1));
							}
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
                /*
                 * 	StatisticTool -load_store
                 */
                ci.addAfter("MyTool", "printLoadStore", ci.getClassName());
                ci.write(out_filename);
            }
        }
        /*
         * 	StatisticTool -branch
         */
        /*int k = 0;
		int total = 0;
		for (int i = 0; i < infilenames.length; i++) {
			String filename = infilenames[i];
			if (filename.endsWith(".class")) {
				String in_filename = file_in.getAbsolutePath() + System.getProperty("file.separator") + filename;
				ClassInfo ci = new ClassInfo(in_filename);

				for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
					Routine routine = (Routine) e.nextElement();
					InstructionArray instructions = routine.getInstructionArray();
					for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
						BasicBlock bb = (BasicBlock) b.nextElement();
						Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
						short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
						if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
							total++;
						}
					}
				}
			}
		}
		for (int i = 0; i < infilenames.length; i++) {
			String filename = infilenames[i];
			if (filename.endsWith(".class")) {
				String in_filename = file_in.getAbsolutePath() + System.getProperty("file.separator") + filename;
				String out_filename = file_out.getAbsolutePath() + System.getProperty("file.separator") + filename;
				ClassInfo ci = new ClassInfo(in_filename);
				for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
					Routine routine = (Routine) e.nextElement();
					routine.addBefore("StatisticsTool", "setBranchMethodName", routine.getMethodName());
					InstructionArray instructions = routine.getInstructionArray();
					for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
						BasicBlock bb = (BasicBlock) b.nextElement();
						Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
						short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
						if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
							instr.addBefore("MyTool", "setBranchPC", new Integer(instr.getOffset()));
							instr.addBefore("MyTool", "updateBranchNumber", new Integer(k));
							instr.addBefore("MyTool", "updateBranchOutcome", "BranchOutcome");
							k++;
						}
					}
				}
				ci.addBefore("MyTool", "setBranchClassName", ci.getClassName());
				ci.addBefore("MyTool", "branchInit", new Integer(total));
				ci.addAfter("MyTool", "printBranch", ci.getClassName());
				ci.write(out_filename);
			}
		}*/
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
			float instr_per_bb = (float) i_count.get(getThreadId()) / (float) b_count.get(getThreadId());
			float instr_per_method = (float) i_count.get(getThreadId()) / (float) m_count.get(getThreadId());
			float bb_per_method = (float) b_count.get(getThreadId()) / (float) m_count.get(getThreadId());
			log.write("Average number of instructions per basic block: " + instr_per_bb + "\n"
					+ "Average number of instructions per method:      " + instr_per_method + "\n"
					+ "Average number of basic blocks per method:      " + bb_per_method + "\n");
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
     * 	StatisticTool -load_store methods
     */
    public static synchronized void printLoadStore(String foo) {
    	try {
			FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
			log.write(foo + " - LOAD_STORE: " + "\n"
					+ "Field load:    " + (fieldloadcount.get(getThreadId()) != null ? fieldloadcount.get(getThreadId()) : "0") + "\n"
					+ "Field store:   " + (fieldstorecount.get(getThreadId()) != null ? fieldstorecount.get(getThreadId()) : "0") + "\n"
					+ "Regular load:  " + (loadcount.get(getThreadId()) != null ? loadcount.get(getThreadId()) : "0") + "\n"
					+ "Regular store: " + (storecount.get(getThreadId()) != null ? storecount.get(getThreadId()) : "0") + "\n");
		
			log.close();
    	} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static synchronized void LSFieldCount(int type) {
		if (type == 0) {
			//fieldloadcount++;
			if(fieldloadcount.get(getThreadId()) != null) {
				fieldloadcount.put(getThreadId(), fieldloadcount.get(getThreadId()) + 1);
	    	} else {
	    		fieldloadcount.put(getThreadId(), 1);
	    	}
		}
		else {
			//fieldstorecount++;
			if(fieldstorecount.get(getThreadId()) != null) {
				fieldstorecount.put(getThreadId(), fieldstorecount.get(getThreadId()) + 1);
	    	} else {
	    		fieldstorecount.put(getThreadId(), 1);
	    	}
		}
	}

    public static synchronized void LSCount(int type) {
		if (type == 0) {
			//loadcount++;
			if(loadcount.get(getThreadId()) != null) {
				loadcount.put(getThreadId(), loadcount.get(getThreadId()) + 1);
	    	} else {
	    		loadcount.put(getThreadId(), 1);
	    	}
		}
		else {
			//storecount++;
			if(storecount.get(getThreadId()) != null) {
				storecount.put(getThreadId(), storecount.get(getThreadId()) + 1);
	    	} else {
	    		storecount.put(getThreadId(), 1);
	    	}
		}
	}
    /*
     * 	StatisticTool -branch methods
     */
    public static synchronized void setBranchClassName(String name) {
		//branch_class_name = name;
    	branch_class_name.put(getThreadId(), name);
	}

    public static synchronized void setBranchMethodName(String name) {
		//branch_method_name = name;
    	branch_method_name.put(getThreadId(), name);
	}

	public static synchronized void setBranchPC(int pc) {
		//branch_pc = pc;
		branch_pc.put(getThreadId(), pc);
	}

	public static synchronized void branchInit(int n) {
		if (branch_info.get(getThreadId()) == null) {
			//branch_info = new StatisticsBranch[n];
			branch_info.put(getThreadId(), new StatisticsBranch[n]);
		}
	}

	public static synchronized void updateBranchNumber(int n) {
		//branch_number = n;
		branch_number.put(getThreadId(), n);
		
		if (branch_info.get(getThreadId())[branch_number.get(getThreadId())] == null) {
			//branch_info[branch_number] = new StatisticsBranch(branch_class_name, branch_method_name, branch_pc);
			branch_info.get(getThreadId())[branch_number.get(getThreadId())] = new StatisticsBranch(
							branch_class_name.get(getThreadId()), branch_method_name.get(getThreadId()), branch_pc.get(getThreadId()));
		}
	}

	public static synchronized void updateBranchOutcome(int br_outcome) {
		if (br_outcome == 0) {
			//branch_info[branch_number].incrNotTaken();
			branch_info.get(getThreadId())[branch_number.get(getThreadId())].incrNotTaken();
		}
		else {
			//branch_info[branch_number].incrTaken();
			branch_info.get(getThreadId())[branch_number.get(getThreadId())].incrTaken();
		}
	}

	public static synchronized void printBranch(String foo) {
		try {
			FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
			log.write(foo + " - BRANCH: " + "\n"
					+ "CLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN\n");
			System.out.println("Started writing!");
			for (int i = 0; i < branch_info.get(getThreadId()).length; i++) {
				if (branch_info.get(getThreadId())[i] != null) {
					//branch_info.get(getThreadId())[i].print();
					log.write(branch_info.get(getThreadId())[i].class_name_ + '\t' + branch_info.get(getThreadId())[i].method_name_ 
							+ '\t' + branch_info.get(getThreadId())[i].pc_ + '\t' + branch_info.get(getThreadId())[i].taken_ + '\t' 
							+ branch_info.get(getThreadId())[i].not_taken_ + '\n');
				}
			}
			System.out.println("Stop writing!");
			log.close();
    	} catch (IOException e) {
			e.printStackTrace();
		}
	}
    /*
     * 	Auxiliar methods
     */
    public static synchronized Long getThreadId() {
    	return Thread.currentThread().getId();
    }
}


