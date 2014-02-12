package net.xdocc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestBrowse.class, TestCache.class, TestCompiler.class,
		TestParser.class, TestTags.class, TestXPath.class })
public class AllTests {

}
