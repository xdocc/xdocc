package net.xdocc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import junit.framework.Assert;
import net.xdocc.handlers.Handler;

import org.junit.Test;

public class TestCompile2 {
	/**
	 * Test the compiler with levels.
	 * 
	 * @throws IOException .
	 * @throws InterruptedException .
	 * 
	 */
	
	private static final String genString = "/tmp/gen";
	private static final String sourceString = "/example|si=50x50|sn=500x500|all";

	private static Site site;
	private static File mapCache;
	private static Service service;

	
	@Test
	public void testCompile() throws IOException, InterruptedException {
		Path source = Paths.get("/tmp/src.xdocc");
		Utils.deleteDirectory(source);
		Path generated = Paths.get("/tmp/gen.xdocc");
		Utils.deleteDirectory(generated);
		// src and dst directories set
		Utils.createFile(source, ".templates/page.ftl", "!header!<br>[${document.generate}/${document.debug}]<br>!footer!");
		Utils.createFile(source, ".templates/collection.ftl", "!col-beg!<br><#list documents as document>${document.generate}/${document.debug}</#list><br>!col-end!");
		
		Utils.createFile(source, ".templates/text.ftl", "text:[${content}]");
		
		Service service = new Service();
		List<Handler> handlers = service.findHandlers();
		Site site = new Site(service, source, generated, handlers, null);
		// create content
		Utils.createFile(site.getSource(), "1-index|.txt", "AAhelloAA");
		Utils.createFile(site.getSource(), "1-more/2-more|.txt", "AAmoreAA");
		Utils.createFile(site.getSource(), "2-evenmore/3-next|.pre/4-even1|.txt", "AAevenAA");
		
		
		service.compile(site);
		service.waitFor(site.getSource());
		
		CompileResult cr = service.getCompileResult(site.getSource());
		List<Document> docs = cr.getDocument().getDocuments();
		Assert.assertEquals(3, docs.size());
		for(Document doc:docs) {
			Assert.assertEquals(1, doc.getLevel());
			if(doc.getUrl().equals("index.html")) {
				Assert.assertEquals("file", doc.getType());			
			} else if (doc.getUrl().equals("more")) {
				List<Document> docs2 = doc.getDocuments();
				Assert.assertEquals(1, docs2.size());
				Assert.assertEquals("more/more.html", docs2.get(0).getUrl());
				Assert.assertEquals(2, docs2.get(0).getLevel());
			} else if (doc.getUrl().equals("evenmore")) {
				List<Document> docs2 = doc.getDocuments();
				Assert.assertEquals(1, docs2.size());
				Assert.assertEquals("evenmore/next/even1.html", docs2.get(0).getUrl());
				Assert.assertEquals(3, docs2.get(0).getLevel());
			} else {
				Assert.fail("unknown name " + doc.getUrl());
			}
		}
	}
	
	@Test
	public void testCompileGallery() throws IOException, InterruptedException {
		
	}
}
