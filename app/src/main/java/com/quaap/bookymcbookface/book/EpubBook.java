package com.quaap.bookymcbookface.book;


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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import com.quaap.bookymcbookface.Zip;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of BookyMcBookface and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

public class EpubBook extends Book {

    private static final String META_PREFIX = "meta.";
    private static final String ORDERCOUNT = "ordercount";
    private static final String BOOK_CONTENT_DIR = "bookContentDir";
    private static final String ORDER = "order.";
    private static final String ITEM = "item.";
    private static final String TOCCOUNT = "toccount";
    private static final String TOC_LABEL = "toc.label.";
    private static final String TOC_CONTENT = "toc.content.";
    private static final String TOC = "toc";

    private File bookContentDir;

    public EpubBook(Context context) {
        super(context);
    }

    @Override
    protected void load() throws IOException {

        if (!getSharedPreferences().contains(ORDERCOUNT)) {
            for (File file: Zip.unzip(getFile(), getThisBookDir())) {
                Log.d("EPUB", "unzipped + " + file);
            }
            loadEpub();
        }

        SharedPreferences bookdat = getSharedPreferences();

        //Set<String> keys = bookdat.getAll().keySet();

        bookContentDir = new File(bookdat.getString(BOOK_CONTENT_DIR,""));
        int ocount = bookdat.getInt(ORDERCOUNT,0);

        for (int i=0; i<ocount; i++) {
            String item = bookdat.getString(ORDER + i, "");
            String file = bookdat.getString(ITEM + item, "");
            docFileOrder.add(item);
            docFiles.put(item, file);

            //Log.d("EPUB", "Item: " + item + ". File: " + file);
        }

        int toccount = bookdat.getInt(TOCCOUNT,0);

        for (int i=0; i<toccount; i++) {
            String label = bookdat.getString(TOC_LABEL + i, "");
            String point = bookdat.getString(TOC_CONTENT + i, "");

            tocPoints.put(point,label);
           // Log.d("EPUB", "TOC: " + label + ". File: " + point);

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
    protected ReadPoint locateReadPoint(String section) {
        ReadPoint point = null;

        if (section==null) return null;

        Uri suri = null;

        try {
            suri = Uri.parse(Uri.decode(section));
        } catch (Exception e) {
            Log.e("Epub", e.getMessage(), e);
        }

        if (suri==null) return null;

        if (suri.isRelative()) {
            suri = new Uri.Builder().scheme("file").path(getFullBookContentDir().getPath()).appendPath(suri.getPath()).fragment(suri.getFragment()).build();
        }

        String file = suri.getLastPathSegment();

        if (file==null) return null;

        String sectionID = null;

        for (Map.Entry<String,String> entry: docFiles.entrySet()) {
            if (file.equals(entry.getValue())) {
                sectionID = entry.getKey();
            }
        }

        if (sectionID!=null) {
            point = new ReadPoint();
            point.setId(sectionID);
            point.setPoint(suri);
        }

        return point;
    }



    private File getFullBookContentDir() {
        return new File(getThisBookDir(), bookContentDir.getPath());
    }


    private Map<String,String> metadata = new HashMap<>();
    private final Map<String,String> docFiles = new LinkedHashMap<>();
    private final List<String> docFileOrder = new ArrayList<>();

    private final Map<String,String> tocPoints = new LinkedHashMap<>();


    private void loadEpub() throws FileNotFoundException {

        List<String> rootFiles = getRootFilesFromContainer(new BufferedReader(new FileReader(new File(getThisBookDir(), "META-INF/container.xml"))));

        SharedPreferences.Editor bookdat = getSharedPreferences().edit();

        String bookContentDir = new File(rootFiles.get(0)).getParent();
        if (bookContentDir==null) bookContentDir = "";

        Map<String,?> dat = processBookDataFromRootFile(new BufferedReader(new FileReader(new File(getThisBookDir(),rootFiles.get(0)))));

        bookdat.putString(BOOK_CONTENT_DIR, bookContentDir);

        for (Map.Entry<String,?> entry: dat.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                bookdat.putString(entry.getKey(), (String) value);
            } else if (value instanceof Integer) {
                bookdat.putInt(entry.getKey(), (Integer) value);
            }
        }

        if (dat.get(TOC)!=null) {
            String fname = (String)dat.get(ITEM + dat.get(TOC));
            Log.d("EPUB", "tocfname = " + fname + " bookContentDir =" + bookContentDir);
            File tocfile = new File(new File(getThisBookDir(), bookContentDir), fname);
            Map<String, ?> tocDat = processToc(new BufferedReader(new FileReader(tocfile)));

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

    @Override
    public String getTitle() {
        return getSharedPreferences().getString(META_PREFIX + "dc:title", "No title");
    }


    public BookMetadata getMetaData() throws IOException {


        try (ZipFile zipReader = new ZipFile(getFile())) {

            ZipEntry container = zipReader.getEntry("META-INF/container.xml");
            if (container == null) return null;

            List<String> rootFiles = getRootFilesFromContainer(new BufferedReader(new InputStreamReader(zipReader.getInputStream(container))));
            if (rootFiles.size() == 0) return null;

            ZipEntry content = zipReader.getEntry(rootFiles.get(0));
            if (content == null) return null;

            Map<String, ?> data = processBookDataFromRootFile(new BufferedReader(new InputStreamReader(zipReader.getInputStream(content))));
            if (data.size()==0) {
                Log.d("Epub", "No data for " + getFile());
            }

            Map<String, String> metadata = new LinkedHashMap<>();

            for (String key : data.keySet()) {
                if (key.startsWith(META_PREFIX)) {
                    metadata.put(key.substring(META_PREFIX.length()), data.get(key).toString());
                   // Log.d("META", key.substring(META_PREFIX.length()) + "=" + data.get(key).toString());
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


    private static List<String> getRootFilesFromContainer(BufferedReader containerxml) {

        List<String> rootFiles = new ArrayList<>();

        try {

            containerxml.mark(4);
            if ('\ufeff' != containerxml.read()) containerxml.reset(); // not the BOM marker

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


    private static Map<String,?> processBookDataFromRootFile(BufferedReader rootReader) {

        //SharedPreferences.Editor bookdat = getSharedPreferences().edit();

        Map<String,Object> bookdat = new LinkedHashMap<>();

        XPathFactory factory = XPathFactory.newInstance();
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

        String toc = null;
        //String bookContentDir = null;


        try {

            rootReader.mark(4);
            if ('\ufeff' != rootReader.read()) rootReader.reset(); // not the BOM marker

            DocumentBuilder builder = dfactory.newDocumentBuilder();

            Document doc = builder.parse(new InputSource(rootReader));

            XPath xpath = factory.newXPath();


            xpath.setNamespaceContext(packnsc);

            Node root = (Node)xpath.evaluate("/package", doc.getDocumentElement(), XPathConstants.NODE);
            //Log.d("EPUB", root.getNodeName());

            {
                XPath metaPaths = factory.newXPath();
                metaPaths.setNamespaceContext(packnsc);
                NodeList metas = (NodeList) metaPaths.evaluate("metadata/*", root, XPathConstants.NODESET);
                for (int i = 0; i < metas.getLength(); i++) {
                    Node node = metas.item(i);
                    if (node == null) continue;
                    //Log.d("FFFF", node.getNodeName() + " " + node.getNodeValue());
                    String key=null;
                    String value = null;
                    NamedNodeMap attrs = node.getAttributes();
                    if (node.getNodeName().equals("meta") && attrs!=null) {
                        Node kn = attrs.getNamedItem("name");
                        if (kn!=null) key  = kn.getNodeValue();
                        Node kc = attrs.getNamedItem("content");
                        if (kc!=null) value = kc.getNodeValue();
                    } else {
                        key = node.getNodeName();
                        value = node.getTextContent();
                    }
                    //Log.d("EPB", "metadata: " + key+"="+value);
                    //metadata.put(key,value);
                    if (key!=null && value!=null) {
                        bookdat.put(META_PREFIX + key, value);
                    }
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
                        bookdat.put(ITEM +key,value);
                       // Log.d("EPB", "manifest: " + key+"="+value);

                    }
                }
            }

            {
                XPath spinePath = factory.newXPath();
                spinePath.setNamespaceContext(packnsc);
                Node spine = (Node) spinePath.evaluate("spine", root, XPathConstants.NODE);
                NamedNodeMap sattrs = spine.getAttributes();
                toc = sattrs.getNamedItem(TOC).getNodeValue();

                bookdat.put(TOC, toc);
                //Log.d("EPB", "spine: toc=" + toc);

                NodeList spineitems = (NodeList) spinePath.evaluate("itemref", spine, XPathConstants.NODESET);
                for (int i = 0; i < spineitems.getLength(); i++) {
                    Node node = spineitems.item(i);
                    if (node.getNodeName().equals("itemref")) {
                        NamedNodeMap attrs = node.getAttributes();

                        String item = attrs.getNamedItem("idref").getNodeValue();

                        bookdat.put(ORDER +i, item);
                        //Log.d("EPB", "spine: " + item);

                        //docFileOrder.add(item);
                    }

                }
                bookdat.put(ORDERCOUNT, spineitems.getLength());
            }


        } catch (Exception e) {
            Log.e("BMBF", "Error parsing xml " + e.getMessage(), e);
        }

        return bookdat;
    }


    private static Map<String,?> processToc(BufferedReader tocReader) {
        Map<String,Object> bookdat = new LinkedHashMap<>();

        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        XPathFactory factory = XPathFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = dfactory.newDocumentBuilder();

            tocReader.mark(4);
            if ('\ufeff' != tocReader.read()) tocReader.reset(); // not the BOM marker

            Document doc = builder.parse(new InputSource(tocReader));

            XPath tocPath = factory.newXPath();
            tocPath.setNamespaceContext(tocnsc);

            Node nav = (Node)tocPath.evaluate("/ncx/navMap", doc, XPathConstants.NODE);

            int total = readNavPoint(nav, tocPath, bookdat, 0);
            bookdat.put(TOCCOUNT, total);

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
            bookdat.put(TOC_LABEL +total, label);
            bookdat.put(TOC_CONTENT +total, content);
            //Log.d("EPB", "toc: " + label + " " + content + " " + total);
            total++;
            total = readNavPoint(node, tocPath, bookdat, total);
        }
        return total;
    }


    private static final NamespaceContext tocnsc = new NamespaceContext() {
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

    private static final NamespaceContext packnsc = new NamespaceContext() {
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


