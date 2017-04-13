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
    //private static int i_count = 0, b_count = 0, m_count = 0;
    private static HashMap<Long, Integer> i_count = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> b_count = new HashMap<Long, Integer>();
    private static HashMap<Long, Integer> m_count = new HashMap<Long, Integer>();
    private static HashMap<Long, String> request = new HashMap<Long, String>();
    
    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();
        
        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            if (infilename.endsWith(".class")) {
				// create class info object
				ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);
				
                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
					routine.addBefore("MyTool", "mcount", new Integer(1));
                    
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("MyTool", "count", new Integer(bb.size()));
                    }
                }
                ci.addAfter("MyTool", "printICount", ci.getClassName());
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }
    
    public static synchronized void printICount(String foo) {
        //System.out.println(i_count + " instructions in " + b_count + " basic blocks were executed in " + m_count + " methods.");
    	try {
    		FileWriter log = new FileWriter("log/" + getThreadId() +".txt", true);
			log.write(foo + ": " + i_count.get(getThreadId()) + " instructions in " + 
					b_count.get(getThreadId()) + " basic blocks were executed in " + m_count.get(getThreadId()) + " methods.\n");
			log.close();
			//i_count = 0;
			//b_count = 0;
			//m_count = 0;
			i_count.put(getThreadId(), 0);
    		b_count.put(getThreadId(), 0);
    		m_count.put(getThreadId(), 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    public static synchronized void count(int incr) {
        //i_count += incr;
        //b_count++;
    	if(i_count.get(getThreadId()) != null && b_count.get(getThreadId()) != null) {
    		i_count.put(getThreadId(), i_count.get(getThreadId()) + incr);
    		b_count.put(getThreadId(), b_count.get(getThreadId()) + 1);
    	} else {
    		i_count.put(getThreadId(), incr);
    		b_count.put(getThreadId(), 1);
    	}
    }

    public static synchronized void mcount(int incr) {
		//m_count++;
    	if(m_count.get(getThreadId()) != null) {
    		m_count.put(getThreadId(), m_count.get(getThreadId()) + incr);
    	} else {
    		m_count.put(getThreadId(), incr);
    	}
    }
    
    public static synchronized void setRequest(String request) {
    	MyTool.request.put(getThreadId(), request);
    }
    
    public static synchronized Long getThreadId() {
    	return Thread.currentThread().getId();
    }
}


