package raytracer;


import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Main {
	private static final String USAGE = "Usage:\n"+
			"java -cp src raytracer.Main infile bmpfile scols srows wcols wrows coff roff [-options]\n"+
			"\n"+
			"    where:\n"+
			"        infile    - input file name\n"+
			"        bmpfile   - bmp output file name\n"+
			"        scols     - scene width (in pixels)\n"+
			"        srows    - scene height (in pixels)\n"+
			"        wcols     - window width (in pixels, max scols)\n"+
			"        wrows    - window height (in pixels, max srows)\n"+
			"        coff     - window column offset (in pixels, max: scols - wcols)\n"+
			"        roff    - windows row offset (in pixels, max: srows - wrows)\n"+
//			"        -test     - run in test mode (see below)\n"+
//			"        -noshadow - don't compute shadows\n"+
//			"        -noreflec - don't do reflections\n"+
//			"        -notrans  - don't do transparency\n"+
			"        -aa       - use anti-aliasing (~4x slower)\n"+
			"        -multi    - use multi-threading (good for large, anti-aliased images)";
//			"        -nocap    - cylinders and cones are infinite";

	public static boolean DEBUG = false;
	public static boolean ANTI_ALIAS = false;
	public static boolean MULTI_THREAD = false;


	private static void printUsage() {
		System.out.println(USAGE);
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Executing main!");
		if(args.length < 8) {
			printUsage();
			System.exit(0);
		}

		// required arguments
		File inFile = new File(args[0]);
		File outFile = new File(args[1]);
		int scols = Integer.parseInt(args[2]);
		int srows = Integer.parseInt(args[3]);
		int wcols = Integer.parseInt(args[4]);
		int wrows = Integer.parseInt(args[5]);
		int coff = Integer.parseInt(args[6]);
		int roff = -Integer.parseInt(args[7]);
		// optional arguments
		int i = 0;
		for(String arg: args) {
			if(i++ < 8) continue;
			if("-test".equals(arg)) {
				DEBUG = true;
			} else if("-aa".equals(arg)) {
				ANTI_ALIAS = true;
			} else if("-multi".equals(arg)) {
				MULTI_THREAD = true;
			} else {
				System.out.print("Unrecognized option: '" + arg + "' ignored.");
			}
		}

		RayTracer rayTracer = new RayTracer(scols, srows, wcols, wrows, coff, roff);
		rayTracer.readScene(inFile);
		if(DEBUG) {
			while(true) {
				Scanner scanner = new Scanner(System.in);
				System.out.println("Input column and row of pixel (relative to upper left corner):");
				int col = scanner.nextInt();
				int row = scanner.nextInt();
				rayTracer.getPixelColor(col, row);
			}
		} else {
			rayTracer.draw(outFile);
		}
	}
}
