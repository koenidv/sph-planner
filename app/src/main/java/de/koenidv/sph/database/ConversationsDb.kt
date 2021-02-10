package de.koenidv.sph.database

import android.database.sqlite.SQLiteDatabase

//  Created by koenidv on 09.01.2021.
class ConversationsDb {

    var writable: SQLiteDatabase = DatabaseHelper.getInstance().writableDatabase

}