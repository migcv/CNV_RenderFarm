import BIT.highBIT.*;
import BIT.lowBIT.*;
import java.io.File;
/*import java.util.Enumeration;
import java.util.Vector;*/
import java.util.*;

class Branch {
	public int taken;
	public int not_taken;
}

public class BranchPrediction {
	static Hashtable branch = null;
	static int pc = 0;
	
	public static void main(String argv[]) {
		String infilename = new String(argv[0]);
		String outfilename = new String(argv[1]);
		ClassInfo ci = new ClassInfo(infilename);
		Vector routines = ci.getRoutines();

		for (Enumeration e=routines.elements();e.hasMoreElements(); ){
				Routine routine = (Routine) e.nextElement();
				Instruction[] instructions = routine.getInstructions();
				for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
					BasicBlock bb = (BasicBlock) b.nextElement();
					Instruction instr = (Instruction)instructions[bb.getEndAddress()];
					short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
					if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
						instr.addBefore("BranchPrediction", "Offset", new Integer(instr.getOffset()));
						instr.addBefore("BranchPrediction", "Branch", new String("BranchOutcome"));
					}
				}
				String method = new String(routine.getMethodName());
				routine.addBefore("BranchPrediction", "EnterMethod", method);
				routine.addAfter("BranchPrediction", "LeaveMethod", method);
		}
		ci.write(outfilename);
	}


	public static void EnterMethod(String s) {
		System.out.println("method: " + s);
		branch = new Hashtable();
	}
	
	public static void LeaveMethod(String s) {
		System.out.println("stat for method: " + s);
		for (Enumeration e = branch.keys(); e.hasMoreElements(); ) {
			Integer key = (Integer) e.nextElement();
			Branch b = (Branch) branch.get(key);
			int total = b.taken + b.not_taken;
			System.out.print("PC: " + key);
			System.out.print("\t\ttaken: " + b.taken + " (" + b.taken*100/total + "%)");
			System.out.println("\t\tnot taken: " + b.not_taken + " (" + b.not_taken*100/total + "%)");
		}
	}

	public static void Offset(int offset) {
		pc = offset;
	}

	public static void Branch(int brOutcome) {
		Integer n = new Integer(pc);
		Branch b = (Branch) branch.get(n);
		if (b == null) {
			b = new Branch();
			branch.put(n,b);
		}
		if (brOutcome == 0)
			b.taken++;
		else
			b.not_taken++;
	}
}