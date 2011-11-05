package org.ratson.pentagrid.json;

import java.io.IOException;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.ratson.pentagrid.DayNightRule;
import org.ratson.pentagrid.Field;
import org.ratson.pentagrid.Path;
import org.ratson.pentagrid.Rule;
import org.ratson.pentagrid.RuleSyntaxException;
import org.ratson.pentagrid.TotalisticRule;
import org.ratson.pentagrid.fields.SimpleMapField;
import org.ratson.util.Pair;

public class JSONSerializer {
	private JSONSerializer(){};
	public final static int FORMAT_VERSION = 1;
	public static void writeField( JsonGenerator jg, Field fld, TotalisticRule rule ) throws JsonGenerationException, IOException{
		jg.writeStartObject();
		jg.writeNumberField( "version", FORMAT_VERSION );
		jg.writeStringField( "description", "Hyperbolic cellular field" );
		jg.writeFieldName( "rule" );
		writeRule( jg, rule );
		jg.writeNumberField( "state", fld.getFieldState() );
		jg.writeFieldName( "cells" );		
		writeCells( jg, fld );
		jg.writeEndObject();
	}
	private static void writeRule( JsonGenerator jg, TotalisticRule rule) throws JsonGenerationException, IOException {
		if (rule instanceof Rule) 
			writeSimpleRule( jg, (Rule)rule );
		else if (rule instanceof DayNightRule)
			writeSimpleRule( jg, ((DayNightRule)rule).getUnderlyingRule());
		else throw new RuntimeException("Rule type is not supproted by serializer");
	}
	private static void writeSimpleRule(JsonGenerator jg, Rule rule) throws JsonGenerationException, IOException {
		jg.writeString( rule.getCode() );
	}
	private static void writeCells(JsonGenerator jg, Field fld) throws JsonGenerationException, IOException {
		jg.writeStartArray();
		for( Path p : fld.getAliveCells()){
			int state = fld.getCell(p);
			writeCell( jg, p, state );
		}
		jg.writeEndArray();
	}
	private static void writeCell(JsonGenerator jg, Path p, int state) throws JsonGenerationException, IOException {
		jg.writeStartObject();
		jg.writeNumberField( "state", state);
		jg.writeFieldName("path");
		writePath( jg, p );
		jg.writeEndObject();
	}
	private static void writePath(JsonGenerator jg, Path p) throws JsonGenerationException, IOException {
		jg.writeStartArray();
		Path pCur = p;
		while (!pCur.isRoot()){
			jg.writeNumber(pCur.getIndex());
			pCur = pCur.getTail();
		}
		jg.writeEndArray();
	}
	public static Pair<SimpleMapField, TotalisticRule> readField(JsonParser jp) throws JsonParseException, IOException, FileFormatException {
		int version = -1;
		int state = -1;
		TotalisticRule r = null;
		SimpleMapField map = new SimpleMapField();
		assertToken( jp.nextToken(), JsonToken.START_OBJECT );
		while (jp.nextToken() != JsonToken.END_OBJECT) {
			String fieldname = jp.getCurrentName();
			jp.nextToken();
			if ( "version".equals( fieldname ) ){
				version = jp.getIntValue();
				if (version > FORMAT_VERSION)
					System.err.println("Warning: reading future version: "+version +" current verison is "+FORMAT_VERSION);
			}else if( "description".equals( fieldname ) ){
				//ignore it
			}else if( "state".equals( fieldname ) ){
				state = jp.getIntValue();
			}else if( "rule".equals(fieldname)){
				r = readRule( jp );
			}else if ("cells".equals(fieldname)){
				readCells( jp, map );
			}else{
				System.err.println("Unexpected field: "+fieldname);
				jp.skipChildren();
			}
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
	
	private static void readCells(JsonParser jp, SimpleMapField map) throws FileFormatException, JsonParseException, IOException {
		assertToken(jp.getCurrentToken(), JsonToken.START_ARRAY);
		while( jp.nextToken() != JsonToken.END_ARRAY){
			readCell( jp, map );
		}
	}
	
	private static void readCell(JsonParser jp, SimpleMapField map) throws FileFormatException, JsonParseException, IOException {
		//must be an object: cell record
		assertToken(jp.getCurrentToken(), JsonToken.START_OBJECT);
		int state = -1;
		Path p = null;
		while ( jp.nextToken() != JsonToken.END_OBJECT){
			String name = jp.getCurrentName();
			jp.nextToken();
			if( "state".equals(name)){
				state = jp.getIntValue();
			}else if( "path".equals(name)){
				p = readPath( jp );
			}else{
				System.err.println("Unexpected tag in the cell:"+name);
				jp.skipChildren();
			}
		}
		if( state == -1 ) throw new FileFormatException("State for cell not specified");
		if( p == null ) throw new FileFormatException("PAth for cell not specified");
		map.setCell(p, state);
	}
	
	private static Path readPath(JsonParser jp) throws FileFormatException, JsonParseException, IOException {
		assertToken(jp.getCurrentToken(), JsonToken.START_ARRAY);
		jp.nextToken();
		return readPathRecursively( jp );
	}
	
	private static Path readPathRecursively(JsonParser jp) throws JsonParseException, IOException {
		if (jp.getCurrentToken() == JsonToken.END_ARRAY ) return Path.getRoot();
		int idx = jp.getIntValue();
		jp.nextToken();
		return readPathRecursively(jp).child(idx);
	}
	
	private static TotalisticRule readRule(JsonParser jp) throws JsonParseException, IOException, FileFormatException {
		try {
			String code = jp.getText();
			Rule r = Rule.parseRule( code );
			if (!r.isVacuumStable()){
				if (r.isValidDayNight())
					return new DayNightRule( r );
				System.err.println("Warning: rule "+code+" is invalid. Loading anyways.");
			}
			return r;
		} catch (RuleSyntaxException e) {
			throw new FileFormatException("Incorrect rule format:"+e.getMessage());
		}
	}
	private static void assertToken(JsonToken t, JsonToken tt) throws FileFormatException {
		if ( t != tt ){
			throw new FileFormatException( "Bad JSON format: token "+t+" != "+tt);
		}
	}
}
