package com.quaap.bookymcbookface.book;

import java.util.LinkedHashMap;
import java.util.Map;

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

public class BookMetadata {
    private String title;
    private String author;
    private String filename;

    private Map<String,String> alldata = new LinkedHashMap<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Map<String, String> getAlldata() {
        return alldata;
    }

    public void setAlldata(Map<String, String> alldata) {
        this.alldata = alldata;
    }
}
