package com.quaap.bookymcbookface;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

public class BookAdapter extends  RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private BookDb mDb;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class BookViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTitleView;
        public TextView mAuthorView;
        public BookViewHolder(ViewGroup listEntry, TextView titleView, TextView authorView) {
            super(listEntry);

            mTitleView = titleView;
            mAuthorView = authorView;

        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public BookAdapter(BookDb db) {
        mDb = db;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BookAdapter.BookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view

        ViewGroup listEntry = (ViewGroup)LayoutInflater.from(parent.getContext()).inflate(R.layout.book_list_item, parent, false);
        TextView titleView = listEntry.findViewById(R.id.book_title);
        TextView authorView = listEntry.findViewById(R.id.book_author);

        BookViewHolder vh = new BookViewHolder(listEntry, titleView, authorView);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(BookViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        //holder.mTitleView


       // holder.mTextView.setText(mDataset[position]);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return 12; //mDataset.length;
    }
}

