/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.exchange.eas;

import android.content.ContentValues;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.exchange.utility.ExchangeTestCase;

import java.io.IOException;
import java.util.ArrayList;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.eas.EasSyncContactsTests exchange
 */
@SmallTest
public class EasSyncContactsTests extends ExchangeTestCase {

    // Return null.
    public void testTryGetStringDataEverythingNull() throws IOException {
        final String result = EasSyncContacts.tryGetStringData(null, null);
        assertNull(result);
    }

    // Return null.
    public void testTryGetStringDataNullContentValues() throws IOException {
        final String result = EasSyncContacts.tryGetStringData(null, "TestColumn");
        assertNull(result);
    }

    // Return null.
    public void testTryGetStringDataNullColumnName() throws IOException {
        final ContentValues contentValues = new ContentValues();
        contentValues.put("test_column", "test_value");
        final String result = EasSyncContacts.tryGetStringData(contentValues, null);
        assertNull(result);
    }

    // Return null.
    public void testTryGetStringDataDoesNotContainColumn() throws IOException {
        final ContentValues contentValues = new ContentValues();
        contentValues.put("test_column", "test_value");
        final String result = EasSyncContacts.tryGetStringData(contentValues, "does_not_exist");
        assertNull(result);
    }

    // Return null.
    public void testTryGetStringDataEmptyColumnValue() throws IOException {
        final String columnName = "test_column";
        final ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, "");
        final String result = EasSyncContacts.tryGetStringData(contentValues, columnName);
        assertNull(result);
    }

    // Return the data type forced to be a string.
    // TODO: Test other data types.
    public void testTryGetStringDataWrongType() throws IOException {
        final String columnName = "test_column";
        final Integer columnValue = new Integer(10);
        final String columnValueAsString = columnValue.toString();
        final ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, columnValue);
        final String result = EasSyncContacts.tryGetStringData(contentValues, columnName);
        assert(result.equals(columnValueAsString));
    }

    // Return the value as a string.
    public void testTryGetStringDataSuccess() throws IOException {
        final String columnName = "test_column";
        final String columnValue = "test_value";
        final ContentValues contentValues = new ContentValues();
        contentValues.put(columnName, columnValue);
        final String result = EasSyncContacts.tryGetStringData(contentValues, columnName);
        assertTrue(result.equals(columnValue));
    }

    // Return null.
    public void testGenerateFileAsNullParameters() throws IOException {
        final String result = EasSyncContacts.generateFileAs(null, null);
        assertNull(result);
    }

    // Should still return null because there is no name and no email.
    public void testGenerateFileAsNullNameValuesAndEmptyList() throws IOException {
        final ArrayList<ContentValues> emailList = new ArrayList<ContentValues>();
        final String result = EasSyncContacts.generateFileAs(null, emailList);
        assertNull(result);
    }

    // Just return the first email address that was passed in.
    public void testGenerateFileAsNullNameValues() throws IOException {
        final ArrayList<ContentValues> emailList = new ArrayList<ContentValues>();
        final ContentValues emailValue = new ContentValues();
        final String emailString = "anthonylee@google.com";
        emailValue.put(Email.DATA, emailString);
        emailList.add(emailValue);
        final String result = EasSyncContacts.generateFileAs(null, emailList);
        assertTrue(result.equals(emailString));
    }

    // Just return the formatted name.
    public void testGenerateFileAsNullEmailValues() throws IOException {
        final ContentValues nameValues = new ContentValues();
        final String firstName = "Joe";
        final String middleName = "Bob";
        final String lastName = "Smith";
        final String suffix = "Jr.";
        nameValues.put(StructuredName.GIVEN_NAME, firstName);
        nameValues.put(StructuredName.FAMILY_NAME, lastName);
        nameValues.put(StructuredName.MIDDLE_NAME, middleName);
        nameValues.put(StructuredName.SUFFIX, suffix);
        final String result = EasSyncContacts.generateFileAs(nameValues, null);
        final String generatedName = lastName + " " + suffix + ", " + firstName + " " + middleName;
        assertTrue(generatedName.equals(result));
    }

    // This will generate a string that is similar to the full string but with no first name.
    public void testGenerateFileAsNullFirstName() throws IOException {
        final ContentValues nameValues = new ContentValues();
        final String middleName = "Bob";
        final String lastName = "Smith";
        final String suffix = "Jr.";
        nameValues.put(StructuredName.FAMILY_NAME, lastName);
        nameValues.put(StructuredName.MIDDLE_NAME, middleName);
        nameValues.put(StructuredName.SUFFIX, suffix);
        final String result = EasSyncContacts.generateFileAs(nameValues, null);
        final String generatedName = lastName + " " + suffix + ", " + middleName;
        assertTrue(generatedName.equals(result));
    }

    // This will generate a string that is missing both the last name and the suffix.
    public void testGenerateFileAsNullLastName() throws IOException {
        final ContentValues nameValues = new ContentValues();
        final String firstName = "Joe";
        final String middleName = "Bob";
        final String suffix = "Jr.";
        nameValues.put(StructuredName.GIVEN_NAME, firstName);
        nameValues.put(StructuredName.MIDDLE_NAME, middleName);
        nameValues.put(StructuredName.SUFFIX, suffix);
        final String result = EasSyncContacts.generateFileAs(nameValues, null);
        final String generatedName = firstName + " " + middleName;
        assertTrue(generatedName.equals(result));
    }

    // This will generate a string that is similar to the full name but missing the middle name.
    public void testGenerateFileAsNullMiddleName() throws IOException {
        final ContentValues nameValues = new ContentValues();
        final String firstName = "Joe";
        final String lastName = "Smith";
        final String suffix = "Jr.";
        nameValues.put(StructuredName.GIVEN_NAME, firstName);
        nameValues.put(StructuredName.FAMILY_NAME, lastName);
        nameValues.put(StructuredName.SUFFIX, suffix);
        final String result = EasSyncContacts.generateFileAs(nameValues, null);
        final String generatedName = lastName + " " + suffix + ", " + firstName;
        assertTrue(generatedName.equals(result));
    }

    // Similar to the full name but no suffix.
    public void testGenerateFileAsNullSuffix() throws IOException {
        final ContentValues nameValues = new ContentValues();
        final String firstName = "Joe";
        final String middleName = "Bob";
        final String lastName = "Smith";
        nameValues.put(StructuredName.GIVEN_NAME, firstName);
        nameValues.put(StructuredName.FAMILY_NAME, lastName);
        nameValues.put(StructuredName.MIDDLE_NAME, middleName);
        final String result = EasSyncContacts.generateFileAs(nameValues, null);
        final String generatedName = lastName + ", " + firstName + " " + middleName;
        assertTrue(generatedName.equals(result));
    }
}
