# Booky McBookface

An extremely unfancy, slightly broken, bare-bones epub reader. Also reads txt and html ebooks. 

<a href="https://f-droid.org/packages/com.quaap.bookymcbookface" target="_blank">
<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://play.google.com/store/apps/details?id=com.quaap.bookymcbookface" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>


## Features:
* Completely Open Source: no ads, tracking, or malicious permissions.
* Loads epub, txt, and HTML books.
* Load all books in a directory.
* List of books.
* Sort book list by title, author, or date added.
* Remembers last position in book.
* Page brightness per book.
* Font size per book.


## Reading controls:
* Page forward by using the ">>" button, or by swiping up or to the left.
* Page back by using the "<<" button, or by swiping down or to the right.
* Sub-page scroll by dragging up or down.
* Long press on text to open standard android text selection controls.
* "Contents" button shows the Table Of Contents, if available.
* The magnifying glass icon opens the font-size menu.
* The lightbulb icon opens the page brightness menu.
* Use Android back button to return to the book list from reader.

## Notes:
I've tested with many epubs from [Project Gutenberg](http://www.gutenberg.org/). If you notice 
problems with books from any source, [file an issue](https://github.com/quaap/BookyMcBookface/issues). 

You can use [Calibre](https://calibre-ebook.com/) to convert other formats like mobi and PDF to epub.

I am not opposed to someone adding support for other formats like mobi, but I don't want to add
third-party jars etc, and everything needs to be GPLv3 compatible.


## Known issues:
* Lag in paging up and down.
* Not actually using "book pages" (just scrolls a big page up and down).
* No bookmarks.
* No "goto page" functionality.
* Changing the font can lose your spot.
* Paging backward sometimes causes a weird downward scrolling, which makes you think you've lost
your spot but you really haven't.

