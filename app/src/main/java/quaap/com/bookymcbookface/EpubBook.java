package quaap.com.bookymcbookface;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/**
 * Created by tom on 9/12/17.
 */

public class EpubBook extends Book {

    private String subbook;
    private File thisBookDir;
    private File bookContentDir;

    public EpubBook(Context context, File dataDir) {
        super(context, dataDir);
    }

    @Override
    protected void load() {
        subbook = "book" + getFile().getName();
        thisBookDir = new File(getDataDir(), subbook);
        if (!getSharedPreferences().contains(subbook)) {
            for (File file: Zip.unzip(getFile(), thisBookDir)) {
                Log.d("EPUB", "unzipped + " + file);
            }
            loadEpub();
        }

        SharedPreferences bookdat = getSharedPreferences();

        //Set<String> keys = bookdat.getAll().keySet();

        int count = bookdat.getInt("ordercount",0);

        for (int i=0; i<count; i++) {
            String item = bookdat.getString("order." + i, "");
            String file = bookdat.getString("item." + item, "");
            docFileOrder.add(item);
            docFiles.put(item, file);

            Log.d("EPUB", "Item: " + item + ". File: " + file);

        }


    }

    @Override
    public Page getPage(int page) {
        Page p = new Page();
        p.file = new File(bookContentDir, docFiles.get(docFileOrder.get(page)));
        return p;
    }

    @Override
    public Page getContents() {
        return null;
    }



    Map<String,String> metadata = new HashMap<>();
    Map<String,String> docFiles = new HashMap<>();
    List<String> docFileOrder = new ArrayList<>();


    private void loadEpub() {

        List<String> rootFiles = new ArrayList<>();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new FileReader(new File(thisBookDir, "META-INF/container.xml")));

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

        SharedPreferences.Editor bookdat = getSharedPreferences().edit();

        XPathFactory factory = XPathFactory.newInstance();
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        for (String rootFile: rootFiles) {
            try {
                Log.d("EPB", "Rootfile: " + rootFile);



                DocumentBuilder builder = dfactory.newDocumentBuilder();
                File container = new File(thisBookDir,rootFile);
                bookContentDir = container.getParentFile();

                Document doc = builder.parse(container);


//                InputSource docSource= new InputSource(new FileReader();

                XPath xpath = factory.newXPath();


                NamespaceContext nsc = new NamespaceContext() {
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

                xpath.setNamespaceContext(nsc);

                Node root = (Node)xpath.evaluate("/package", doc.getDocumentElement(), XPathConstants.NODE);
                Log.d("EPUB", root.getNodeName());

                {
                    XPath metaPaths = factory.newXPath();
                    metaPaths.setNamespaceContext(nsc);
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
                        bookdat.putString("meta."+key,value);
                    }
                }

                {
                    XPath manifestPath = factory.newXPath();
                    manifestPath.setNamespaceContext(nsc);

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
                            bookdat.putString("item."+key,value);
                            Log.d("EPB", "manifest: " + key+"="+value);

                        }
                    }
                }

                {
                    XPath spinePath = factory.newXPath();
                    spinePath.setNamespaceContext(nsc);
                    NodeList spineitems = (NodeList) spinePath.evaluate("spine/itemref", root, XPathConstants.NODESET);
                    for (int i = 0; i < spineitems.getLength(); i++) {
                        Node node = spineitems.item(i);
                        if (node.getNodeName().equals("itemref")) {
                            NamedNodeMap attrs = node.getAttributes();

                            String item = attrs.getNamedItem("idref").getNodeValue();

                            bookdat.putString("order."+i, item);
                            Log.d("EPB", "spine: " + item);

                            //docFileOrder.add(item);
                        }

                    }
                    bookdat.putInt("ordercount", spineitems.getLength());
                }


            } catch (Exception e) {
                Log.e("BMBF", "Error parsing xml " + e.getMessage(), e);
            }
        }
        bookdat.apply();


    }
}


