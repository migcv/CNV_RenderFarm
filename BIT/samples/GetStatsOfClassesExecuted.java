import java.io.*;
import java.util.*;
import BIT.highBIT.*;
import BIT.lowBIT.*;

public class GetStatsOfClassesExecuted {
	public static double grand_total = 0;
	public static double const_total = 0;
	public static double field_total = 0;
	public static double interface_total = 0;
	public static double methods_total = 0;
	public static double bytecodes_total = 0;
	public static double bytecodes_partial=0;

        public static final String usage = "Usage: java GetSizeOfClassesExecuted ." 
	    + "\nThis program finds all of the class files in this directory and prints"
	    + "\nout the size of each (as well as the grand total) to stderr.\n";

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
	System.err.println("Totals: class size: " + grand_total + " const: " + const_total+ 
							" fields: "+ field_total+ " interfaces: " + interface_total + " methods: " + methods_total + 
						    " bytecodes: " +  bytecodes_total);


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
                        ClassInfo ci = new ClassInfo(name); /* BIT/highBIT call that reads/processes the class */
					ClassFile cf = ci.getClassFile(); /* BIT/lowBIT call that returns the class file (BIT format)*/

					grand_total += cf.size();
					const_total += cf.constant_pool_count  ;
					field_total += cf.field_count;
					interface_total += cf.interface_count;
					methods_total += cf.methods_count;

					bytecodes_partial=0;
					for (int i_attr=0; i<cf.attributes_count; i++) {
						Attribute_Info attr = (Attribute_Info) cf.attributes[i_attr];
						if (attr instanceof Code_Attribute)
						{
//							System.out.println( "code-size "+((Code_Attribute)attr).code_length);
							bytecodes_partial+= ((Code_Attribute)attr).code_length;
						}
					}
					bytecodes_total += bytecodes_partial;



					/* getClassName is in BIT/highBIT/ClassInfo */
					System.err.println(ci.getClassName() + " size: " + cf.size() + " const: " + cf.constant_pool_count + 
							" fields: "+ cf.field_count+ " interfaces: " + cf.interface_count + " methods: " + cf.methods_count + 
						    " bytecodes: " +  bytecodes_partial);
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
		
