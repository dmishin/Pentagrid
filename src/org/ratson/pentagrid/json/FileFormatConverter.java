package org.ratson.pentagrid.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.TotalisticRule;
import org.ratson.pentagrid.fields.SimpleMapField;
import org.ratson.util.Pair;

import com.google.gson.stream.JsonWriter;

public class FileFormatConverter {
	private static Pair<SimpleMapField, TotalisticRule> loadFieldData( File f ) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream( f )));
		Object o = ois.readObject();
		Object rule = ois.readObject();
		ois.close();
		try{
			return new Pair<SimpleMapField, TotalisticRule>( (SimpleMapField)o, (TotalisticRule) rule );
		}catch(ClassCastException e){
			throw new IOException("File has wrong format");
		}
	}
	private static void saveFieldGson( File f, Field fld, TotalisticRule rule ) throws IOException{
		GZIPOutputStream gzout = new GZIPOutputStream( new FileOutputStream( f ));
		JsonWriter jsw = new JsonWriter(new OutputStreamWriter(gzout));
		try{
			GSONSerializer.writeField( jsw, fld, rule );
		}finally{
			jsw.close();
			gzout.close();
		}
	}
	
	public static boolean convertFile( File sgzFile, File jsongzFile ){
		try {
			Pair<SimpleMapField, TotalisticRule> fld_rule = loadFieldData( sgzFile );
			
			saveFieldGson(jsongzFile, fld_rule.left, fld_rule.right);
			return true;
		} catch (FileNotFoundException e) {
			System.err.println( "File not found "+e.getMessage());
		} catch (IOException e) {
			System.err.println( e.getMessage() );
		} catch (ClassNotFoundException e) {
			System.err.println( e.getMessage() );
		}
		return false;
	}
	public static void main(String[] args) {
		if( args.length == 0){
			System.out.println("Usage: convert file1.sgz file2.sgz ...");
		}
		for (int i = 0; i < args.length; i++) {
			File f = new File(args[i]);
			String name = f.getName();
			int pointIdx = name.lastIndexOf(".");
			String newName = null;
			if ( pointIdx == -1 )
				newName = name + ".jsongz";
			else
				newName = name.substring(0, pointIdx) + ".jsongz";
			File outFile = new File( f.getParentFile(), newName ); 
			
			System.out.println("Converting file "+f+" to "+outFile);
			if (convertFile( f, outFile ) )
				System.out.println("Success");
			else
				System.out.println("Failure");
		}
	}
}
