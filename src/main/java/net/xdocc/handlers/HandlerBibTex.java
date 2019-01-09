package net.xdocc.handlers;

import net.xdocc.*;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.Value;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlerBibTex  implements Handler {
    public static final Map<String, String> MAP = new HashMap<String, String>();
    static{
        MAP.put("bibtex.ftl", "${content}");
    }

    @Override
    public boolean canHandle(Site site, XPath xPath) {
        return xPath.isCompile() && !xPath.isDirectory() && HandlerUtils.knowsExtension(knownExtensions(), xPath);
    }

    @Override
    public List<String> knownExtensions() {
        return Arrays.asList(new String[]{"bib", "BIB", "bibtex", "Bibtex", "BIBTEX"});
    }

    @Override
    public XItem compile(Site site, XPath xPath, Map<String, Integer> filesCounter, Cache cache) throws Exception {
        final XItem doc;
        final Path generatedFile = xPath.resolveTargetFromBasePath(xPath.getTargetURL() + ".html");
        Cache.CacheEntry cached = cache.getCached(site, xPath);
        if (cached != null) {
            doc = cached.xItem();
            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
        } else {

            Charset charset = HandlerUtils.detectCharset(Paths.get(xPath.path()));
            String content = HandlerUtils.readFile(Paths.get(xPath.path()), charset);
            Reader reader = new StringReader(content);
            org.jbibtex.BibTeXParser bibtexParser = new org.jbibtex.BibTeXParser();
            org.jbibtex.BibTeXDatabase database = bibtexParser.parseFully(reader);

            String htmlContent = convertHTML(database.getEntries());
            doc = Utils.createDocument(site, xPath, htmlContent, "text");
            if (xPath.getParent().isItemWritten() && xPath.isItemWritten()) {
                Utils.writeHTML(xPath, doc, generatedFile);
                Utils.increase(filesCounter, Utils.listPathsGen(site, generatedFile));
            }
            cache.setCached(site, xPath, (Path)null, doc, generatedFile);
        }
        return doc;
    }

    private String convertHTML(final Map<Key, BibTeXEntry> map) throws IOException {

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<Key,BibTeXEntry> entry:map.entrySet()) {
            String authors = convertAuthor(entry.getValue().getField(BibTeXEntry.KEY_AUTHOR));
            String title = convertTitle(entry.getValue().getField(BibTeXEntry.KEY_TITLE));
            String journal = convertJournal(entry.getValue().getField(BibTeXEntry.KEY_JOURNAL));
            String date = convertDate(entry.getValue().getField(BibTeXEntry.KEY_YEAR), entry.getValue().getField(BibTeXEntry.KEY_MONTH));
            String how = convertHow(entry.getValue().getField(BibTeXEntry.KEY_HOWPUBLISHED));
            String booktitle = convertHow(entry.getValue().getField(BibTeXEntry.KEY_BOOKTITLE));
            String url = convertUrl(entry.getValue().getField(BibTeXEntry.KEY_URL));

            sb.append("<div class=\"citation\">");
            sb.append(convertAll(entry.getValue().getType(), authors, title, journal, date, how, booktitle, url));
            sb.append("</div>");
        }

        return sb.toString();
    }

    private String convertAll(Key type, String authors, String title, String journal, String date, String how, String booktitle, String url) {
        StringBuilder sb = new StringBuilder();
        sb.append(authors);
        sb.append(", ");
        sb.append("\"");
        sb.append(title);
        sb.append("\", ");

        if(journal != null) {
            sb.append(journal);
            sb.append(";");
        } else if (how !=null) {
            sb.append(how);
            sb.append(";");
        }  else if (booktitle !=null) {
            sb.append(booktitle);
            sb.append("; ");
        }

        if(date != null) {
            sb.append(date);
            sb.append(". ");
        }
        if(url != null) {
            sb.append(url);
        }

        return sb.toString();
    }

    private String convertHow(Value how) {
        if(how == null) {
            return null;
        }
        return "<b>" + how.toUserString() +"</b>";
    }

    private String convertUrl(Value url) {
        if(url == null) {
            return null;
        }
        return "<a href=\""+url.toUserString()+"\">"+url.toUserString()+"</a>";
    }

    private String convertDate(Value year, Value month) {
        if(month != null && year != null) {
            String rawMonth = month.toUserString();
            String monthString;
            if(rawMonth.equalsIgnoreCase("jan")) {
                monthString = "January";
            } else if(rawMonth.equalsIgnoreCase("feb")) {
                monthString = "February";
            } else if(rawMonth.equalsIgnoreCase("mar")) {
                monthString = "March";
            } else if(rawMonth.equalsIgnoreCase("apr")) {
                monthString = "April";
            } else if(rawMonth.equalsIgnoreCase("may")) {
                monthString = "May";
            } else if(rawMonth.equalsIgnoreCase("jun")) {
                monthString = "June";
            } else if(rawMonth.equalsIgnoreCase("jul")) {
                monthString = "July";
            } else if(rawMonth.equalsIgnoreCase("aug")) {
                monthString = "August";
            } else if(rawMonth.equalsIgnoreCase("sep")) {
                monthString = "September";
            } else if(rawMonth.equalsIgnoreCase("oct")) {
                monthString = "October";
            } else if(rawMonth.equalsIgnoreCase("nov")) {
                monthString = "November";
            } else {
                monthString = "December";
            }

            return monthString + ", " + year.toUserString();
        } else if(year != null) {
            return year.toUserString();
        } else return null;
    }

    private String convertJournal(Value journal) {
        if(journal == null) {
            return null;
        }
        return "<b>" + journal.toUserString() + "</b>";
    }

    private String convertTitle(Value title) {
        return title.toUserString();
    }

    private String convertAuthor(Value author) {
        if(author == null) {
            return null;
        }
        String raw = author.toUserString();

        StringBuilder sb = new StringBuilder();
        //StringTokenizer st = new StringTokenizer(raw, "and");
        String[] tokens = raw.split("and");
        for(int i=0;i<tokens.length;i++) {
            String oneAuthor = tokens[i];
            StringBuilder sb2 = new StringBuilder();
            String[] tokens2 = oneAuthor.split(",");
            for(int j=0;j<tokens2.length;j++) {
                String part = tokens2[j].trim();
                if(sb2.length() == 0) {
                    sb2.insert(0, part);
                } else {
                    sb2.insert(0, part + " ");
                }
            }
            sb.append(sb2);
            if(i + 1 < tokens.length) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
