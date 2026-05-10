package product.rest.internal.graphql.mutation.v1_0;

import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.petra.function.UnsafeFunction;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.vulcan.accept.language.AcceptLanguage;
import com.liferay.portal.vulcan.batch.engine.resource.VulcanBatchEngineImportTaskResource;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;

import java.util.function.BiFunction;

import javax.annotation.Generated;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.validation.constraints.NotEmpty;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.ComponentServiceObjects;

import product.rest.dto.v1_0.Product;
import product.rest.dto.v1_0.ProductCategories;
import product.rest.dto.v1_0.ProductTags;
import product.rest.resource.v1_0.ProductResource;

/**
 * @author naves
 * @generated
 */
@Generated("")
public class Mutation {

	public static void setProductResourceComponentServiceObjects(
		ComponentServiceObjects<ProductResource>
			productResourceComponentServiceObjects) {

		_productResourceComponentServiceObjects =
			productResourceComponentServiceObjects;
	}

	@GraphQLField
	public Response deleteSiteProduct(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("productId") Long productId)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.deleteSiteProduct(
				Long.valueOf(siteKey), productId));
	}

	@GraphQLField
	public Product createSiteProduct(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("product") Product product)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.postSiteProduct(
				Long.valueOf(siteKey), product));
	}

	@GraphQLField
	public Response createSiteProductBatch(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("callbackURL") String callbackURL,
			@GraphQLName("object") Object object)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.postSiteProductBatch(
				Long.valueOf(siteKey), callbackURL, object));
	}

	@GraphQLField
	public Product updateSiteProduct(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("productId") Long productId,
			@GraphQLName("product") Product product)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.putSiteProduct(
				Long.valueOf(siteKey), productId, product));
	}

	@GraphQLField
	public Product updateSiteProductCategories(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("productId") Long productId,
			@GraphQLName("productCategories") ProductCategories
				productCategories)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.putSiteProductCategories(
				Long.valueOf(siteKey), productId, productCategories));
	}

	@GraphQLField
	public Product updateSiteProductTags(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("productId") Long productId,
			@GraphQLName("productTags") ProductTags productTags)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.putSiteProductTags(
				Long.valueOf(siteKey), productId, productTags));
	}

	private <T, R, E1 extends Throwable, E2 extends Throwable> R
			_applyComponentServiceObjects(
				ComponentServiceObjects<T> componentServiceObjects,
				UnsafeConsumer<T, E1> unsafeConsumer,
				UnsafeFunction<T, R, E2> unsafeFunction)
		throws E1, E2 {

		T resource = componentServiceObjects.getService();

		try {
			unsafeConsumer.accept(resource);

			return unsafeFunction.apply(resource);
		}
		finally {
			componentServiceObjects.ungetService(resource);
		}
	}

	private <T, E1 extends Throwable, E2 extends Throwable> void
			_applyVoidComponentServiceObjects(
				ComponentServiceObjects<T> componentServiceObjects,
				UnsafeConsumer<T, E1> unsafeConsumer,
				UnsafeConsumer<T, E2> unsafeFunction)
		throws E1, E2 {

		T resource = componentServiceObjects.getService();

		try {
			unsafeConsumer.accept(resource);

			unsafeFunction.accept(resource);
		}
		finally {
			componentServiceObjects.ungetService(resource);
		}
	}

	private void _populateResourceContext(ProductResource productResource)
		throws Exception {

		productResource.setContextAcceptLanguage(_acceptLanguage);
		productResource.setContextCompany(_company);
		productResource.setContextHttpServletRequest(_httpServletRequest);
		productResource.setContextHttpServletResponse(_httpServletResponse);
		productResource.setContextUriInfo(_uriInfo);
		productResource.setContextUser(_user);
		productResource.setGroupLocalService(_groupLocalService);
		productResource.setRoleLocalService(_roleLocalService);

		productResource.setVulcanBatchEngineImportTaskResource(
			_vulcanBatchEngineImportTaskResource);
	}

	private static ComponentServiceObjects<ProductResource>
		_productResourceComponentServiceObjects;

	private AcceptLanguage _acceptLanguage;
	private com.liferay.portal.kernel.model.Company _company;
	private GroupLocalService _groupLocalService;
	private HttpServletRequest _httpServletRequest;
	private HttpServletResponse _httpServletResponse;
	private RoleLocalService _roleLocalService;
	private BiFunction<Object, String, com.liferay.portal.kernel.search.Sort[]>
		_sortsBiFunction;
	private UriInfo _uriInfo;
	private com.liferay.portal.kernel.model.User _user;
	private VulcanBatchEngineImportTaskResource
		_vulcanBatchEngineImportTaskResource;

}