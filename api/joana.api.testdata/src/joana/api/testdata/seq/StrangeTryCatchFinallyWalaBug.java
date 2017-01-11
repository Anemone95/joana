/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

/**
 * TODO: @author Add your name here.
 */
public class StrangeTryCatchFinallyWalaBug {

	public static void main(String[] args) {
		StrangeTryCatchFinallyWalaBug s = new StrangeTryCatchFinallyWalaBug();
		s.runWorker();
		leak(toggle(SECRET));
	}
	
	Throwable rofl;
	
	void foo() {
	}
    final void runWorker() {
                    Throwable thrown = rofl;
                    try {
                        foo();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw x;
                    } finally {
                    	rofl = thrown;
                    }
    }
}

class MyError extends Error {};
class MyRuntimeException extends RuntimeException {};
