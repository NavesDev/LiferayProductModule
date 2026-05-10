package product.rest.internal.resource.v1_0;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import product.rest.dto.v1_0.Product;
import product.rest.dto.v1_0.ProductCategories;
import product.rest.dto.v1_0.ProductTags;
import product.rest.resource.v1_0.ProductResource;
import product.service.model.ProductStatusConstants;
import product.service.service.ProductLocalService;

/**
 * @author naves
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/product.properties",
	scope = ServiceScope.PROTOTYPE, service = ProductResource.class
)
public class ProductResourceImpl extends BaseProductResourceImpl {

	@Override
	public Response deleteSiteProduct(Long siteId, Long productId)
		throws Exception {

		_validateProductSite(siteId, productId);

		_productLocalService.deleteProduct(productId);

		return Response.noContent().build();
	}

	@Override
	public Product getSiteProduct(Long siteId, Long productId) throws Exception {
		product.service.model.Product serviceProduct = _validateProductSite(
			siteId, productId);

		return _toDTO(serviceProduct, siteId);
	}

	@Override
	public Page<Product> getSiteProductsPage(
			Long siteId,
			String search, String status, Long categoryId, Long tagId,
			Boolean inStock, Pagination pagination,
			com.liferay.portal.kernel.search.Sort[] sorts)
		throws Exception {

		List<product.service.model.Product> products = _productLocalService.getProducts(
			0, Integer.MAX_VALUE);

		List<Product> filtered = products.stream(
		).map(
			product -> _toDTO(product, siteId)
		).filter(
			product -> _matchesFilters(product, search, status, categoryId, tagId, inStock)
		).collect(Collectors.toCollection(ArrayList::new));

		_applySort(filtered, sorts);

		if (pagination == null) {
			return Page.of(filtered);
		}

		int start = pagination.getStartPosition();
		int end = Math.min(pagination.getEndPosition(), filtered.size());

		if (start >= filtered.size()) {
			return Page.of(new ArrayList<>());
		}

		return Page.of(filtered.subList(start, end));
	}

	@Override
	public Product postSiteProduct(Long siteId, Product product) throws Exception {
		_validatePayload(product);

		product.service.model.Product createdProduct = _productLocalService.addProduct(
			contextUser.getUserId(), siteId, product.getName(),
			product.getDescription(), _defaultDouble(product.getPrice()),
			_toStatus(product.getStatus()), _defaultInteger(product.getStockQuantity()),
			_toLongArray(product.getCategoryIds()), _toLongArray(product.getTagIds()),
			_createServiceContext(siteId));

		return _toDTO(createdProduct, siteId);
	}

	@Override
	public Product putSiteProduct(Long siteId, Long productId, Product product)
		throws Exception {

		_validatePayload(product);
		_validateProductSite(siteId, productId);

		product.service.model.Product updatedProduct = _productLocalService.updateProduct(
			productId, product.getName(), product.getDescription(),
			_defaultDouble(product.getPrice()), _toStatus(product.getStatus()),
			_defaultInteger(product.getStockQuantity()),
			_toLongArray(product.getCategoryIds()), _toLongArray(product.getTagIds()),
			_createServiceContext(siteId));

		return _toDTO(updatedProduct, siteId);
	}

	@Override
	public Product putSiteProductCategories(
			Long siteId, Long productId, ProductCategories productCategories)
		throws Exception {

		if (productCategories == null) {
			throw new BadRequestException("Product categories payload is required");
		}
		_validateProductSite(siteId, productId);

		product.service.model.Product updatedProduct =
			_productLocalService.updateProductCategories(
				productId, _toLongArray(productCategories.getCategoryIds()),
				_createServiceContext(siteId));

		return _toDTO(updatedProduct, siteId);
	}

	@Override
	public Product putSiteProductTags(
			Long siteId, Long productId, ProductTags productTags)
		throws Exception {

		if (productTags == null) {
			throw new BadRequestException("Product tags payload is required");
		}
		_validateProductSite(siteId, productId);

		product.service.model.Product updatedProduct =
			_productLocalService.updateProductTags(
				productId, _toLongArray(productTags.getTagIds()),
				_createServiceContext(siteId));

		return _toDTO(updatedProduct, siteId);
	}

	private void _applySort(
		List<Product> products, com.liferay.portal.kernel.search.Sort[] sorts) {

		if (ArrayUtil.isEmpty(sorts)) {
			return;
		}

		com.liferay.portal.kernel.search.Sort sort = sorts[0];
		Comparator<Product> comparator = null;
		String fieldName = sort.getFieldName();

		if (Objects.equals(fieldName, "name")) {
			comparator = Comparator.comparing(
				product -> _lower(product.getName()), Comparator.nullsLast(String::compareTo));
		}
		else if (Objects.equals(fieldName, "price")) {
			comparator = Comparator.comparing(
				Product::getPrice, Comparator.nullsLast(Double::compareTo));
		}
		else if (Objects.equals(fieldName, "id")) {
			comparator = Comparator.comparing(
				Product::getId, Comparator.nullsLast(Long::compareTo));
		}

		if (comparator == null) {
			return;
		}

		if (sort.isReverse()) {
			comparator = comparator.reversed();
		}

		products.sort(comparator);
	}

	private ServiceContext _createServiceContext(long siteId) {
		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(contextCompany.getCompanyId());
		serviceContext.setScopeGroupId(siteId);
		serviceContext.setUserId(contextUser.getUserId());

		return serviceContext;
	}

	private int _defaultInteger(Integer value) {
		if (value == null) {
			return 0;
		}

		return value;
	}

	private double _defaultDouble(Double value) {
		if (value == null) {
			return 0D;
		}

		return value;
	}

	private String _lower(String value) {
		if (value == null) {
			return null;
		}

		return value.toLowerCase(Locale.ROOT);
	}

	private boolean _matchesFilters(
		Product product, String search, String status, Long categoryId, Long tagId,
		Boolean inStock) {

		if (!_matchesSearch(product, search)) {
			return false;
		}

		if ((status != null) && !_matchesStatus(product, status)) {
			return false;
		}

		if ((categoryId != null) && !_contains(product.getCategoryIds(), categoryId)) {
			return false;
		}

		if ((tagId != null) && !_contains(product.getTagIds(), tagId)) {
			return false;
		}

		boolean productInStock =
			(product.getStockQuantity() != null) &&
			(product.getStockQuantity() > 0);

		if ((inStock != null) && (inStock != productInStock)) {
			return false;
		}

		return true;
	}

	private boolean _matchesSearch(Product product, String search) {
		if (search == null) {
			return true;
		}

		String normalizedSearch = _lower(search);

		return StringUtil.containsIgnoreCase(product.getName(), normalizedSearch) ||
			StringUtil.containsIgnoreCase(product.getDescription(), normalizedSearch);
	}

	private boolean _matchesStatus(Product product, String status) {
		if (product.getStatus() == null) {
			return false;
		}

		return Objects.equals(product.getStatus().getValue(), status);
	}

	private Product _toDTO(product.service.model.Product product, long siteId) {
		Product dtoProduct = new Product();

		dtoProduct.setDescription(product.getDescription());
		dtoProduct.setId(product.getProductId());
		dtoProduct.setName(product.getName());
		dtoProduct.setPrice(product.getPrice());
		dtoProduct.setStatus(_toStatus(product.getStatus()));
		dtoProduct.setStockQuantity(product.getStockQuantity());

		try {
			AssetEntry assetEntry = _assetEntryLocalService.getEntry(
				product.service.model.Product.class.getName(), product.getProductId());

			if (assetEntry == null) {
				dtoProduct.setCategoryIds(new Long[0]);
				dtoProduct.setTagIds(new Long[0]);
			}
			else {
				dtoProduct.setCategoryIds(
					_toLongObjectArray(assetEntry.getCategoryIds()));
				dtoProduct.setTagIds(_toTagIds(siteId, assetEntry.getTagNames()));
			}
		}
		catch (PortalException portalException) {
			dtoProduct.setCategoryIds(new Long[0]);
			dtoProduct.setTagIds(new Long[0]);
		}

		return dtoProduct;
	}

	private Product.Status _toStatus(int status) {
		if (status == ProductStatusConstants.PUBLISHED) {
			return Product.Status.PUBLISHED;
		}
		else if (status == ProductStatusConstants.INACTIVE) {
			return Product.Status.INACTIVE;
		}

		return Product.Status.DRAFT;
	}

	private int _toStatus(Product.Status status) {
		if (status == null) {
			return ProductStatusConstants.DRAFT;
		}

		if (status == Product.Status.PUBLISHED) {
			return ProductStatusConstants.PUBLISHED;
		}
		else if (status == Product.Status.INACTIVE) {
			return ProductStatusConstants.INACTIVE;
		}

		return ProductStatusConstants.DRAFT;
	}

	private boolean _contains(Long[] values, Long value) {
		if ((values == null) || (value == null)) {
			return false;
		}

		for (Long currentValue : values) {
			if (Objects.equals(currentValue, value)) {
				return true;
			}
		}

		return false;
	}

	private long[] _toLongArray(Long[] values) {
		if (values == null) {
			return new long[0];
		}

		long[] array = new long[values.length];

		for (int i = 0; i < values.length; i++) {
			array[i] = values[i];
		}

		return array;
	}

	private Long[] _toLongObjectArray(long[] values) {
		Long[] array = new Long[values.length];

		for (int i = 0; i < values.length; i++) {
			array[i] = values[i];
		}

		return array;
	}

	private Long[] _toTagIds(long siteId, String[] tagNames) {
		if (ArrayUtil.isEmpty(tagNames)) {
			return new Long[0];
		}

		List<Long> tagIds = new ArrayList<>();

		for (String tagName : tagNames) {
			AssetTag assetTag = _assetTagLocalService.fetchTag(siteId, tagName);

			if (assetTag != null) {
				tagIds.add(assetTag.getTagId());
			}
		}

		return tagIds.toArray(new Long[0]);
	}

	private void _validatePayload(Product product) {
		if (product == null) {
			throw new BadRequestException("Product payload is required");
		}
	}

	private product.service.model.Product _validateProductSite(
			Long siteId, Long productId)
		throws Exception {

		product.service.model.Product product = _productLocalService.getProduct(
			productId);

		if (!Objects.equals(product.getGroupId(), siteId)) {
			throw new BadRequestException(
				"Product does not belong to the provided siteId");
		}

		return product;
	}

	@Reference
	private AssetEntryLocalService _assetEntryLocalService;

	@Reference
	private AssetTagLocalService _assetTagLocalService;

	@Reference
	private ProductLocalService _productLocalService;

}
