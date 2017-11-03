package com.example.android.inventoryapp.data;
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

import android.net.Uri;
import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * API Contract for the Product.
 */
public final class ProductContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ProductContract() {}

    /** The "Content authority" */
    public static final String CONTENT_AUTHORITY = "com.example.android.inventoryapp";

    /** Base content URI */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /** Path to get products */
    public static final String PATH_PRODUCTS = "products";

    /**
     * Inner class that defines constant values for the products database table.
     */
    public static final class ProductEntry implements BaseColumns {

        /** The content URI to access the product data in the provider */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        /**
         * The MIME type of the {@link #CONTENT_URI} for a list of products.
         */
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * The MIME type of the {@link #CONTENT_URI} for a single product.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /** Database table for products */
        public final static String TABLE_NAME = "products";

        /** Unique ID number for the product */
        public final static String _ID = BaseColumns._ID;

        /** Name of the product */
        public final static String COLUMN_PRODUCT_NAME ="name";

        /** Product description. */
        public final static String COLUMN_PRODUCT_DESCRIPTION = "description";

        /** Product quantity */
        public final static String COLUMN_PRODUCT_QUANTITY = "quantity";

        /** Product price */
        public final static String COLUMN_PRODUCT_PRICE = "price";

        /** Product image */
        public final static String COLUMN_PRODUCT_IMAGE = "image";

        /** Product supplier name */
        public final static String COLUMN_PRODUCT_SUPPLIER_NAME = "supplier_name";

        /** Product supplier email */
        public final static String COLUMN_PRODUCT_SUPPLIER_EMAIL = "supplier_email";


    }

}

