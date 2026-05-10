package product.rest.client.dto.v1_0;

import java.io.Serializable;

import java.util.Objects;

import javax.annotation.Generated;

import product.rest.client.function.UnsafeSupplier;
import product.rest.client.serdes.v1_0.ProductTagsSerDes;

/**
 * @author naves
 * @generated
 */
@Generated("")
public class ProductTags implements Cloneable, Serializable {

	public static ProductTags toDTO(String json) {
		return ProductTagsSerDes.toDTO(json);
	}

	public Long[] getTagIds() {
		return tagIds;
	}

	public void setTagIds(Long[] tagIds) {
		this.tagIds = tagIds;
	}

	public void setTagIds(
		UnsafeSupplier<Long[], Exception> tagIdsUnsafeSupplier) {

		try {
			tagIds = tagIdsUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Long[] tagIds;

	@Override
	public ProductTags clone() throws CloneNotSupportedException {
		return (ProductTags)super.clone();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof ProductTags)) {
			return false;
		}

		ProductTags productTags = (ProductTags)object;

		return Objects.equals(toString(), productTags.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		return ProductTagsSerDes.toJSON(this);
	}

}