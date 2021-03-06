package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.R;
import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.util.Locale;

/**
 * {@link ProductCursorAdapter} is an adapter for a list view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class ProductCursorAdapter extends CursorAdapter {

    private final Context mContext;

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
        mContext = context;
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        final int id       = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));

        // product name.
        TextView tvName     = (TextView) view.findViewById(R.id.name);
        String name        = cursor.getString(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
        tvName.setText(name);

        // product quantity.
        TextView tvQuantity = (TextView) view.findViewById(R.id.quantity);
        final int quantity = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY));
        tvQuantity.setText(String.valueOf(quantity));
        setQuantityColor(quantity, tvQuantity);

        // product price
        TextView tvPrice    = (TextView) view.findViewById(R.id.price);
        float price        = cursor.getFloat(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE));
        String priceString = String.valueOf(price)+"€";
        tvPrice.setText( priceString);

        final Button bSale  = (Button) view.findViewById(R.id.button_sale);
        setButtonState(bSale, quantity);

        // Click listener for the SALE button.
        bSale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (quantity > 0) {
                    ContentValues values = new ContentValues();
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity - 1);

                    Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                    context.getContentResolver().update(uri, values, null, null);
                }
                setButtonState(bSale, quantity - 1);
            }
        });
    }

    /**
     * Set enabled/disabled state of the SALE button.
     * @param button
     * @param quantity
     */
    public void setButtonState(Button button, int quantity) {
        if (quantity == 0){
            button.setEnabled(false);
        } else {
            button.setEnabled(true);
        }
    }

    /**
     * Set the text color for the quantity TextView.
     * @param quantity
     * @param textView
     */
    public void setQuantityColor(int quantity, TextView textView) {
        if (quantity <= 5){
            textView.setTextColor(mContext.getResources().getColor(R.color.colorRed));
            return;
        }

        if (quantity <= 15){
            textView.setTextColor(mContext.getResources().getColor(R.color.colorOrange));
            return;
        }

        textView.setTextColor(mContext.getResources().getColor(R.color.colorGreen));
    }
}