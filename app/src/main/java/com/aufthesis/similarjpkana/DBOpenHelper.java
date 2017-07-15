package com.aufthesis.similarjpkana;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// Created by yoichi75jp2 on 2017/03/04.
class DBOpenHelper extends SQLiteOpenHelper
{
    static private int DB_VERSION = 1;  //2017/04/25 ver.1
    final static private String DB_NAME = "kana";
    final static private String DB_NAME_ASSET = "kana.db";
    final static private String DB_NAME_ZIP = "kana.zip";
    final static private String FILE_DIR_PATH = Environment.getExternalStorageDirectory() + "/archive/";
    final static private int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private SQLiteDatabase m_Database;
    private  Context m_Context;
    private  File m_DatabasePath;

    DBOpenHelper(Context context)
    {
        super(context, DB_NAME, null, DB_VERSION);
        m_Context = context;
        m_DatabasePath = m_Context.getDatabasePath(DB_NAME);
        m_Database = null;
    }

    /**
     * asset に格納したデータベースをコピーするための空のデータベースを作成する
     **/
    private void createEmptyDataBase() throws IOException
    {
        boolean dbExist = checkDataBaseExists();

        // すでにデータベースは作成されている場合はDoNothing
        if(!dbExist)
        {
            // このメソッドを呼ぶことで、空のデータベースがアプリのデフォルトシステムパスに作られる
            getReadableDatabase();
            try
            {
                // asset に格納したデータベースをコピーする
                copyDataBaseFromAsset();

                String dbPath = m_DatabasePath.getAbsolutePath();
                SQLiteDatabase checkDb = null;
                try
                {
                    checkDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
                }
                catch(SQLiteException e)
                {
                    Log.e("TAG", e.getMessage());
                }
                if(checkDb != null)
                {
                    checkDb.setVersion(DB_VERSION);
                    checkDb.close();
                }
            }
            catch(IOException e)
            {
                throw new Error("Error copying database");
            }
        }
    }

    /**
     * 再コピーを防止するために、すでにデータベースがあるかどうか判定する
     *
     * @return 存在している場合 {@code true}
     **/
    private boolean checkDataBaseExists()
    {
        String dbPath = m_DatabasePath.getAbsolutePath();

        SQLiteDatabase checkDb;
        try
        {
            checkDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
        }
        catch(SQLiteException e)
        {
            // データベースはまだ存在していない
            return false;
        }

        int oldVersion = checkDb.getVersion();
        int newVersion = DB_VERSION;

        if(oldVersion == newVersion)
        {
            // データベースは存在していて最新
            checkDb.close();
            return true;
        }

        // データベースが存在していて最新ではないので削除
        File f = new File(dbPath);
        f.delete();
        return false;
    }

    /**
     * asset に格納したデーだベースをデフォルトのデータベースパスに作成したからのデータベースにコピーする
     **/
    private void copyDataBaseFromAsset() throws IOException
    {
        //DB.zipを解凍
        extractZipFiles(DB_NAME_ZIP);

        // asset 内のデータベースファイルにアクセス
        //InputStream mInput = m_Context.getAssets().open(DB_NAME_ASSET);
        File inputFile = new File(Environment.getDataDirectory().getPath() + "/data/" + m_Context.getPackageName() + "/databases/" + DB_NAME_ASSET);
        InputStream mInput = new FileInputStream(inputFile);

        // デフォルトのデータベースパスに作成した空のDB
        OutputStream mOutput = new FileOutputStream(m_DatabasePath);

        // コピー
        byte[] buffer = new byte[1024];
        int size;
        while((size = mInput.read(buffer)) > 0)
        {
            mOutput.write(buffer, 0, size);
        }
        // Close the streams
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    private SQLiteDatabase openDataBase() throws SQLException
    {
        return getReadableDatabase();
    }

    //ZIPフィアル（DB）の結合（１MBオーバー対策）
    private void extractZipFiles(String zipName)
    {
        try
        {
            AssetManager assetManager = m_Context.getAssets();
            InputStream inputStream = assetManager.open(zipName, AssetManager.ACCESS_STREAMING);

            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            //保存ディレクトリを作成する。
            File fileDir = new File(FILE_DIR_PATH);
            fileDir.mkdirs();

            while(zipEntry != null)
            {
                //String entryName = zipEntry.getName();
                int n;
                FileOutputStream fileOutputStream;

                //DB領域
                //保存ディレクトの指定
                File file = new File(Environment.getDataDirectory().getPath() + "/data/" + m_Context.getPackageName() + "/databases/" + DB_NAME_ASSET);
                fileOutputStream = new FileOutputStream(file);

                byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
                while ((n = zipInputStream.read(buf, 0, DEFAULT_BUFFER_SIZE)) > -1)
                {
                    fileOutputStream.write(buf, 0, n);
                }

                fileOutputStream.close();
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.close();
        }
        catch(Exception e)
        {
            Log.e("TAG", e.getMessage());
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {

    }

    SQLiteDatabase getDataBase()
    {
        try
        {
            if(m_Database == null)
            {
                 //asset に格納したデータベースをコピーするための空のデータベースを作成する
                createEmptyDataBase();
                m_Database = openDataBase();
            }
        }
        catch (IOException ioe)
        {
            throw new Error("Unable to create database");
        }
        catch(SQLException sqle)
        {
            throw new Error(sqle.getMessage());
        }
        return m_Database;
    }
}



