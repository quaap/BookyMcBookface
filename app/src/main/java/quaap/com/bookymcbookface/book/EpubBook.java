package quaap.com.bookymcbookface.book;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import quaap.com.bookymcbookface.FsTools;
import quaap.com.bookymcbookface.Zip;

/**
 * Created by tom on 9/12/17.
 */

public class EpubBook extends Book {

    public static final String META_PREFIX = "meta.";

    private File bookContentDir;

    public EpubBook(Context context) {
        super(context);
    }

    @Override
    protected void load() throws IOException {

        if (!getSharedPreferences().contains("ordercount")) {
            for (File file: Zip.unzip(getFile(), getThisBookDir())) {
                Log.d("EPUB", "unzipped + " + file);
            }
            loadEpub();
        }

        SharedPreferences bookdat = getSharedPreferences();

        //Set<String> keys = bookdat.getAll().keySet();

        bookContentDir = new File(bookdat.getString("bookContentDir",""));
        int ocount = bookdat.getInt("ordercount",0);

        for (int i=0; i<ocount; i++) {
            String item = bookdat.getString("order." + i, "");
            String file = bookdat.getString("item." + item, "");
            docFileOrder.add(item);
            docFiles.put(item, file);

            Log.d("EPUB", "Item: " + item + ". File: " + file);
        }

        int toccount = bookdat.getInt("ordercount",0);

        for (int i=0; i<toccount; i++) {
            String label = bookdat.getString("toc.label." + i, "");
            String point = bookdat.getString("toc.content." + i, "");

            tocPoints.put(point,label);
            Log.d("EPUB", "TOC: " + label + ". File: " + point);

        }
    }

    @Override
    public Map<String,String> getToc() {
        return Collections.unmodifiableMap(tocPoints);
    }

    @Override
    protected List<String> getSectionIds() {
        return Collections.unmodifiableList(docFileOrder);
    }

    @Override
    protected Uri getUriForSectionID(String id) {

        return Uri.fromFile(new File(getFullBookContentDir(), docFiles.get(id)));
    }

    @Override
    protected Uri getUriForSection(String section) {
//        int pound = section.indexOf("#");
//        if (pound>-1) {
//            section = section.substring(0,pound);
//        }
        if (section.startsWith(getFullBookContentDir().getPath())) {
            return Uri.fromFile(new File(section));
        }

        return Uri.fromFile(new File(getFullBookContentDir(), section));
    }


    protected String getSectionIDForSection(String section) {
        Uri sectionUri = Uri.parse(section);
        Log.d("EPB", "Section file: " + section);
//        int pound = section.indexOf("#");
//        if (pound>-1) {
//            section = section.substring(0,pound);
//        }
//
//        Log.d("EPB", "Section file: " + section);
//        Log.d("EPB", "Remove: " + getFullBookContentDir().toURI().toString());
//        section = section.replaceFirst(getFullBookContentDir().toURI().toString(), "");

        String sectionPath = sectionUri.getPath();

        Log.d("EPB", "Section path: " + sectionPath);

        section = sectionPath.replaceFirst(getFullBookContentDir().toString(), "");
        section = section.replaceFirst("/", "");

        Log.d("EPB", "Section file: " + section);
        for (Map.Entry<String,String> entry: docFiles.entrySet()) {
            if (section.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    private File getFullBookContentDir() {
        return new File(getThisBookDir(), bookContentDir.getPath());
    }


    private Map<String,String> metadata = new HashMap<>();
    private Map<String,String> docFiles = new LinkedHashMap<>();
    private List<String> docFileOrder = new ArrayList<>();

    private Map<String,String> tocPoints = new LinkedHashMap<>();


    private void loadEpub() throws FileNotFoundException {

        List<String> rootFiles = getRootFilesFromContainer(new FileReader(new File(getThisBookDir(), "META-INF/container.xml")));

        SharedPreferences.Editor bookdat = getSharedPreferences().edit();

        String bookContentDir = new File(rootFiles.get(0)).getParent();
        Map<String,?> dat = processBookDataFromRootFile(new FileReader(new File(getThisBookDir(),rootFiles.get(0))));

        bookdat.putString("bookContentDir", bookContentDir);

        for (Map.Entry<String,?> entry: dat.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                bookdat.putString(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                bookdat.putInt(entry.getKey(), (Integer) value);
            }
        }

        if (dat.get("toc")!=null) {
            File tocfile = new File(new File(getThisBookDir(), bookContentDir), (String)dat.get("item." + dat.get("toc")));
            Map<String, ?> tocDat = processToc(new FileReader(tocfile));

            for (Map.Entry<String,?> entry: tocDat.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    bookdat.putString(entry.getKey(), (String) value);
                } else if (value instanceof Integer) {
                    bookdat.putInt(entry.getKey(), (Integer) value);
                }
            }
        }

        bookdat.apply();

    }


    public BookMetadata getMetaData() throws IOException {


        try (ZipFile zipReader = new ZipFile(getFile())) {

            ZipEntry container = zipReader.getEntry("META-INF/container.xml");
            if (container == null) return null;

            List<String> rootFiles = getRootFilesFromContainer(new InputStreamReader(zipReader.getInputStream(container)));
            if (rootFiles.size() == 0) return null;

            ZipEntry content = zipReader.getEntry(rootFiles.get(0));
            if (content == null) return null;

            Map<String, ?> data = processBookDataFromRootFile(new InputStreamReader(zipReader.getInputStream(content)));

            Map<String, String> metadata = new LinkedHashMap<>();

            for (String key : data.keySet()) {
                if (key.startsWith(META_PREFIX)) {
                    metadata.put(key.substring(META_PREFIX.length()), data.get(key).toString());
                    Log.d("META", key.substring(META_PREFIX.length()) + "=" + data.get(key).toString());
                }
            }

            BookMetadata mdata = new BookMetadata();
            mdata.setFilename(getFile().getPath());
            mdata.setTitle(metadata.get("dc:title"));
            mdata.setAuthor(metadata.get("dc:creator"));
            mdata.setAlldata(metadata);
            return mdata;

        }
    }


    private static List<String> getRootFilesFromContainer(Reader containerxml) {

        List<String> rootFiles = new ArrayList<>();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(containerxml);

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("rootfile")) {
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            if (xpp.getAttributeName(i).equals("full-path")) {
                                rootFiles.add(xpp.getAttributeValue(i));
                            }
                        }
                    }

                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e("BMBF", "Error parsing xml " + e, e);
        }

        return rootFiles;

    }


    private static Map<String,?> processBookDataFromRootFile(Reader rootReader) {

        //SharedPreferences.Editor bookdat = getSharedPreferences().edit();

        Map<String,Object> bookdat = new LinkedHashMap<>();

        XPathFactory factory = XPathFactory.newInstance();
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        String toc = null;
        //String bookContentDir = null;


        try {



            DocumentBuilder builder = dfactory.newDocumentBuilder();

            Document doc = builder.parse(new InputSource(rootReader));

            XPath xpath = factory.newXPath();


            xpath.setNamespaceContext(packnsc);

            Node root = (Node)xpath.evaluate("/package", doc.getDocumentElement(), XPathConstants.NODE);
            Log.d("EPUB", root.getNodeName());

            {
                XPath metaPaths = factory.newXPath();
                metaPaths.setNamespaceContext(packnsc);
                NodeList metas = (NodeList) metaPaths.evaluate("metadata/*", root, XPathConstants.NODESET);
                for (int i = 0; i < metas.getLength(); i++) {
                    Node node = metas.item(i);
                    String key;
                    String value;
                    if (node.getNodeName().equals("meta")) {
                        NamedNodeMap attrs = node.getAttributes();
                        key  = attrs.getNamedItem("name").getNodeValue();
                        value = attrs.getNamedItem("content").getNodeValue();
                    } else {
                        key = node.getNodeName();
                        value = node.getTextContent();
                    }
                    Log.d("EPB", "metadata: " + key+"="+value);
                    //metadata.put(key,value);
                    bookdat.put(META_PREFIX +key,value);
                }
            }

            {
                XPath manifestPath = factory.newXPath();
                manifestPath.setNamespaceContext(packnsc);

                NodeList mani = (NodeList) manifestPath.evaluate("manifest/item", root, XPathConstants.NODESET);
                for (int i = 0; i < mani.getLength(); i++) {
                    Node node = mani.item(i);
                    if (node.getNodeName().equals("item")) {
                        String key;
                        String value;
                        NamedNodeMap attrs = node.getAttributes();
                        key = attrs.getNamedItem("id").getNodeValue();
                        value = attrs.getNamedItem("href").getNodeValue();
                        //docFiles.put(key,value);
                        bookdat.put("item."+key,value);
                        Log.d("EPB", "manifest: " + key+"="+value);

                    }
                }
            }

            {
                XPath spinePath = factory.newXPath();
                spinePath.setNamespaceContext(packnsc);
                Node spine = (Node) spinePath.evaluate("spine", root, XPathConstants.NODE);
                NamedNodeMap sattrs = spine.getAttributes();
                toc = sattrs.getNamedItem("toc").getNodeValue();

                bookdat.put("toc", toc);
                Log.d("EPB", "spine: toc=" + toc);

                NodeList spineitems = (NodeList) spinePath.evaluate("itemref", spine, XPathConstants.NODESET);
                for (int i = 0; i < spineitems.getLength(); i++) {
                    Node node = spineitems.item(i);
                    if (node.getNodeName().equals("itemref")) {
                        NamedNodeMap attrs = node.getAttributes();

                        String item = attrs.getNamedItem("idref").getNodeValue();

                        bookdat.put("order."+i, item);
                        Log.d("EPB", "spine: " + item);

                        //docFileOrder.add(item);
                    }

                }
                bookdat.put("ordercount", spineitems.getLength());
            }


        } catch (Exception e) {
            Log.e("BMBF", "Error parsing xml " + e.getMessage(), e);
        }

        return bookdat;
    }


    private static Map<String,?> processToc(Reader tocReader) {
        Map<String,Object> bookdat = new LinkedHashMap<>();

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        XPathFactory factory = XPathFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dfactory.newDocumentBuilder();

            Document doc = builder.parse(new InputSource(tocReader));

            XPath tocPath = factory.newXPath();
            tocPath.setNamespaceContext(tocnsc);

            Node nav = (Node)tocPath.evaluate("/ncx/navMap", doc, XPathConstants.NODE);

            int total = readNavPoint(nav, tocPath, bookdat, 0);
            bookdat.put("toccount", total);

        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            Log.e("BMBF", "Error parsing xml " + e.getMessage(), e);
        }
        return bookdat;
    }


    private static  int readNavPoint(Node nav, XPath tocPath, Map<String,Object> bookdat, int total) throws XPathExpressionException {

        NodeList list = (NodeList)tocPath.evaluate("navPoint", nav, XPathConstants.NODESET);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            String label = tocPath.evaluate("navLabel/text/text()", node);
            String content = tocPath.evaluate("content/@src", node);
            bookdat.put("toc.label."+total, label);
            bookdat.put("toc.content."+total, content);
            //Log.d("EPB", "toc: " + label + " " + content);
            total++;
            total = readNavPoint(node, tocPath, bookdat, total);
        }
        return total;
    }


    private static NamespaceContext tocnsc = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String s) {
            return "http://www.daisy.org/z3986/2005/ncx/";
        }

        @Override
        public String getPrefix(String s) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String s) {
            return null;
        }
    };

    private static NamespaceContext packnsc = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String s) {
            if (s!=null && s.equals("dc")) {
                return "http://purl.org/dc/elements/1.1/";
            }
            return "http://www.idpf.org/2007/opf";

        }

        @Override
        public String getPrefix(String s) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String s) {
            return null;
        }
    };


}


