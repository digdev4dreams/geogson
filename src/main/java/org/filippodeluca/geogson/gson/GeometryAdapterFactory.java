package org.filippodeluca.geogson.gson;

import java.io.IOException;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.filippodeluca.geogson.model.Geometry;
import org.filippodeluca.geogson.model.Point;
import org.filippodeluca.geogson.model.Positions;
import org.filippodeluca.geogson.model.SinglePosition;

/**
 * @author Filippo De Luca - me@filippodeluca.com
 */
public class GeometryAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if(type.getRawType().isAssignableFrom(Geometry.class)) {
            return (TypeAdapter<T>) new GeometryAdapter(gson);
        } else {
            return gson.getAdapter(type);
        }
    }

    private static class GeometryAdapter extends TypeAdapter<Geometry> {

        private final Gson gson;

        private GeometryAdapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, Geometry value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.beginObject();

                out.name("type").value(value.getType().getValue());
                if(value.getType() != Geometry.Type.GEOMETRY_COLLECTION) {
                    out.name("coordinates");
                    gson.getAdapter(Positions.class).write(out, value.getPositions());
                } else {
                    // TODO
                }

                out.endObject();
            }
        }

        @Override
        public Geometry read(JsonReader in) throws IOException {

            Geometry geometry = null;
            if(in.peek() == JsonToken.NULL) {
                in.nextNull();
            } else if(in.peek() == JsonToken.BEGIN_OBJECT) {
                in.beginObject();

                String type = null;
                Positions positions = null;
                Iterable<Geometry> geometries = null;

                while (in.hasNext()) {
                    switch (in.nextName()) {
                        case "type":
                            type = in.nextString();
                            break;
                        case "coordinates":
                            positions = readPosition(in);
                            break;
                        case "geometries":
                            // TODO
                            break;
                        default:
                            // Ignore
                    }
                }

                geometry = buildGeometry(type, positions, geometries);

                in.endObject();
            } else {
                throw new IllegalArgumentException("The give json is not a valid Geometry: " + in.peek());
            }

            return geometry;
        }

        private Positions readPosition(JsonReader in) throws IOException {
            return gson.getAdapter(Positions.class).read(in);
        }

        private Geometry buildGeometry(final String type, Positions positions, Iterable<Geometry> geometries) {
            return buildPoint(type, positions).or(new Supplier<Geometry>() {
                @Override
                public Geometry get() {
                    throw new IllegalArgumentException("Cannot build a geometry for type: " + type);
                }
            });
        }

        private Optional<Geometry> buildPoint(String type, Positions coordinates)  {

            Optional<Geometry> mayGeometry = Optional.absent();

            if (type.equals(Geometry.Type.POINT.getValue())) {
                mayGeometry = Optional.<Geometry>of(Point.of(((SinglePosition)coordinates).getPosition()));
            }

            return mayGeometry;
        }

    }
}
