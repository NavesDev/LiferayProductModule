/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package product.service.service.impl;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.util.Arrays;
import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import product.service.exception.ProductCategoryException;
import product.service.exception.ProductAssetUpdateException;
import product.service.exception.ProductPersistenceException;
import product.service.exception.ProductStatusException;
import product.service.exception.ProductTagException;
import product.service.exception.ProductUserException;
import product.service.exception.ProductValidationException;
import product.service.model.Product;
import product.service.model.ProductStatusConstants;
import product.service.service.base.ProductLocalServiceBaseImpl;

/**
 * @author Brian Wing Shun Chan
 */
@Component(
	property = "model.class.name=product.service.model.Product",
	service = AopService.class
)
public class ProductLocalServiceImpl extends ProductLocalServiceBaseImpl {

	public Product addProduct(
			long userId, long groupId, String name, String description,
			double price, int status, int stockQuantity, long[] categoryIds,
			long[] tagIds, ServiceContext serviceContext)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"addProduct(userId=" + userId + ", groupId=" + groupId +
					", status=" + status + ", stockQuantity=" + stockQuantity +
					", categoryIds=" + Arrays.toString(categoryIds) +
					", tagIds=" + Arrays.toString(tagIds) + ")");
		}

		_validate(status, stockQuantity, name, description, price, categoryIds);

		long[] validatedCategoryIds = _validateCategories(groupId, categoryIds);
		String[] tagNames = _resolveTagNames(groupId, tagIds);

		serviceContext = _getServiceContext(serviceContext, userId, groupId);

		User user;

		try {
			user = userLocalService.getUser(userId);
		}
		catch (PortalException portalException) {
			throw new ProductUserException(
				"Unable to load user with userId " + userId,
				portalException);
		}

		long productId = counterLocalService.increment(Product.class.getName());

		Product product = productPersistence.create(productId);

		Date now = new Date();

		product.setUuid(PortalUUIDUtil.generate());
		product.setCompanyId(user.getCompanyId());
		product.setCreateDate(serviceContext.getCreateDate(now));
		product.setDescription(_normalize(description));
		product.setGroupId(groupId);
		product.setModifiedDate(serviceContext.getModifiedDate(now));
		product.setName(_normalize(name));
		product.setPrice(price);
		product.setStatus(status);
		product.setStockQuantity(stockQuantity);
		product.setUserId(userId);
		product.setUserName(user.getFullName());

		try {
			product = productPersistence.update(product);
		}
		catch (RuntimeException runtimeException) {
			_log.error(
				"Failed to persist product for userId=" + userId + ", groupId=" +
					groupId,
				runtimeException);

			throw new ProductPersistenceException(
				"Unable to persist product for userId " + userId +
					" and groupId " + groupId,
				runtimeException);
		}

		try {
			_updateAsset(product, validatedCategoryIds, tagNames, serviceContext);
		}
		catch (PortalException portalException) {
			_log.error(
				"Failed to update asset for productId=" + productId +
					", categoryIds=" + Arrays.toString(validatedCategoryIds) +
					", tagNames=" + Arrays.toString(tagNames),
				portalException);

			throw new ProductAssetUpdateException(
				"Unable to update asset for productId " + productId,
				portalException);
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"addProduct completed for productId=" + product.getProductId() +
					", status=" + product.getStatus());
		}

		return product;
	}

	@Override
	public Product deleteProduct(long productId) throws PortalException {
		if (_log.isDebugEnabled()) {
			_log.debug("deleteProduct(productId=" + productId + ")");
		}

		return deleteProduct(productPersistence.findByPrimaryKey(productId));
	}

	@Override
	public Product deleteProduct(Product product) {
		if (_log.isDebugEnabled()) {
			_log.debug(
				"deleteProduct(productId=" + product.getProductId() +
					", groupId=" + product.getGroupId() + ")");
		}

		_deleteAssetEntry(product.getProductId());

		Product removedProduct = productPersistence.remove(product);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"deleteProduct completed for productId=" +
					removedProduct.getProductId());
		}

		return removedProduct;
	}

	public Product updateProduct(
			long productId, String name, String description, double price,
			int status, int stockQuantity, long[] categoryIds, long[] tagIds,
			ServiceContext serviceContext)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProduct(productId=" + productId + ", status=" + status +
					", stockQuantity=" + stockQuantity + ", categoryIds=" +
					Arrays.toString(categoryIds) + ", tagIds=" +
					Arrays.toString(tagIds) + ")");
		}

		Product product = productPersistence.findByPrimaryKey(productId);

		_validateTransition(product.getStatus(), status);
		_validate(status, stockQuantity, name, description, price, categoryIds);

		serviceContext = _getServiceContext(
			serviceContext, product.getUserId(), product.getGroupId());

		product.setDescription(_normalize(description));
		product.setModifiedDate(serviceContext.getModifiedDate(new Date()));
		product.setName(_normalize(name));
		product.setPrice(price);
		product.setStatus(status);
		product.setStockQuantity(stockQuantity);

		product = productPersistence.update(product);

		_updateAsset(
			product, _validateCategories(product.getGroupId(), categoryIds),
			_resolveTagNames(product.getGroupId(), tagIds), serviceContext);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProduct completed for productId=" + product.getProductId() +
					", status=" + product.getStatus());
		}

		return product;
	}

	public Product updateProductCategories(
			long productId, long[] categoryIds, ServiceContext serviceContext)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProductCategories(productId=" + productId +
					", categoryIds=" + Arrays.toString(categoryIds) + ")");
		}

		Product product = productPersistence.findByPrimaryKey(productId);

		long[] validatedCategoryIds = _validateCategories(
			product.getGroupId(), categoryIds);

		serviceContext = _getServiceContext(
			serviceContext, product.getUserId(), product.getGroupId());

		if (product.getStatus() == ProductStatusConstants.PUBLISHED) {
			_validatePublishedProduct(
				product.getName(), product.getDescription(), product.getPrice(),
				validatedCategoryIds);
		}

		_updateAsset(
			product, validatedCategoryIds, _getAssetTagNames(productId),
			serviceContext);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProductCategories completed for productId=" + productId +
					", categoryCount=" + validatedCategoryIds.length);
		}

		return product;
	}

	public Product updateProductStatus(
			long productId, int status, ServiceContext serviceContext)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProductStatus(productId=" + productId + ", status=" +
					status + ")");
		}

		Product product = productPersistence.findByPrimaryKey(productId);

		_validateTransition(product.getStatus(), status);

		serviceContext = _getServiceContext(
			serviceContext, product.getUserId(), product.getGroupId());

		long[] categoryIds = _getAssetCategoryIds(productId);

		if (status == ProductStatusConstants.PUBLISHED) {
			_validatePublishedProduct(
				product.getName(), product.getDescription(), product.getPrice(),
				categoryIds);
		}

		product.setModifiedDate(serviceContext.getModifiedDate(new Date()));
		product.setStatus(status);

		product = productPersistence.update(product);

		_updateAsset(
			product, categoryIds, _getAssetTagNames(productId), serviceContext);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProductStatus completed for productId=" + productId +
					", newStatus=" + product.getStatus());
		}

		return product;
	}

	public Product updateProductTags(
			long productId, long[] tagIds, ServiceContext serviceContext)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProductTags(productId=" + productId + ", tagIds=" +
					Arrays.toString(tagIds) + ")");
		}

		Product product = productPersistence.findByPrimaryKey(productId);

		serviceContext = _getServiceContext(
			serviceContext, product.getUserId(), product.getGroupId());

		_updateAsset(
			product, _getAssetCategoryIds(productId),
			_resolveTagNames(product.getGroupId(), tagIds), serviceContext);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"updateProductTags completed for productId=" + productId);
		}

		return product;
	}

	public java.util.List<Product> getProductsByGroupId(long groupId) {
		if (_log.isDebugEnabled()) {
			_log.debug("getProductsByGroupId(groupId=" + groupId + ")");
		}

		return productPersistence.findByGroupId(groupId);
	}

	private void _deleteAssetEntry(long productId) {
		if (_log.isDebugEnabled()) {
			_log.debug("_deleteAssetEntry(productId=" + productId + ")");
		}

		try {
			assetEntryLocalService.deleteEntry(Product.class.getName(), productId);
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"_deleteAssetEntry failed for productId=" + productId,
					portalException);
			}
		}
	}

	private long[] _getAssetCategoryIds(long productId) {
		if (_log.isDebugEnabled()) {
			_log.debug("_getAssetCategoryIds(productId=" + productId + ")");
		}

		try {
			AssetEntry assetEntry = assetEntryLocalService.getEntry(
				Product.class.getName(), productId);

			long[] categoryIds = assetEntry.getCategoryIds();

			if (_log.isDebugEnabled()) {
				_log.debug(
					"_getAssetCategoryIds found " + categoryIds.length +
						" categories for productId=" + productId);
			}

			return categoryIds;
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"_getAssetCategoryIds returning empty array for productId=" +
						productId,
					portalException);
			}

			return new long[0];
		}
	}

	private String[] _getAssetTagNames(long productId) {
		if (_log.isDebugEnabled()) {
			_log.debug("_getAssetTagNames(productId=" + productId + ")");
		}

		try {
			AssetEntry assetEntry = assetEntryLocalService.getEntry(
				Product.class.getName(), productId);

			String[] tagNames = assetEntry.getTagNames();

			if (_log.isDebugEnabled()) {
				_log.debug(
					"_getAssetTagNames found " + tagNames.length +
						" tags for productId=" + productId);
			}

			return tagNames;
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"_getAssetTagNames returning empty array for productId=" +
						productId,
					portalException);
			}

			return new String[0];
		}
	}

	private ServiceContext _getServiceContext(
		ServiceContext serviceContext, long userId, long groupId) {

		if (serviceContext != null) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"_getServiceContext reusing provided ServiceContext for userId=" +
						userId + ", groupId=" + groupId);
			}

			return serviceContext;
		}

		ServiceContext newServiceContext = new ServiceContext();

		newServiceContext.setScopeGroupId(groupId);
		newServiceContext.setUserId(userId);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_getServiceContext created new ServiceContext for userId=" +
					userId + ", groupId=" + groupId);
		}

		return newServiceContext;
	}

	private String _normalize(String value) {
		if (value == null) {
			if (_log.isDebugEnabled()) {
				_log.debug("_normalize received null value");
			}

			return null;
		}

		String normalizedValue = value.trim();

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_normalize trimmed value from length=" + value.length() +
					" to length=" + normalizedValue.length());
		}

		return normalizedValue;
	}

	private String[] _resolveTagNames(long groupId, long[] tagIds)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_resolveTagNames(groupId=" + groupId + ", tagIds=" +
					Arrays.toString(tagIds) + ")");
		}

		if ((tagIds == null) || (tagIds.length == 0)) {
			return new String[0];
		}

		String[] tagNames = new String[tagIds.length];

		for (int i = 0; i < tagIds.length; i++) {
			AssetTag assetTag = assetTagLocalService.fetchAssetTag(tagIds[i]);

			if (assetTag == null) {
				throw new ProductTagException("Tag not found: " + tagIds[i]);
			}

			if (assetTag.getGroupId() != groupId) {
				throw new ProductTagException(
					"Tag does not belong to the expected group: " + tagIds[i]);
			}

			tagNames[i] = assetTag.getName();
		}

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_resolveTagNames resolved " + tagNames.length +
					" tag names for groupId=" + groupId);
		}

		return tagNames;
	}

	private void _updateAsset(
			Product product, long[] categoryIds, String[] tagNames,
			ServiceContext serviceContext)
		throws PortalException {

		boolean visible = product.getStatus() == ProductStatusConstants.PUBLISHED;

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_updateAsset(productId=" + product.getProductId() +
					", categoryIds=" + Arrays.toString(categoryIds) +
					", tagNames=" + Arrays.toString(tagNames) + ", visible=" +
					visible + ")");
		}

		assetEntryLocalService.updateEntry(
			product.getUserId(), product.getGroupId(), product.getCreateDate(),
			product.getModifiedDate(), Product.class.getName(),
			product.getProductId(), product.getUuid(), 0L, categoryIds, tagNames,
			visible, visible, null, null, null, null, null, product.getName(),
			product.getDescription(), null, null, null, 0, 0, product.getPrice(),
			serviceContext);
	}

	private long[] _validateCategories(long groupId, long[] categoryIds)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_validateCategories(groupId=" + groupId + ", categoryIds=" +
					Arrays.toString(categoryIds) + ")");
		}

		if (categoryIds == null) {
			return new long[0];
		}

		for (long categoryId : categoryIds) {
			AssetCategory assetCategory =
				assetCategoryLocalService.fetchAssetCategory(categoryId);

			if (assetCategory == null) {
				throw new ProductCategoryException(
					"Category not found: " + categoryId);
			}

			if (assetCategory.getGroupId() != groupId) {
				throw new ProductCategoryException(
					"Category does not belong to the expected group: " +
						categoryId);
			}
		}

		long[] validatedCategoryIds = Arrays.copyOf(
			categoryIds, categoryIds.length);

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_validateCategories validated " +
					validatedCategoryIds.length + " categories");
		}

		return validatedCategoryIds;
	}

	private void _validate(
			int status, int stockQuantity, String name, String description,
			double price, long[] categoryIds)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_validate(status=" + status + ", stockQuantity=" +
					stockQuantity + ", price=" + price + ", categoryIds=" +
					Arrays.toString(categoryIds) + ")");
		}

		if (!ProductStatusConstants.isValid(status)) {
			throw new ProductStatusException("Invalid product status: " + status);
		}

		if (stockQuantity < 0) {
			throw new ProductValidationException(
				"Stock quantity cannot be negative");
		}

		if (status == ProductStatusConstants.PUBLISHED) {
			_validatePublishedProduct(name, description, price, categoryIds);
		}

		if (_log.isDebugEnabled()) {
			_log.debug("_validate passed for status=" + status);
		}
	}

	private void _validatePublishedProduct(
			String name, String description, double price, long[] categoryIds)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_validatePublishedProduct(nameBlank=" + Validator.isBlank(name) +
					", descriptionBlank=" + Validator.isBlank(description) +
					", price=" + price + ", categoryCount=" +
					((categoryIds == null) ? 0 : categoryIds.length) + ")");
		}

		if (Validator.isBlank(name)) {
			throw new ProductValidationException(
				"Name is required for publication");
		}

		if (Validator.isBlank(description)) {
			throw new ProductValidationException(
				"Description is required for publication");
		}

		if (price < 0) {
			throw new ProductValidationException(
				"Price must be greater than or equal to zero");
		}

		if ((categoryIds == null) || (categoryIds.length == 0)) {
			throw new ProductValidationException(
				"At least one category is required for publication");
		}

		if (_log.isDebugEnabled()) {
			_log.debug("_validatePublishedProduct passed");
		}
	}

	private void _validateTransition(int currentStatus, int newStatus)
		throws PortalException {

		if (_log.isDebugEnabled()) {
			_log.debug(
				"_validateTransition(currentStatus=" + currentStatus +
					", newStatus=" + newStatus + ")");
		}

		if (currentStatus == newStatus) {
			if (_log.isDebugEnabled()) {
				_log.debug(
					"_validateTransition ignored because status is unchanged");
			}

			return;
		}

		if ((currentStatus == ProductStatusConstants.DRAFT) &&
			((newStatus == ProductStatusConstants.PUBLISHED) ||
			 (newStatus == ProductStatusConstants.INACTIVE))) {

			if (_log.isDebugEnabled()) {
				_log.debug("_validateTransition accepted from DRAFT");
			}

			return;
		}

		if ((currentStatus == ProductStatusConstants.PUBLISHED) &&
			(newStatus == ProductStatusConstants.INACTIVE)) {

			if (_log.isDebugEnabled()) {
				_log.debug("_validateTransition accepted from PUBLISHED");
			}

			return;
		}

		if ((currentStatus == ProductStatusConstants.INACTIVE) &&
			((newStatus == ProductStatusConstants.DRAFT) ||
			 (newStatus == ProductStatusConstants.PUBLISHED))) {

			if (_log.isDebugEnabled()) {
				_log.debug("_validateTransition accepted from INACTIVE");
			}

			return;
		}

		throw new ProductStatusException(
			"Invalid status transition from " + currentStatus + " to " +
				newStatus);
	}

	@Reference
	private AssetCategoryLocalService assetCategoryLocalService;

	private static final Log _log = LogFactoryUtil.getLog(
		ProductLocalServiceImpl.class);

}
