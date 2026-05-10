package product.rest.internal.graphql.query.v1_0;

import com.liferay.petra.function.UnsafeConsumer;
import com.liferay.petra.function.UnsafeFunction;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ResourceActionLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.vulcan.accept.language.AcceptLanguage;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLField;
import com.liferay.portal.vulcan.graphql.annotation.GraphQLName;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.Map;
import java.util.function.BiFunction;

import javax.annotation.Generated;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.validation.constraints.NotEmpty;

import javax.ws.rs.core.UriInfo;

import org.osgi.service.component.ComponentServiceObjects;

import product.rest.dto.v1_0.Product;
import product.rest.resource.v1_0.ProductResource;

/**
 * @author naves
 * @generated
 */
@Generated("")
public class Query {

	public static void setProductResourceComponentServiceObjects(
		ComponentServiceObjects<ProductResource>
			productResourceComponentServiceObjects) {

		_productResourceComponentServiceObjects =
			productResourceComponentServiceObjects;
	}

	/**
	 * Invoke this method with the command line:
	 *
	 * curl -H 'Content-Type: text/plain; charset=utf-8' -X 'POST' 'http://localhost:8080/o/graphql' -d $'{"query": "query {product(productId: ___, siteKey: ___){id, name, description, price, status, stockQuantity, categoryIds, tagIds}}"}' -u 'test@liferay.com:test'
	 */
	@GraphQLField
	public Product product(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("productId") Long productId)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> productResource.getSiteProduct(
				Long.valueOf(siteKey), productId));
	}

	/**
	 * Invoke this method with the command line:
	 *
	 * curl -H 'Content-Type: text/plain; charset=utf-8' -X 'POST' 'http://localhost:8080/o/graphql' -d $'{"query": "query {products(categoryId: ___, inStock: ___, page: ___, pageSize: ___, search: ___, siteKey: ___, sorts: ___, status: ___, tagId: ___){items {__}, page, pageSize, totalCount}}"}' -u 'test@liferay.com:test'
	 */
	@GraphQLField
	public ProductPage products(
			@GraphQLName("siteKey") @NotEmpty String siteKey,
			@GraphQLName("search") String search,
			@GraphQLName("status") String status,
			@GraphQLName("categoryId") Long categoryId,
			@GraphQLName("tagId") Long tagId,
			@GraphQLName("inStock") Boolean inStock,
			@GraphQLName("pageSize") int pageSize,
			@GraphQLName("page") int page,
			@GraphQLName("sort") String sortsString)
		throws Exception {

		return _applyComponentServiceObjects(
			_productResourceComponentServiceObjects,
			this::_populateResourceContext,
			productResource -> new ProductPage(
				productResource.getSiteProductsPage(
					Long.valueOf(siteKey), search, status, categoryId, tagId,
					inStock, Pagination.of(page, pageSize),
					_sortsBiFunction.apply(productResource, sortsString))));
	}

	@GraphQLName("ProductPage")
	public class ProductPage {

		public ProductPage(Page productPage) {
			actions = productPage.getActions();

			items = productPage.getItems();
			lastPage = productPage.getLastPage();
			page = productPage.getPage();
			pageSize = productPage.getPageSize();
			totalCount = productPage.getTotalCount();
		}

		@GraphQLField
		protected Map<String, Map<String, String>> actions;

		@GraphQLField
		protected java.util.Collection<Product> items;

		@GraphQLField
		protected long lastPage;

		@GraphQLField
		protected long page;

		@GraphQLField
		protected long pageSize;

		@GraphQLField
		protected long totalCount;

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

	private void _populateResourceContext(ProductResource productResource)
		throws Exception {

		productResource.setContextAcceptLanguage(_acceptLanguage);
		productResource.setContextCompany(_company);
		productResource.setContextHttpServletRequest(_httpServletRequest);
		productResource.setContextHttpServletResponse(_httpServletResponse);
		productResource.setContextUriInfo(_uriInfo);
		productResource.setContextUser(_user);
		productResource.setGroupLocalService(_groupLocalService);
		productResource.setResourceActionLocalService(
			_resourceActionLocalService);
		productResource.setResourcePermissionLocalService(
			_resourcePermissionLocalService);
		productResource.setRoleLocalService(_roleLocalService);
	}

	private static ComponentServiceObjects<ProductResource>
		_productResourceComponentServiceObjects;

	private AcceptLanguage _acceptLanguage;
	private com.liferay.portal.kernel.model.Company _company;
	private BiFunction
		<Object, String, com.liferay.portal.kernel.search.filter.Filter>
			_filterBiFunction;
	private GroupLocalService _groupLocalService;
	private HttpServletRequest _httpServletRequest;
	private HttpServletResponse _httpServletResponse;
	private ResourceActionLocalService _resourceActionLocalService;
	private ResourcePermissionLocalService _resourcePermissionLocalService;
	private RoleLocalService _roleLocalService;
	private BiFunction<Object, String, com.liferay.portal.kernel.search.Sort[]>
		_sortsBiFunction;
	private UriInfo _uriInfo;
	private com.liferay.portal.kernel.model.User _user;

}