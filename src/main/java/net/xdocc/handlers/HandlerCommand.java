package net.xdocc.handlers;

import freemarker.template.TemplateException;
import net.xdocc.*;
import net.xdocc.Compiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class HandlerCommand implements Handler {

    private static final Logger LOG = LoggerFactory.getLogger(HandlerCommand.class);

    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("directory-command.ftl", "${content}");
        MAP.put("command.ftl", "${content}");
    }


    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return HandlerUtils.knowsExtension(knownExtensions(), xPath);
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

            if(xPath.isDirectory()) {
                String command = xPath.getDirectoryCommand();
                File tmpDir = Files.createTempDirectory("directory-command").toFile();
                command.replace("%TMP", tmpDir.toString());
                if (xPath.isDirectory()) {
                    command.replace("%DIR", xPath.toString());
                } else {
                    command.replace("%DIR", xPath.getParent().toString());
                    command.replace("%FILE", xPath.toString());
                }


                //create docbook in tmp output
                String output = Utils.executeAndOutput(new ProcessBuilder(command.split(" ")));

                Path sourceFile = tmpDir.toPath().resolve("index.html");
                String htmlContent = HandlerHTML.htmlContent(sourceFile.toString());
                doc = Utils.createDocument(site, xPath, htmlContent, "directory-command");
                Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + "/" + sourceFile.getFileName());


                Utils.writeHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));


                //copy HTML files, but modify header

                try (Stream<Path> paths = Files.walk(tmpDir.toPath())) {
                    paths.forEach((Path filePath) -> {
                        try {
                            if (filePath.toString().indexOf("index.html") < 0) {
                                String htmlContent2 = HandlerHTML.htmlContent(filePath.toString());
                                XItem doc2 = Utils.createDocument(site, xPath, htmlContent2, "directory-command");
                                Path generatedFile2 = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + "/" + filePath.getFileName());
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
                            Path gen = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + "/" + rel.toString());
                            if (!rel.toString().contains("docgen")) {
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
                cache.setCached(site, xPath, (Path) null, doc, generatedFile);
            } else {
                String command = null;
                if(xPath.extensionList().contains("rst")) {
                    command = xPath.getCommandRST();
                } else if(xPath.extensionList().contains("tex")) {
                    command = xPath.getCommandTEX();
                } else if(xPath.extensionList().contains("odt")) {
                    command = xPath.getCommandODT();
                } else if(xPath.extensionList().contains("docx")) {
                    command = xPath.getCommandDOCX();
                }
                if(command == null) {
                    command = "pandoc --mathjax -o %TMP %FILE";
                }

                File tmpDir = Files.createTempDirectory("command").toFile();

                command = command.replace("%TMPDIR", tmpDir.toString());
                String tmpFile = tmpDir.toString()+"/"+xPath.getTargetURLPath()+"/"+xPath.url()+".html";
                Path tmpP = Paths.get(tmpFile);
                Files.createDirectories(tmpP.getParent());

                command = command.replace("%TMP", tmpP.toString());
                command = command.replace("%OUTPUT", tmpDir.toString() + "/" + xPath.getTargetURLFilename());
                command = command.replace("%DIR", xPath.directory());
                command = command.replace("%FILE", xPath.path());


                String output = Utils.executeAndOutput(new ProcessBuilder(command.split(" ")));
                LOG.debug("exec output: {}", output);

                Path sourceFile = Paths.get(tmpFile);
                if(Files.exists(sourceFile)) {
                    String htmlContent = HandlerHTML.htmlContent(sourceFile.toString());
                    doc = Utils.createDocument(site, xPath, htmlContent, "command");
                    Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + "/" + sourceFile.getFileName());

                    if (xPath.getParent().isItemWritten()) {
                        Utils.writeHTML(xPath, doc, generatedFile);
                        Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
                    }
                    cache.setCached(site, xPath, (Path) null, doc, generatedFile);
                } else {
                    LOG.debug("file {} does not exist", sourceFile);
                    doc = null;
                }
            }
        }
        //XItem ret = Utils.createDocument(site, xPath, null, "docbook");
        //ret.setConsumesDirectory(true);
        return doc;
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
        return Arrays.asList(new String[]{"pandoc", "Pandoc", "PANDOC",
        "rst", "Rst", "RST",
        "tex", "Tex", "TEX",
        "odt", "Odt", "ODT",
        "docx", "Docx", "DOCX"});
    }
}