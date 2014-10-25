package org.ratson.pentagrid.json;

import java.io.IOException;
import java.util.LinkedList;

import org.ratson.pentagrid.StrobingRule;
import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.Rule;
import org.ratson.pentagrid.RuleSyntaxException;
import org.ratson.pentagrid.TotalisticRule;
import org.ratson.pentagrid.fields.SimpleMapField;
import org.ratson.util.Pair;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**Serializer that uses Google GSON lbrary for reading/writing JSON files*/
public class GSONSerializer {
	private final static String STATE = "state";
	private final static String VERSION = "version";
	private final static String DESCRIPTION = "description";
	private static final String RULE = "rule";
	private static final String CELLS = "cells";
	private static final String PATH = "path";
	
	private GSONSerializer(){};
	public final static int FORMAT_VERSION = 1;
	public static void writeField( JsonWriter jg, Field fld, TotalisticRule rule ) throws IOException{
		jg.beginObject();
		jg.name( VERSION );     jg.value( FORMAT_VERSION );
		jg.name( DESCRIPTION ); jg.value( "Hyperbolic cellular field" );
		jg.name( RULE );        writeRule( jg, rule );
		jg.name( STATE );       jg.value( fld.getFieldState() );
		jg.name( CELLS );		writeCells( jg, fld );
		jg.endObject();
	}
	private static void writeRule( JsonWriter jg, TotalisticRule rule) throws IOException {
		if (rule instanceof Rule) 
			writeSimpleRule( jg, (Rule)rule );
		else if (rule instanceof StrobingRule)
			writeSimpleRule( jg, ((StrobingRule)rule).getUnderlyingRule());
		else throw new RuntimeException("Rule type is not supproted by serializer");
	}
	private static void writeSimpleRule(JsonWriter jg, Rule rule) throws IOException {
		jg.value( rule.getCode()  );
	}
	private static void writeCells(JsonWriter jg, Field fld) throws IOException {
		jg.beginArray();
		for( Path p : fld.getAliveCells()){
			int state = fld.getCell(p);
			writeCell( jg, p, state );
		}
		jg.endArray();
	}
	private static void writeCell(JsonWriter jg, Path p, int state) throws IOException {
		jg.beginObject();
		jg.name( STATE );   jg.value(state);
		jg.name( PATH); 	writePath( jg, p );
		jg.endObject();
	}
	private static void writePath(JsonWriter jg, Path p) throws IOException {
		jg.beginArray();
		Path pCur = p;
		while (!pCur.isRoot()){
			jg.value(pCur.getIndex());
			pCur = pCur.getTail();
		}
		jg.endArray();
	}
	/////////////////////////////////////////////////
	// Reading
	/////////////////////////////////////////////////
	public static Pair<SimpleMapField, TotalisticRule> readField(JsonReader jp) throws  IOException, FileFormatException {
		int version = -1;
		int state = -1;
		TotalisticRule r = null;
		SimpleMapField map = new SimpleMapField();
		try{
			jp.beginObject();
			while (jp.hasNext()) {
				String fieldname = jp.nextName();
				if ( VERSION.equals( fieldname ) ){
					version = jp.nextInt();
					if (version > FORMAT_VERSION)
						System.err.println("Warning: reading future version: "+version +" current verison is "+FORMAT_VERSION);
				}else if( DESCRIPTION.equals( fieldname ) ){
					//ignore it
					jp.skipValue();
				}else if( STATE.equals( fieldname ) ){
					state = jp.nextInt();
				}else if( RULE.equals(fieldname)){
					r = readRule( jp );
				}else if( CELLS.equals(fieldname)){
					readCells( jp, map );
				}else{
					System.err.println("Unexpected field: "+fieldname);
					jp.skipValue();
				}
			}
			jp.endObject();
		}catch( IllegalStateException exc ){
			throw new FileFormatException("JSON format invalid:"+exc.getMessage());
		}
		if ( version == -1) System.err.println("Warning: version not specified");
		if ( state == -1) 
			System.err.println("Warning: state not specified");
		else 
			map.setFieldState(state);
		
		if ( r == null ) {
			r = new Rule(new int[]{3}, new int[]{2,3});
			System.err.println( "Rule not specified, using default:"+r.getCode() );
		}
		return new Pair<SimpleMapField, TotalisticRule>( map, r );
	}
	
	private static void readCells(JsonReader jp, SimpleMapField map) throws FileFormatException, IOException {
		jp.beginArray();
		while( jp.hasNext() ){
			readCell( jp, map );
		}
		jp.endArray();
	}
	
	private static void readCell(JsonReader jp, SimpleMapField map) throws FileFormatException, IOException {
		//must be an object: cell record
		jp.beginObject();
		int state = -1;
		Path p = null;
		while ( jp.hasNext() ){
			String name = jp.nextName();
			if( STATE.equals(name)){
				state = jp.nextInt();
			}else if( PATH.equals(name)){
				p = readPath( jp );
			}else{
				System.err.println("Unexpected tag in the cell:"+name);
				jp.skipValue();
			}
		}
		jp.endObject();
		if( state == -1 ) throw new FileFormatException("State for cell not specified");
		if( p == null ) throw new FileFormatException("PAth for cell not specified");
		map.setCell(p, state);
	}
	
	private static Path readPath(JsonReader jp) throws FileFormatException, IOException {
		jp.beginArray();
		/*non-recursive reading of the path from JSON array*/
		LinkedList<Integer> path = new LinkedList<Integer>();
		while( jp.hasNext() )
			path.addFirst( jp.nextInt() );
		jp.endArray();
		Path p = Path.getRoot();
		for( int i : path )
		    p = p.child( i );
		if ( ! p.isCorrect() ) throw new FileFormatException("Path is invalid:"+p );
		return p;
	}
	private static TotalisticRule readRule(JsonReader jp) throws  IOException, FileFormatException {
		try {
			String code = jp.nextString();
			Rule r = Rule.parseRule( code );
			if (!r.isVacuumStable()){
				if (r.isValidDayNight())
					return new StrobingRule( r );
				System.err.println("Warning: rule "+code+" is invalid. Loading anyways.");
			}
			return r;
		} catch (RuleSyntaxException e) {
			throw new FileFormatException("Incorrect rule format:"+e.getMessage());
		}
	}
}
