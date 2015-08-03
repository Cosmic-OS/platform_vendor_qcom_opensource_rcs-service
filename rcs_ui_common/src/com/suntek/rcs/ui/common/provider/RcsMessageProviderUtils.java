/*
 * Copyright (c) 2015 pci-suntektech Technologies, Inc.  All Rights Reserved.
 * pci-suntektech Technologies Proprietary and Confidential.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.suntek.rcs.ui.common.provider;

import android.database.sqlite.SQLiteDatabase;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.MmsSms.PendingMessages;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Sms.Conversations;
import android.provider.Telephony.Threads;
import android.provider.Telephony.ThreadsColumns;
import android.telephony.SubscriptionManager;

import com.suntek.mway.rcs.client.aidl.common.RcsColumns;

public class RcsMessageProviderUtils {

    // add rcs rebuild columns.
    public static void upgradeDatabaseToVersion65(SQLiteDatabase db) {
        // backup the old table
        db.execSQL("ALTER TABLE threads RENAME TO threads_old;");
        // create new table
        createRcsThreadsTable(db);
        // insert old data to new table
        db.execSQL("INSERT INTO threads (" +
                Threads._ID + "," +
                Threads.DATE + "," +
                Threads.MESSAGE_COUNT + "," +
                Threads.RECIPIENT_IDS + "," +
                Threads.SNIPPET + "," +
                Threads.SNIPPET_CHARSET + "," +
                Threads.READ + "," +
                Threads.ARCHIVED + "," +
                Threads.TYPE + "," +
                Threads.ERROR + "," +
                Threads.HAS_ATTACHMENT + "," +
                RcsColumns.ThreadColumns.RCS_TOP + "," +  // rename from top;
                RcsColumns.ThreadColumns.RCS_TOP_TIME +  // rename from top time.
                ")" +
                "SELECT " +
                Threads._ID + "," +
                Threads.DATE + "," +
                Threads.MESSAGE_COUNT + "," +
                Threads.RECIPIENT_IDS + "," +
                Threads.SNIPPET + "," +
                Threads.SNIPPET_CHARSET + "," +
                Threads.READ + "," +
                Threads.ARCHIVED + "," +
                Threads.TYPE + "," +
                Threads.ERROR + "," +
                Threads.HAS_ATTACHMENT + "," +
                 "top," +
                "top_time " +
                " FROM threads_old;");
        db.execSQL("update threads set " + RcsColumns.ThreadColumns.RCS_CHAT_TYPE +
                " = 1;");
        db.execSQL("DROP TABLE threads_old;");

        // backup old table data
        db.execSQL("ALTER TABLE sms RENAME TO sms_old;");
        db.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_on_insert;");
        db.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_date_subject_on_update;");
        db.execSQL("DROP TRIGGER IF EXISTS sms_update_thread_read_on_update;");
        db.execSQL("DROP TRIGGER IF EXISTS update_threads_error_on_update_sms;");
        db.execSQL("DROP TRIGGER IF EXISTS sms_words_update;");
        db.execSQL("DROP TRIGGER IF EXISTS sms_words_delete;");
        // create new table and trigger.
        createRcsSmsTable(db);
        createSmsTrigger65(db);
        db.execSQL("INSERT INTO sms (" +
                "_id," +
                "thread_id," +
                "address," +
                "person," +
                "date," +
                "date_sent," +
                "protocol," +
                "read," +
                "status," +
                "type," +
                "reply_path_present," +
                "subject," +
                "body," +
                "service_center," +
                "locked," +
                "sub_id, " +
                "phone_id, " +
                "error_code," +
                "creator," +
                "seen," +
                "priority," +
                "favourite," +
                "rcs_message_id," +
                "rcs_file_name," +
                "rcs_mime_type," +
                "rcs_msg_type," +
                "rcs_msg_state," +
                "rcs_chat_type," +
                "rcs_conversation_id," +
                "rcs_contribution_id," +
                "rcs_file_selector," +
                "rcs_file_transfered," +
                "rcs_file_transfer_id, " +
                "rcs_file_icon," +
                "rcs_burn," +
                "rcs_header," +
                "rcs_file_path," +
                "rcs_is_download," +
                "rcs_file_size," +
                "rcs_thumb_path," +
                "rcs_burn_body " +
                ")" +
                // select old data to create new table.
                "SELECT " +
                "_id," +
                "thread_id," +
                "address," +
                "person," +
                "date," +
                "date_sent," +
                "protocol," +
                "read," +
                "status," +
                "type," +
                "reply_path_present," +
                "subject," +
                "body," +
                "service_center," +
                "locked," +
                "sub_id, " +
                "phone_id, " +
                "error_code," +
                "creator," +
                "seen," +
                "priority," +
                "favourite," +
                "rcs_message_id," +
                "rcs_filename," +
                "rcs_mime_type," +
                "rcs_msg_type," +
                "rcs_msg_state," +
                "rcs_chat_type," +
                "rcs_conversation_id," +
                "rcs_contribution_id," +
                "rcs_file_selector," +
                "rcs_file_transfer_ext," +
                "rcs_file_transfer_id," +
                "rcs_file_icon," +
                "rcs_is_burn," +
                "rcs_header," +
                "rcs_path," +
                "rcs_is_download," +
                "rcs_file_size," +
                "rcs_thumb_path," +
                "rcs_burn_body " +
                " FROM sms_old;"
                );
        db.execSQL("DROP TABLE sms_old;");
        db.execSQL("update sms set " + RcsColumns.SmsRcsColumns.RCS_CHAT_TYPE +
                " = 1;");
        createRcsOneToManyMesageStatusTable(db);
        createRcsThreadUpdateTriggers(db);
    }
    /**
     * create a table to save rcs 1-N message status.
     */
    public static void createRcsOneToManyMesageStatusTable(SQLiteDatabase db) {
         db.execSQL("CREATE TABLE group_status (" +
                 RcsColumns.GroupStatusColumns._ID + " INTEGER PRIMARY KEY,"+
                 RcsColumns.GroupStatusColumns.MSG_ID + " INTEGER DEFAULT -1,"+
                 RcsColumns.GroupStatusColumns.GROUP_DATE + " INTEGER DEFAULT 0,"+
                 RcsColumns.GroupStatusColumns.GROUP_NUMBER + " TEXT,"+
                 RcsColumns.GroupStatusColumns.GROUP_STATUS + " INTEGER DEFAULT 0 "+
                 ");");
    }

    public static void createRcsThreadUpdateTriggers(SQLiteDatabase db) {
        db.execSQL("CREATE TRIGGER sms_update_thread_rcs_message_info_on_insert" +
                " AFTER INSERT ON sms "
                + RcsMessageProviderConstants.SMS_UPDATE_THREAD_RCS_MESSAGE_INFO_ON_NEW);
    }

    public static void createRcsThreadsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE threads (" +
                Threads._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Threads.DATE + " INTEGER DEFAULT 0," +
                Threads.MESSAGE_COUNT + " INTEGER DEFAULT 0," +
                Threads.RECIPIENT_IDS + " TEXT," +
                Threads.SNIPPET + " TEXT," +
                Threads.SNIPPET_CHARSET + " INTEGER DEFAULT 0," +
                Threads.READ + " INTEGER DEFAULT 1," +
                Threads.ARCHIVED + " INTEGER DEFAULT 0," +
                Threads.TYPE + " INTEGER DEFAULT 0," +
                Threads.ERROR + " INTEGER DEFAULT 0," +
                Threads.HAS_ATTACHMENT + " INTEGER DEFAULT 0," +
                RcsColumns.ThreadColumns.RCS_TOP + " INTEGER DEFAULT 0," +
                RcsColumns.ThreadColumns.RCS_TOP_TIME + " INTEGER DEFAULT 0," +
                RcsColumns.ThreadColumns.RCS_NUMBER + " TEXT," +
                RcsColumns.ThreadColumns.RCS_MSG_ID + " INTEGER  DEFAULT -1," +
                RcsColumns.ThreadColumns.RCS_MSG_TYPE + " INTEGER  DEFAULT -1," +
                RcsColumns.ThreadColumns.RCS_CHAT_TYPE + " INTEGER  DEFAULT -1"+
                ");");
    }

    public static void createRcsSmsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sms (" +
                "_id INTEGER PRIMARY KEY," +
                "thread_id INTEGER," +
                "address TEXT," +
                "person INTEGER," +
                "date INTEGER," +
                "date_sent INTEGER DEFAULT 0," +
                "protocol INTEGER," +
                "read INTEGER DEFAULT 0," +
                "status INTEGER DEFAULT -1," + // a TP-Status value
                                               // or -1 if it
                                               // status hasn't
                                               // been received
                "type INTEGER," +
                "reply_path_present INTEGER," +
                "subject TEXT," +
                "body TEXT," +
                "service_center TEXT," +
                "locked INTEGER DEFAULT 0," +
                "sub_id INTEGER DEFAULT " + SubscriptionManager.INVALID_SUBSCRIPTION_ID + ", " +
                "phone_id INTEGER DEFAULT -1, " +
                "error_code INTEGER DEFAULT 0," +
                "creator TEXT," +
                "seen INTEGER DEFAULT 0," +
                "priority INTEGER DEFAULT -1," +
                "favourite INTEGER DEFAULT 0,"+
                "rcs_message_id TEXT," +
                "rcs_file_name TEXT," +
                "rcs_mime_type TEXT," +
                "rcs_msg_type INTEGER DEFAULT -1," +
                "rcs_msg_state INTEGER," +
                "rcs_chat_type INTEGER DEFAULT -1," +
                "rcs_conversation_id TEXT," +
                "rcs_contribution_id TEXT," +
                "rcs_file_selector TEXT," +
                "rcs_file_transfered TEXT," +
                "rcs_file_transfer_id TEXT," +
                "rcs_file_icon TEXT," +
                "rcs_burn INTEGER  DEFAULT -1," +
                "rcs_header TEXT," +
                "rcs_file_path TEXT," +
                "rcs_is_download INTEGER DEFAULT 0," +
                "rcs_file_size INTEGER DEFAULT 0," +
                "rcs_thumb_path TEXT," +
                "rcs_burn_body TEXT," +
                "rcs_media_played INTEGER DEFAULT 0," +
                "rcs_ext_contact TEXT," +
                "rcs_file_record INTEGER " +
                ");");
    }

    public static void createSmsTrigger65(SQLiteDatabase db) {
        // Updates threads table whenever a message is added to sms.
        db.execSQL("CREATE TRIGGER sms_update_thread_on_insert AFTER INSERT ON sms " +
                RcsMessageProviderConstants.SMS_UPDATE_THREAD_DATE_SNIPPET_COUNT_ON_UPDATE);

        // Updates threads table whenever a message in sms is updated.
        db.execSQL("CREATE TRIGGER sms_update_thread_date_subject_on_update AFTER" +
                   "  UPDATE OF " + Sms.DATE + ", " + Sms.BODY + ", " + Sms.TYPE +
                   "  ON sms " +
                   RcsMessageProviderConstants.SMS_UPDATE_THREAD_DATE_SNIPPET_COUNT_ON_UPDATE);

        // Updates threads table whenever a message in sms is updated.
        db.execSQL("CREATE TRIGGER sms_update_thread_read_on_update AFTER" +
                   "  UPDATE OF " + Sms.READ +
                   "  ON sms " +
                   "BEGIN " +
                   RcsMessageProviderConstants.SMS_UPDATE_THREAD_READ_BODY +
                   "END;");
        // Update the error flag of threads after a text message was
        // failed to send/receive.
        db.execSQL("CREATE TRIGGER update_threads_error_on_update_sms " +
                   "  AFTER UPDATE OF type ON sms" +
                   "  WHEN (OLD.type != 5 AND NEW.type = 5)" +
                   "    OR (OLD.type = 5 AND NEW.type != 5) " +
                   "BEGIN " +
                   "  UPDATE threads SET error = " +
                   "    CASE" +
                   "      WHEN NEW.type = 5 THEN error + 1" +
                   "      ELSE error - 1" +
                   "    END " +
                   "  WHERE _id = NEW.thread_id; " +
                   "END;");
        // monitor the sms table
        // NOTE don't handle inserts using a trigger because it has an unwanted
        // side effect:  the value returned for the last row ends up being the
        // id of one of the trigger insert not the original row insert.
        // Handle inserts manually in the provider.
        db.execSQL("CREATE TRIGGER sms_words_update AFTER UPDATE ON sms BEGIN UPDATE words " +
                " SET index_text = NEW.body WHERE (source_id=NEW._id AND table_to_use=1); " +
                " END;");
        db.execSQL("CREATE TRIGGER sms_words_delete AFTER DELETE ON sms BEGIN DELETE FROM " +
                "  words WHERE source_id = OLD._id AND table_to_use = 1; END;");
    }
}
