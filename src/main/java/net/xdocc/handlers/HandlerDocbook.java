package net.xdocc.handlers;

import freemarker.template.TemplateException;
import net.xdocc.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class HandlerDocbook implements Handler {

    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("docbook.ftl", "${content}");
    }


    @Override
    public boolean canHandle(Site site, XPath xPath) {
        System.err.println("testing: "+xPath);
        return xPath.isDirectory()
                && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception {

        final XItem doc;

        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten()) {
                for(String generatedFile:cached.generatedFiles()) {
                    Utils.increase(filesCounter, Utils.listPathsGen(site, Paths.get(generatedFile)));
                }
            }
        } else {

            //find first xml
            final File sourceXML = firstXMLFile(xPath);
            File tmpDir = Files.createTempDirectory("docbook").toFile();
            //create docbook in tmp output
            String output = Utils.executeAndOutput(new ProcessBuilder(
                    "/usr/bin/docbook2html",
                    "-o", tmpDir.toString(),
                    sourceXML.toString()));

            Path sourceFile = tmpDir.toPath().resolve("index.html");
            String htmlContent = HandlerHTML.htmlContent(sourceFile.toString());
            doc = Utils.createDocument(site, xPath, htmlContent, "docbook");
            Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL()+"/"+sourceFile.getFileName());


            Utils.writeHTML(xPath, doc, generatedFile);
            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));


            //copy HTML files, but modify header

            try (Stream<Path> paths = Files.walk(tmpDir.toPath())) {
                paths.forEach((Path filePath) -> {
                    try {
                        if(filePath.toString().indexOf("index.html") < 0) {
                            String htmlContent2 = HandlerHTML.htmlContent(filePath.toString());
                            XItem doc2 = Utils.createDocument(site, xPath, htmlContent2, "docbook");
                            Path generatedFile2 = xPath.resolveTargetFromBasePath(xPath.getTargetURL()+"/"+filePath.getFileName());
                            Utils.writeHTML(xPath, doc2, generatedFile2);
                            Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile2));
                        }
                    } catch (IOException | TemplateException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            final Path basePath = Paths.get(xPath.path());
            //copy files such as images, but not docgen* folders
            try (Stream<Path> paths = Files.walk(basePath)) {
                paths.forEach((Path filePath) -> {
                    try {
                        Path rel = basePath.relativize(filePath);
                        Path gen = xPath.resolveTargetFromBasePath(xPath.getTargetURL()+"/"+rel.toString());
                        if(!rel.toString().contains("docgen") && !filePath.toFile().equals(sourceXML)) {
                            if (Files.isDirectory(filePath)) {
                                Files.createDirectories(gen);
                            } else {
                                Files.copy(filePath, gen);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            doc.setConsumesDirectory(true);
            cache.setCached(site, xPath, (Path)null, doc, generatedFile);
        }
        //XItem ret = Utils.createDocument(site, xPath, null, "docbook");
        //ret.setConsumesDirectory(true);
        return doc;
    }

    private static File firstXMLFile(XPath xPath) {
        File dir = new File(xPath.path());
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".xml"));
        if(files.length > 0) {
            return files[0];
        }
        return null;
    }

    private String convertHTML(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        int len = lines.size();
        for (int i = 0; i < len; i++) {
            sb.append(lines.get(i));
            if (i < (len - 1)) {
                sb.append("<br/>");
            }
        }
        return sb.toString();
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"docbook", "Docbook", "DOCBOOK"});
    }
}