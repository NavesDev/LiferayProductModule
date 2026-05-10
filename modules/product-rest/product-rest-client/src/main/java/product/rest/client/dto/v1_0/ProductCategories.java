package product.rest.client.dto.v1_0;

import java.io.Serializable;

import java.util.Objects;

import javax.annotation.Generated;

import product.rest.client.function.UnsafeSupplier;
import product.rest.client.serdes.v1_0.ProductCategoriesSerDes;

/**
 * @author naves
 * @generated
 */
@Generated("")
public class ProductCategories implements Cloneable, Serializable {

	public static ProductCategories toDTO(String json) {
		return ProductCategoriesSerDes.toDTO(json);
	}

	public Long[] getCategoryIds() {
		return categoryIds;
	}

	public void setCategoryIds(Long[] categoryIds) {
		this.categoryIds = categoryIds;
	}

	public void setCategoryIds(
		UnsafeSupplier<Long[], Exception> categoryIdsUnsafeSupplier) {

		try {
			categoryIds = categoryIdsUnsafeSupplier.get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected Long[] categoryIds;

	@Override
	public ProductCategories clone() throws CloneNotSupportedException {
		return (ProductCategories)super.clone();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof ProductCategories)) {
			return false;
		}

		ProductCategories productCategories = (ProductCategories)object;

		return Objects.equals(toString(), productCategories.toString());
	}

	@Override
	public int hashCode() {
		String string = toString();

		return string.hashCode();
	}

	public String toString() {
		return ProductCategoriesSerDes.toJSON(this);
	}

}