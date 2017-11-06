/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.android.inventoryapp.data.ProductContract.ProductEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{
    public static final String LOG_TAG = EditorActivity.class.getSimpleName();

    private static final int PRODUCT_LOADER_ID = 1;

    private static final int PICK_IMAGE = 100;

    /** maximum quantity allowed */
    private static final int MIN_QUANTITY = 0;

    /** minimum quantity allowed */
    private static final int MAX_QUANTITY = 100;

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the product's description */
    private EditText mDescriptionEditText;

    /** EditText field to enter the product's price */
    private EditText mPriceEditText;

    /** EditText field to enter the product's quantity */
    private EditText mQuantityEditText;

    /** EditText field to enter supplier name */
    private EditText mSupplierNameEditText;

    /** EditText field to enter supplier email */
    private EditText mSupplierEmailEditText;

    /** ImageView for the product's image */
    private ImageView mImageView;

    /** Fake button for selecting an image from the gallery */
    private RelativeLayout mBrowseGalleryButton;

    /** Fake button for placing an order (send asn email) */
    private RelativeLayout mOrderButton;

    /** EditText that hold the value to increment/decrement the quantity */
    private EditText mIncrementEditText;

    /** Button for incrementing th quantity */
    private Button mIncrementButton;

    /** Button for decrementing the quantity */
    private Button mDecrementButton;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductUri;

    /** Flag that indicates if the product has changed */
    private boolean mProductHasChanged = false;

    /** Product image URI */
    private Uri mImageUri = null;

    /** Click listener for the "pick image" fake button. */
    private View.OnClickListener mBrowseGalleryListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            openGallery();
        }
    };

    /** Sets an event listener to be attached to the EditText elements. */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    /** Listener for the decrement button */
    private View.OnClickListener mDecrementListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mIncrementButton.setEnabled(true);

            // parse increment
            int increment = getIncrement();
            mIncrementEditText.setText(String.valueOf(increment));

            // parse old quantity
            int quantity = getQuantity();

            // set new quantity
            int newQuantity = quantity - increment;
            if (newQuantity < (MIN_QUANTITY+1)) {
                newQuantity = MIN_QUANTITY;
                mDecrementButton.setEnabled(false);
                Toast.makeText(EditorActivity.this, "You have reached the minimum", Toast.LENGTH_SHORT).show();
            }
            mQuantityEditText.setText(String.valueOf(newQuantity));
        }
    };

    /** Listener for the increment button */
    private View.OnClickListener mIncrementListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mDecrementButton.setEnabled(true);

            // parse increment
            int increment = getIncrement();
            mIncrementEditText.setText(String.valueOf(increment));

            // parse old quantity
            int quantity = getQuantity();

            // set new quantity
            int newQuantity = quantity + increment;
            if (newQuantity > (MAX_QUANTITY-1) ) {
                newQuantity = MAX_QUANTITY;
                mIncrementButton.setEnabled(false);
                Toast.makeText(EditorActivity.this, "You have reached the maximum", Toast.LENGTH_SHORT).show();
            }
            mQuantityEditText.setText(String.valueOf(newQuantity));
        }
    };

    /** Listener that launches the email client for placing an order */
    private View.OnClickListener mOrderListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("mailto:" + mSupplierEmailEditText.getText().toString().trim()));
            if (intent.resolveActivity(getPackageManager()) !=null) {
                startActivity(intent);
            } else {
                Context context = getApplicationContext();
                Toast.makeText(context, "Unable to open.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /** Error message for the form validation */
    private String mErrorMessage = null;

    //////////////////////////
    /// Overridden methods ///
    //////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        setActivityTitle();
        setEventListeners();
        initLoader();
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     * @param  menu    The options menu.
     * @return boolean If the menu is displayed or not.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);

        for(int i = 0; i < menu.size(); i++){
            Drawable drawable = menu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);
            }
        }

        return true;
    }

    /**
     * Prepare the options menu in the toolbar.
     * @param  menu    The options menu.
     * @return boolean If the menu is displayed or not.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // if there is no URI, hide the "delete" option form the menu.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    /**
     * Handles click event when an item is selected in the menu.
     * @param  item    The menu item selected.
     * @return boolean Whether to process normally (false) or consume locally (true).
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                hideSoftKeyboard(this);
                saveProduct();
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // If there are unsaved changes, setup a dialog to warn the user.
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, navigate to parent activity.
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when the user clicks on the "back" button.
     */
    @Override
    public void onBackPressed() {

        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Click event listener for the discard button.
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };

        // Show dialog for confirmation and pass he event listener.
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Handles the cursor loader initialization.
     * @param  id   Loader id.
     * @param  args
     * @return Loader<Cursor>
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_IMAGE,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    /**
     * Handles when the loader has finished.
     * @param loader The loader.
     * @param cursor The cursor.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);
            int supplierNameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String description = cursor.getString(descriptionColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            float price = cursor.getFloat(priceColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mDescriptionEditText.setText(description);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPriceEditText.setText(Float.toString(price));
            mSupplierNameEditText.setText(supplierName);
            mSupplierEmailEditText.setText(supplierEmail);

            // handle image
            String imageUriString = cursor.getString(imageColumnIndex);
            if (!TextUtils.isEmpty(imageUriString)) {
                mImageUri = Uri.parse(imageUriString);
                ViewTreeObserver viewTreeObserver = mImageView.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        mImageView.setImageBitmap(getBitmapFromUri(mImageUri));
                    }
                });
            }
        }
    }

    /**
     * Handles loader reset.
     * @param loader
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    /**
     * Handle the execution after returning from other activity.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if the activity was the image picker
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            mImageUri = imageUri;
            mImageView.setImageURI(imageUri);
        }
    }

    ///////////////////////
    /// Utility methods ///
    ///////////////////////

    /**
     * Initialize loader if there's a URI.
     */
    private void initLoader() {
        setCurrentUri();
        if (mCurrentProductUri != null)
            getLoaderManager().initLoader(PRODUCT_LOADER_ID, null, this);
    }

    /**
     * Extract the current URI from the intent data.
     */
    private void setCurrentUri() {
        mCurrentProductUri = getIntent().getData();
    }

    /**
     * Set event listeners for the form EditText elements.
     */
    private void setEventListeners() {
        setFormFields();

        mNameEditText.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);

        mBrowseGalleryButton.setOnClickListener(mBrowseGalleryListener);
        mOrderButton.setOnClickListener(mOrderListener);
        mDecrementButton.setOnClickListener(mDecrementListener);
        mIncrementButton.setOnClickListener(mIncrementListener);
    }

    /**
     * Get the increment value from the EditText.
     * @return
     */
    private int getIncrement() {
        return getIntegerValueFromEditText(mIncrementEditText, 1);
    }

    /**
     * Get quantity value from the EditText.
     * @return
     */
    private int getQuantity() {
        return getIntegerValueFromEditText(mQuantityEditText, 0);
    }

    /**
     * Extract an integer value from an EditText or a default value.
     * @param field
     * @param defaultValue
     * @return
     */
    private int getIntegerValueFromEditText(EditText field, int defaultValue) {
        String incrementString = field.getText().toString();
        try {
            return Integer.parseInt(incrementString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get form fields in the UI and set them as class attributes.
     */
    private void setFormFields() {
        // Find all relevant views that we will need to read user input from
        mNameEditText          = (EditText) findViewById(R.id.edit_product_name);
        mDescriptionEditText   = (EditText) findViewById(R.id.edit_product_description);
        mQuantityEditText      = (EditText) findViewById(R.id.edit_product_quantity);
        mIncrementEditText     = (EditText) findViewById(R.id.edit_product_increment);
        mPriceEditText         = (EditText) findViewById(R.id.edit_product_price);
        mSupplierNameEditText  = (EditText) findViewById(R.id.edit_product_supplier_name);
        mSupplierEmailEditText = (EditText) findViewById(R.id.edit_product_supplier_email);
        mBrowseGalleryButton   = (RelativeLayout) findViewById(R.id.edit_product_browse_gallery);
        mOrderButton           = (RelativeLayout) findViewById(R.id.edit_product_order);
        mIncrementButton       = (Button) findViewById(R.id.button_increment);
        mDecrementButton       = (Button) findViewById(R.id.button_decrement);
        mImageView             = (ImageView) findViewById(R.id.image_preview);
    }

    /**
     * Set the title in the toolbar.
     */
    private void setActivityTitle() {
        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));
        }
    }

    /**
     * Build a dialog and attach event listeners to positive and negative buttons.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.dialog_button_delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Delete a product with the current URI, show a Toast message and finish activity.
     */
    private void deleteProduct() {
        int deletedProducts = getContentResolver().delete(mCurrentProductUri, null, null);
        // Show a toast message depending on whether or not the insertion was successful
        if (deletedProducts == 1) {
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_product_successful), Toast.LENGTH_SHORT).show();
        } else {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_delete_product_failed), Toast.LENGTH_SHORT).show();

        }
        finish();
        return;
    }

    /**
     * Show confirmation dialog for unsaved changes.
     * @param discardButtonClickListener
     */
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.dialog_button_discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.dialog_button_keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Get user input from editor and save new product into database.
     */
    private void saveProduct() {
        ContentValues values = getContentValues();
        if (values == null) {
            Toast.makeText(this, mErrorMessage, Toast.LENGTH_LONG).show();
            return;
        }

        Uri newUri = null;
        int updatedProducts = 0;
        if (mCurrentProductUri == null) {
            // Insert a new product into the provider, returning the content URI for the new product.
            newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);
        } else {
            updatedProducts = getContentResolver().update(mCurrentProductUri, values, null, null);
        }

        // Show a toast message depending on whether or not the insertion was successful
        if (newUri != null || updatedProducts == 1) {
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_product_successful), Toast.LENGTH_SHORT).show();
        } else {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_product_failed), Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    /**
     * Builds ContentValues object.
     * @return
     */
    @Nullable
    private ContentValues getContentValues() {
        // Read from input fields and trim trailing white space
        String nameString          = mNameEditText.getText().toString().trim();
        String descriptionString   = mDescriptionEditText.getText().toString().trim();
        String quantityString      = mQuantityEditText.getText().toString().trim();
        String priceString         = mPriceEditText.getText().toString().trim();
        String supplierNameString  = mSupplierNameEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();
        String imageUriString      = mImageUri==null?null:mImageUri.toString();

        // handle empty quantity
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }

        // handle empty price
        float price = 0.0f;
        if (!TextUtils.isEmpty(priceString)) {
            price = Float.parseFloat(priceString);
        }

        if (TextUtils.isEmpty(nameString)) {
            mErrorMessage = "Product name is required";
            return null;
        }

        if (TextUtils.isEmpty(descriptionString)) {
            mErrorMessage = "Product description is required";
            return null;
        }

        if (getIntegerValueFromEditText(mQuantityEditText, MAX_QUANTITY) > MAX_QUANTITY) {
            mErrorMessage = "Maximum quantity is " + MAX_QUANTITY;
            return null;
        }

        if (getIntegerValueFromEditText(mQuantityEditText, MIN_QUANTITY) < MIN_QUANTITY) {
            mErrorMessage = "Minimum quantity is " + MIN_QUANTITY;
            return null;
        }

        if (TextUtils.isEmpty(quantityString)) {
            mErrorMessage = "Product quantity is required";
            return null;
        }

        if (TextUtils.isEmpty(priceString)) {
            mErrorMessage = "Product price is required";
            return null;
        }


        if (TextUtils.isEmpty(supplierNameString)) {
            mErrorMessage = "Supplier name is required";
            return null;
        }

        if (TextUtils.isEmpty(supplierEmailString)) {
            mErrorMessage = "Supplier email is required";
            return null;
        }

        if (mCurrentProductUri != null && TextUtils.isEmpty(imageUriString)) {
            mErrorMessage = "Product image is required";
            return null;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, descriptionString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageUriString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_NAME, supplierNameString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplierEmailString);
        return values;
    }

    /**
     * Launch intent to select an image from gallery.
     */
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryIntent.setType("image/*");

        startActivityForResult(galleryIntent, PICK_IMAGE);
    }

    /**
     * Get bitmap stored from URI
     * @param uri
     * @return
     */
    private Bitmap getBitmapFromUri(Uri uri) {
        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    /**
     * Hide keyboard
     * @param activity
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}