

import java.io.*;
import java.util.*;
import BIT.highBIT.*;
import BIT.lowBIT.*;

public class AnalyzeMethods {
	public static double grand_total = 0;
        public static final String usage = "Usage: java AnalyzeMethods ." 
	    + "\nThis program finds all of the class files in this directory and prints out the"
	    + "\nname of all of the methods in each and the access_flags of the method to stderr.\n";

	public static void main(String args[]) {

    	   try {
		if (args.length < 1) {
		    System.err.println(usage);
		    System.exit(-1);
	        }
	      	File file_in = new File(args[0]);
		String tmppath = new String(file_in.getAbsolutePath());
                String p = new String(tmppath.substring(0, tmppath.length() - 2));
		processFiles(file_in, p); 

	   } catch (Exception e) {
      		System.err.println("Exception! in main method: " + e);
      		System.err.println(e.getMessage());
		e.printStackTrace();
		System.exit(-1);
    	   }
  	}

  	public static void processFiles(File fi, String tmppath) {

	   /* recursive method that finds all class files under this directory */
    	   try {
		String ifnames[] = fi.list();
	      	for (int i = 0; i < ifnames.length; i++) {

			String tmpstr = new String(tmppath+"/"+ifnames[i]);
                        File file_tmp = new File(tmpstr);
        		if (file_tmp.isDirectory() == true) { /* search this directory for class files */
				/* if (file_tmp.isDirectory()) would have worked above as well */
                                //tmppath = new String(file_tmp.getAbsolutePath());
								//processFiles(file_tmp, tmppath);

			} else { /* see if this is a class file and if so, process it */
			
			  	String name = new String(tmppath + "/" + ifnames[i]);

        		  	if (name.endsWith(".class")) {
					/* BIT/highBIT/ClassInfo */
                                	ClassInfo ci = new ClassInfo(name); /* read & process the class */

					/* BIT/lowBIT/ClassFile */
					ClassFile cf = ci.getClassFile(); /* returns the class file in the BIT representation*/

					short super_class_index = cf.getSuperClassIndex(); 

					/* Type Cp_Info can be found in BIT/lowBIT/Cp_Info */
					Cp_Info[] cpool = ci.getConstantPool(); /* ci is type BIT/highBIT/ClassInfo */

					/* 
					 * get the element in the constant pool at super_class_index 
					 * its type is BIT/lowBIT/CONSTANT_Class_Info
					 */
					CONSTANT_Class_Info tmp_class_info = (CONSTANT_Class_Info) cpool[super_class_index];
					/* get the name index from the CONSTANT_Class_Info element */
					int name_index = tmp_class_info.name_index;

					/* 
					 * get the element in the constant pool at name_index
					 * its type is BIT/lowBIT/CONSTANT_Utf8_Info
					 */
					CONSTANT_Utf8_Info tmp_utf8_info = (CONSTANT_Utf8_Info) cpool[name_index];

					/* convert the utf8 object to a string */
					String supername = new String(tmp_utf8_info.bytes);
					System.err.println("\nsuper class name: " + supername);


					/* java.util.Vector - a Java library (look it up on the web) impl. of a dynamic array */
					Vector routines = ci.getRoutines(); /* BIT/highBIT/ClassInfo call that returns a vector
									     * containing all of the methods in the class */

					/* java.util.Enumeration - std way to walk through a vector */
					for (Enumeration e=routines.elements(); e.hasMoreElements();) {
					    Routine routine = (Routine) e.nextElement(); /* nextElement returns type Object
											  * hence, we need the cast */
					    /* see BIT/highBIT/Routine for all the ways to manipulate a routine */
					    
					    System.err.println("class: " + routine.getClassName() + " method: " 
						+ routine.getMethodName() 
						+ " type: " + routine.getDescriptor());

					    /* see BIT/lowBIT/Method_Info */
					    Method_Info meth = routine.getMethodInfo();

					    System.err.println("\tAccess_flags: " + meth.access_flags);
				        }
			  	}
			}
		}
		return;
	   } catch(Exception e) {
      		System.err.println("Exception! in processFiles: " + e);
      		System.err.println(e.getMessage());
		e.printStackTrace();
		System.exit(-1);
	   }
	}
}
		
