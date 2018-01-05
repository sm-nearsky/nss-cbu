package com.nearskysolutions.cloudbackup.util;

import java.lang.reflect.Type;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

public class JsonConverter {

	private static Gson gsonParser = null;
	
	static {		 
    	GsonBuilder builder = new GsonBuilder(); 

    	// Register an adapter to manage the date types as long values 
    	builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {

			@Override
			public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
					throws JsonParseException {
				return new Date(json.getAsJsonPrimitive().getAsLong()); 
			}	     	    
    	});
    	
    	// Register an adapter to manage the date types as long values 
    	builder.registerTypeAdapter(Date.class, new com.google.gson.JsonSerializer<Date>() {

			@Override
			public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {				
				return new JsonPrimitive(src.getTime());
			}
				     	    
    	});
    	    	
    	gsonParser = builder.create();
	}
	
	public static String ConvertObjectToJson(Object obj) {
		return JsonConverter.gsonParser.toJson(obj);
	}
	
	public static Object ConvertJsonToObject(String jsonStr, Class<?> type) {
		return JsonConverter.gsonParser.fromJson(jsonStr, type);
	}
	
}
