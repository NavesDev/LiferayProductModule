package product.rest.client.serdes.v1_0;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Generated;

import product.rest.client.dto.v1_0.ProductTags;
import product.rest.client.json.BaseJSONParser;

/**
 * @author naves
 * @generated
 */
@Generated("")
public class ProductTagsSerDes {

	public static ProductTags toDTO(String json) {
		ProductTagsJSONParser productTagsJSONParser =
			new ProductTagsJSONParser();

		return productTagsJSONParser.parseToDTO(json);
	}

	public static ProductTags[] toDTOs(String json) {
		ProductTagsJSONParser productTagsJSONParser =
			new ProductTagsJSONParser();

		return productTagsJSONParser.parseToDTOs(json);
	}

	public static String toJSON(ProductTags productTags) {
		if (productTags == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();

		sb.append("{");

		if (productTags.getTagIds() != null) {
			if (sb.length() > 1) {
				sb.append(", ");
			}

			sb.append("\"tagIds\": ");

			sb.append("[");

			for (int i = 0; i < productTags.getTagIds().length; i++) {
				sb.append(productTags.getTagIds()[i]);

				if ((i + 1) < productTags.getTagIds().length) {
					sb.append(", ");
				}
			}

			sb.append("]");
		}

		sb.append("}");

		return sb.toString();
	}

	public static Map<String, Object> toMap(String json) {
		ProductTagsJSONParser productTagsJSONParser =
			new ProductTagsJSONParser();

		return productTagsJSONParser.parseToMap(json);
	}

	public static Map<String, String> toMap(ProductTags productTags) {
		if (productTags == null) {
			return null;
		}

		Map<String, String> map = new TreeMap<>();

		if (productTags.getTagIds() == null) {
			map.put("tagIds", null);
		}
		else {
			map.put("tagIds", String.valueOf(productTags.getTagIds()));
		}

		return map;
	}

	public static class ProductTagsJSONParser
		extends BaseJSONParser<ProductTags> {

		@Override
		protected ProductTags createDTO() {
			return new ProductTags();
		}

		@Override
		protected ProductTags[] createDTOArray(int size) {
			return new ProductTags[size];
		}

		@Override
		protected boolean parseMaps(String jsonParserFieldName) {
			if (Objects.equals(jsonParserFieldName, "tagIds")) {
				return false;
			}

			return false;
		}

		@Override
		protected void setField(
			ProductTags productTags, String jsonParserFieldName,
			Object jsonParserFieldValue) {

			if (Objects.equals(jsonParserFieldName, "tagIds")) {
				if (jsonParserFieldValue != null) {
					productTags.setTagIds(
						toLongs((Object[])jsonParserFieldValue));
				}
			}
		}

	}

	private static String _escape(Object object) {
		String string = String.valueOf(object);

		for (String[] strings : BaseJSONParser.JSON_ESCAPE_STRINGS) {
			string = string.replace(strings[0], strings[1]);
		}

		return string;
	}

	private static String _toJSON(Map<String, ?> map) {
		StringBuilder sb = new StringBuilder("{");

		@SuppressWarnings("unchecked")
		Set set = map.entrySet();

		@SuppressWarnings("unchecked")
		Iterator<Map.Entry<String, ?>> iterator = set.iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, ?> entry = iterator.next();

			sb.append("\"");
			sb.append(entry.getKey());
			sb.append("\": ");

			Object value = entry.getValue();

			sb.append(_toJSON(value));

			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}

		sb.append("}");

		return sb.toString();
	}

	private static String _toJSON(Object value) {
		if (value == null) {
			return "null";
		}

		if (value instanceof Map) {
			return _toJSON((Map)value);
		}

		Class<?> clazz = value.getClass();

		if (clazz.isArray()) {
			StringBuilder sb = new StringBuilder("[");

			Object[] values = (Object[])value;

			for (int i = 0; i < values.length; i++) {
				sb.append(_toJSON(values[i]));

				if ((i + 1) < values.length) {
					sb.append(", ");
				}
			}

			sb.append("]");

			return sb.toString();
		}

		if (value instanceof String) {
			return "\"" + _escape(value) + "\"";
		}

		return String.valueOf(value);
	}

}