JAVAC = javac
JFLAGS = 

all:
	$(JAVAC) $(JFLAGS) src/*.java src/raytracer/*.java src/raytracer/pigments/*.java src/raytracer/shapes/*.java
	#$(JAVAC) $(JFLAGS) BIT/samples/MyTool.java

clean:
	$(RM) src/raytracer/*.class src/raytracer/pigments/*.class src/raytracer/shapes/*.class
	$(RM) src/raytracerBIT/raytracer/*.class

run:
	java MyTool src/raytracer/ src/raytracerBIT/raytracer
	java WebServer
