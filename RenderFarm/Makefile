JAVAC = javac
JFLAGS =

all:
	$(JAVAC) $(JFLAGS) src/worker/*.java src/raytracer/*.java src/raytracer/pigments/*.java src/raytracer/shapes/*.java
	#$(RM) classes/raytracer/*.class classes/raytracer/pigments/*.class classes/raytracer/shapes/*.class
	#$(JAVAC) $(JFLAGS) src/worker/*.java
	#java -cp /home/ec2-user/BIT/samples ICount src/raytracer classes/raytracer

clean:
	$(RM) src/worker/*.class src/raytracer/*.class src/raytracer/pigments/*.class src/raytracer/shapes/*.class
