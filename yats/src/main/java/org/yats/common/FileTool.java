package org.yats.common;

import org.apache.commons.io.FileUtils;

import java.io.*;

public class FileTool {

    public static String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public static void writeToTextFile(String filename, String stringToWrite, boolean append) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filename, append));
            out.write(stringToWrite);
            out.close();
        } catch (IOException e) {
            throw new CommonExceptions.FileWriteException(e.getMessage());
        }
    }

    public static String readFromTextFile(String filePath) {
        StringBuffer fileData = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
            }
            reader.close();
            return fileData.toString();
        } catch (IOException e) {
            throw new CommonExceptions.FileReadException(e.getMessage());
        }
    }

    public static void deleteFile(String filename) {
        File f = new File(filename);
        f.delete();
    }

    public static void deleteDirectory(String directoryName) {
        File f = new File(directoryName);
        if(FileTool.exists(directoryName)) {
            try {
                FileUtils.cleanDirectory(f);
                FileUtils.deleteDirectory(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean exists(String filename) {
        File f = new File(filename);
        return f.exists();
    }

    public static void createDirectories(String path) {
        if(exists(path)) return;
        File theFile = new File(path);
        theFile.mkdirs();
    }

    public static void moveToNewFilename(String filename, String filenameBackup) {
        if(FileTool.exists(filenameBackup)) FileTool.deleteFile(filenameBackup);
        if(FileTool.exists(filename)) FileTool.rename(filename, filenameBackup);
    }

    public static void rename(String oldname, String newName) {
        File theFile = new File(oldname);
        File newFile = new File(newName);
        theFile.renameTo(newFile);
    }


    public static String getTail( String filename, int lines) {
        if(!exists(filename)) throw new CommonExceptions.FileReadException("File not found: "+filename);
        File file = new File(filename);
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler =
                    new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;

            for(long filePointer = fileLength; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                if( readByte == 0xA ) {
                    line = line + 1;
                    if (line >= lines) {
                        if (filePointer == fileLength) {
                            continue;
                        }
                        break;
                    }
                } else if( readByte == 0xD ) {
                    line = line + 1;
                    if (line >= lines) {
                        if (filePointer == fileLength - 1) {
                            continue;
                        }
                        break;
                    }
                }
                sb.append( ( char ) readByte );
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch( java.io.FileNotFoundException e ) {
            e.printStackTrace();
            throw new CommonExceptions.FileReadException(e.getMessage());
        } catch( java.io.IOException e ) {
            e.printStackTrace();
            throw new CommonExceptions.FileReadException(e.getMessage());
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                    throw new CommonExceptions.FileReadException(e.getMessage());
                }
        }
    } // getTail...yes, its ugly

}
