package com.example.android.inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final int PRODUCT_LOADER_ID = 1;
    private ProductCursorAdapter mProductAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        setAdapter();
        setToolbar();
        setFloatingActionButton();
        getLoaderManager().initLoader(PRODUCT_LOADER_ID, null, this);
    }

    /**
     * Create the toolbar menu.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    /**
     * Handles when a item in the ListView is clicked.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertProduct();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllProducts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the cursor loader initialization.
     * @param id
     * @param args
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE};

        // Perform a query on the provider using the ContentResolver.
        // Use the {@link PetEntry#CONTENT_URI} to access the pet data.
        return new CursorLoader(
                this,
                ProductEntry.CONTENT_URI,   // The content URI of the words table
                projection,             // The columns to return for each row
                null,                   // Selection criteria
                null,                   // Selection criteria
                null);
    }

    /**
     * Handles when the loader has finished.
     * @param loader
     * @param data
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mProductAdapter.swapCursor(data);
    }

    /**
     * Handles the loader reset.
     * @param loader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mProductAdapter.swapCursor(null);
    }

    ///////////////////////
    /// Utility methods ///
    ///////////////////////

    /**
     * Helper method to insert hardcoded pet data into the database. For debugging purposes only.
     */
    private void insertProduct() {
        // Create a ContentValues object where column names are the keys, product attributes are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, "Toto");
        values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, "Terrier");
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, 1);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, 10.0);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME, "Supplier name");
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, "supplier@example.com");

        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
    }

    /**
     * Delete all products in the database.
     */
    private void deleteAllProducts() {
        int deletedProducts = getContentResolver().delete(ProductEntry.CONTENT_URI, null, null);
        if (deletedProducts > 0) {
            // The delete was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.action_delete_all_products_successful) + " (" + deletedProducts + ")", Toast.LENGTH_SHORT).show();
        } else {
            // There was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_delete_product_failed), Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * Sets the cursor adapter for the ListView that holds the product list.
     */
    private void setAdapter() {
        // get the pieces that compose the adapter
        mProductAdapter      = new ProductCursorAdapter(this, null);
        View emptyView       = findViewById(R.id.empty_view);
        ListView petListView = (ListView)findViewById(R.id.list_view_pet);

        // set the cursor adapter
        petListView.setAdapter(mProductAdapter);
        // set the empty view
        petListView.setEmptyView(emptyView);
        // add the click event handler for every item in the ListView
        petListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent editProductIntent = new Intent(InventoryActivity.this, EditorActivity.class);
                editProductIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                editProductIntent.setData(ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id));
                startActivity(editProductIntent);
            }
        });
    }

    /**
     * Set the FAB click event handler.
     */
    private void setFloatingActionButton() {
        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Set the toolbar.
     */
    private void setToolbar() {
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
    }
}
