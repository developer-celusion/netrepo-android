package com.mobiliteam.networkrepo.gsonadapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by swapnilnandgave on 18/04/18.
 */

public class CustomGsonTypeAdapter {

    public static Type getIntegerType() {
        Type type = new TypeToken<Integer>() {
        }.getType();
        return type;
    }

    public static Type getBooleanType() {
        Type type = new TypeToken<Boolean>() {
        }.getType();
        return type;
    }

    public static Type getDoubleType() {
        Type type = new TypeToken<Double>() {
        }.getType();
        return type;
    }

    public static final TypeAdapter<Integer> IntegerTypeAdapter = new TypeAdapter<Integer>() {
        @Override
        public void write(JsonWriter out, Integer value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.intValue());
            }
        }

        @Override
        public Integer read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            switch (peek) {
                case NULL:
                    in.nextNull();
                    return null;
                case NUMBER:
                    return new Integer(in.nextInt());
                case STRING:
                    return new Integer(in.nextInt());
                default:
                    throw new IllegalStateException("Expected STRING or NUMBER but was " + peek);
            }
        }
    };

    public static final TypeAdapter<Boolean> BooleanTypeAdapter = new TypeAdapter<Boolean>() {
        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.booleanValue());
            }
        }

        @Override
        public Boolean read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            switch (peek) {
                case NULL:
                    in.nextNull();
                    return null;
                case NUMBER:
                    return new Boolean(in.nextBoolean());
                case STRING:
                    return new Boolean(in.nextBoolean());
                case BOOLEAN:
                    return new Boolean(in.nextBoolean());
                default:
                    throw new IllegalStateException("Expected STRING or NUMBER but was " + peek);
            }
        }
    };

    public static final TypeAdapter<Double> DoubleTypeAdapter = new TypeAdapter<Double>() {
        @Override
        public void write(JsonWriter out, Double value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.doubleValue());
            }
        }

        @Override
        public Double read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            switch (peek) {
                case NULL:
                    in.nextNull();
                    return null;
                case NUMBER:
                    return new Double(in.nextDouble());
                case STRING:
                    return new Double(in.nextDouble());
                default:
                    throw new IllegalStateException("Expected STRING or NUMBER but was " + peek);
            }
        }
    };

    public static class PrimBooleanToIntAdapter implements JsonSerializer<Boolean>, JsonDeserializer<Boolean> {

        @Override
        public JsonElement serialize(Boolean arg0, Type type, JsonSerializationContext jsc) {
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            return new JsonPrimitive(Boolean.TRUE.equals(arg0) ? 1 : 0);
        }

        @Override
        public Boolean deserialize(JsonElement json, Type typeOfT,
                                   JsonDeserializationContext context) throws JsonParseException {
            boolean status = false;
            try {
                final int code = json.getAsInt();
                status = (code == 1);
            } catch (Exception e) {
            }
            try {
                status = json.getAsBoolean();
            } catch (Exception e) {
            }
            return status;
        }
    }

    public static class GsonUTCDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        private final DateFormat dateFormat;

        public GsonUTCDateAdapter(final String strFormat) {
            dateFormat = new SimpleDateFormat(strFormat, Locale.getDefault());      //This is the format I need
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));                   //This is the key line which converts the date to UTC which cannot be accessed with the default serializer
        }

        @Override
        public synchronized JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(dateFormat.format(date));
        }

        @Override
        public synchronized Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            try {
                return dateFormat.parse(jsonElement.getAsString());
            } catch (ParseException e) {
                throw new JsonParseException(e);
            }
        }

    }

}
