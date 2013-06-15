package net.xdocc;

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
	 */
	@Test
	public void testCompile() throws IOException, InterruptedException {
		Path source = Paths.get("/tmp/src.xdocc");
		Utils.deleteDirectory(source);
		Path generated = Paths.get("/tmp/gen.xdocc");
		Utils.deleteDirectory(generated);
		// src and dst directories set
		Utils.createFile(source, ".templates/page.ftl", "!header!<br>[${document.generate}/${document.debug}]<br>!footer!");
		Utils.createFile(source, ".templates/collection.ftl", "!col-beg!<br><#list documents as document>${document.generate}[${document.name}]/${document.debug}</#list><br>!col-end!");
		
		Utils.createFile(source, ".templates/text.ftl", "text:[${content}]");
		List<Handler> handlers = Service.findHandlers();
		Site site = new Site(source, generated, handlers, null);
		// create content
		Utils.createFile(site.getSource(), "1|index|.txt", "AAhelloAA");
		Utils.createFile(site.getSource(), "1|more|more/2|more|.txt", "AAmoreAA");
		Utils.createFile(site.getSource(), "2|evenmore|evenmore/3|next|next.pre/4|even1|.txt", "AAevenAA");
		
		
		Service.compile(site);
		Service.waitFor(site.getSource());
		
		CompileResult cr = Service.getCompileResult(site.getSource());
		List<Document> docs = cr.getDocument().getDocuments();
		Assert.assertEquals(3, docs.size());
		for(Document doc:docs) {
			Assert.assertEquals(1, doc.getLevel());
			if(doc.getName().equals("index")) {
				Assert.assertEquals("file", doc.getType());			
			} else if (doc.getName().equals("more")) {
				List<Document> docs2 = doc.getDocuments();
				Assert.assertEquals(1, docs2.size());
				Assert.assertEquals("more", docs2.get(0).getName());
				Assert.assertEquals(2, docs2.get(0).getLevel());
			} else if (doc.getName().equals("evenmore")) {
				List<Document> docs2 = doc.getDocuments();
				Assert.assertEquals(1, docs2.size());
				Assert.assertEquals("even1", docs2.get(0).getName());
				Assert.assertEquals(3, docs2.get(0).getLevel());
			} else {
				Assert.fail("unknown name " + doc.getName());
			}
		}
	}
}
