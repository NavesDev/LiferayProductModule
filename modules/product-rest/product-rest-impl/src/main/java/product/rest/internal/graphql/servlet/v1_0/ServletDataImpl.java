package product.rest.internal.graphql.servlet.v1_0;

import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.vulcan.graphql.servlet.ServletData;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Generated;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;

import product.rest.internal.graphql.mutation.v1_0.Mutation;
import product.rest.internal.graphql.query.v1_0.Query;
import product.rest.internal.resource.v1_0.ProductResourceImpl;
import product.rest.resource.v1_0.ProductResource;

/**
 * @author naves
 * @generated
 */
@Component(service = ServletData.class)
@Generated("")
public class ServletDataImpl implements ServletData {

	@Activate
	public void activate(BundleContext bundleContext) {
		Mutation.setProductResourceComponentServiceObjects(
			_productResourceComponentServiceObjects);

		Query.setProductResourceComponentServiceObjects(
			_productResourceComponentServiceObjects);
	}

	public String getApplicationName() {
		return "ProductRest";
	}

	@Override
	public Mutation getMutation() {
		return new Mutation();
	}

	@Override
	public String getPath() {
		return "/product-rest-graphql/v1_0";
	}

	@Override
	public Query getQuery() {
		return new Query();
	}

	public ObjectValuePair<Class<?>, String> getResourceMethodObjectValuePair(
		String methodName, boolean mutation) {

		if (mutation) {
			return _resourceMethodObjectValuePairs.get(
				"mutation#" + methodName);
		}

		return _resourceMethodObjectValuePairs.get("query#" + methodName);
	}

	private static final Map<String, ObjectValuePair<Class<?>, String>>
		_resourceMethodObjectValuePairs =
			new HashMap<String, ObjectValuePair<Class<?>, String>>() {
				{
					put(
						"mutation#deleteProduct",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "deleteProduct"));
					put(
						"mutation#deleteProductBatch",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "deleteProductBatch"));
					put(
						"mutation#createProduct",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "postProduct"));
					put(
						"mutation#createProductBatch",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "postProductBatch"));
					put(
						"mutation#updateProduct",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "putProduct"));
					put(
						"mutation#updateProductBatch",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "putProductBatch"));
					put(
						"mutation#updateProductCategories",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "putProductCategories"));
					put(
						"mutation#updateProductTags",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "putProductTags"));

					put(
						"query#product",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "getProduct"));
					put(
						"query#products",
						new ObjectValuePair<>(
							ProductResourceImpl.class, "getProductsPage"));
				}
			};

	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private ComponentServiceObjects<ProductResource>
		_productResourceComponentServiceObjects;

}