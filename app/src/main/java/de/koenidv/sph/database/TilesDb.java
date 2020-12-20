package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.koenidv.sph.objects.Tile;

public class TilesDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static TilesDb instance;

    private TilesDb() {
    }

    public static TilesDb getInstance() {
        if (TilesDb.instance == null) {
            TilesDb.instance = new TilesDb();
        }
        return TilesDb.instance;
    }


    public List<Tile> getAllTiles() {
        List<Tile> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM tiles";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String location = cursor.getString(1);
                String type = cursor.getString(2);
                String icon = cursor.getString(3);
                int color = cursor.getInt(4);

                Tile newTile = new Tile(name, location, type, icon, color);

                returnList.add(newTile);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return returnList;
    }

    /**
     * Get a List of Tiles with a specific type
     *
     * @param type Feature type to query for
     * @return List of all Tiles in the db with the given type
     */
    public List<Tile> getTilesByType(String type) {
        List<Tile> returnList = new ArrayList<>();
        final SQLiteDatabase db = dbhelper.getReadableDatabase();

        String queryString = "SELECT * FROM tiles WHERE type = '" + type + "'";

        Cursor cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                String location = cursor.getString(1);
                String tiletype = cursor.getString(2);
                String icon = cursor.getString(3);
                int color = cursor.getInt(4);

                Tile newTile = new Tile(name, location, tiletype, icon, color);

                returnList.add(newTile);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return returnList;
    }

    /**
     * Adds or updates feature tiles in the database
     * Will override everything with the same name if it's not null
     *
     * @param tiles List of tiles to be added or updated
     */
    public void save(List<Tile> tiles) {
        for (Tile tile : tiles) {
            save(tile);
        }
    }


    public void save(Tile tile) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("name", tile.getName());
        cv.put("location", tile.getLocation());
        if (tile.getType() != null) cv.put("type", tile.getType());
        cv.put("color", tile.getColor());
        cv.put("icon", tile.getIcon());

        Cursor cursor = db.rawQuery("SELECT * FROM tiles WHERE name = '" + tile.getName() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("tiles", null, cv);
        } else {
            db.update("tiles", cv, "name = '" + tile.getName() + "'", null);
        }
        cursor.close();
    }


}
