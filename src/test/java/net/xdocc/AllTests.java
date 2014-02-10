package net.xdocc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestCompile2.class, TestCompiler.class, TestParser.class,
		TestPath.class, TestTags.class })
public class AllTests {

}
