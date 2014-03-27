package net.xdocc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestBrowse.class, TestCache.class, TestCompiler.class,
TestParser.class, TestTags.class, TestXPath.class, TestLink.class, TestLinkUrl.class, TestLinkUrl2.class, TestService.class })
public class AllTests {
	

}
