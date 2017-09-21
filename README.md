# Booky McBookface

An extremely unfancy, slightly broken, bare-bones epub reader. Also reads txt and html ebooks. 

Features:
* Completely opensource: no ads, tracking, or malicious permissions.
* Loads epub, txt, and html books.
* Load all books in a directory.
* List of books.
* Sort book list by title, author, or date added.
* Remembers last position in book.
* Page forward by using the button, or by swiping up or to the left.
* Page back by using the button, or by swiping down or to the right.
* "Contents" button shows the Table Of Contents, if available.
* Use Android back button to return to the book list.

I've tested with many epubs from [Project Gutenberg](http://www.gutenberg.org/). If you notice 
problems with books from any source, [file an issue](https://github.com/quaap/BookyMcBookface/issues). 

You can use [Calibre](https://calibre-ebook.com/) to convert other formats like mobi and PDF to epub.

I am not opposed to someone adding support for other formats like mobi, but I don't want to add
third-party jars etc, and everything needs to be GPLv3 compatible.


Known issues:
* Lag in paging up and down.
* Not actually using "book pages" (just scrolls a big page up and down).
* No bookmarks.
* No "goto page" functionality.
* Changing the font can lose your spot.
* Paging backward sometimes causes a weird downward scrolling, which makes you think you've lost
your spot but you really haven't.

