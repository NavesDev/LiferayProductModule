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
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.uuid.PortalUUIDUtil;

import java.util.Arrays;
import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import product.service.exception.ProductCategoryException;
import product.service.exception.ProductStatusException;
import product.service.exception.ProductTagException;
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

		_validate(status, stockQuantity, name, description, price, categoryIds);

		long[] validatedCategoryIds = _validateCategories(groupId, categoryIds);
		String[] tagNames = _resolveTagNames(groupId, tagIds);

		serviceContext = _getServiceContext(serviceContext, userId, groupId);

		User user = userLocalService.getUser(userId);

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

		product = productPersistence.update(product);

		resourceLocalService.addResources(
			product.getCompanyId(), groupId, userId, Product.class.getName(),
			productId, false, true, true);

		_updateAsset(product, validatedCategoryIds, tagNames, serviceContext);

		return product;
	}

	@Override
	public Product deleteProduct(long productId) throws PortalException {
		return deleteProduct(productPersistence.findByPrimaryKey(productId));
	}

	@Override
	public Product deleteProduct(Product product) {
		_deleteAssetEntry(product.getProductId());

		try {
			resourceLocalService.deleteResource(
				product.getCompanyId(), Product.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL, product.getProductId());
		}
		catch (PortalException portalException) {
			throw new SystemException(portalException);
		}

		return productPersistence.remove(product);
	}

	public Product updateProduct(
			long productId, String name, String description, double price,
			int status, int stockQuantity, long[] categoryIds, long[] tagIds,
			ServiceContext serviceContext)
		throws PortalException {

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

		return product;
	}

	public Product updateProductCategories(
			long productId, long[] categoryIds, ServiceContext serviceContext)
		throws PortalException {

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

		return product;
	}

	public Product updateProductStatus(
			long productId, int status, ServiceContext serviceContext)
		throws PortalException {

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

		return product;
	}

	public Product updateProductTags(
			long productId, long[] tagIds, ServiceContext serviceContext)
		throws PortalException {

		Product product = productPersistence.findByPrimaryKey(productId);

		serviceContext = _getServiceContext(
			serviceContext, product.getUserId(), product.getGroupId());

		_updateAsset(
			product, _getAssetCategoryIds(productId),
			_resolveTagNames(product.getGroupId(), tagIds), serviceContext);

		return product;
	}

	private void _deleteAssetEntry(long productId) {
		try {
			assetEntryLocalService.deleteEntry(Product.class.getName(), productId);
		}
		catch (PortalException portalException) {
		}
	}

	private long[] _getAssetCategoryIds(long productId) {
		try {
			AssetEntry assetEntry = assetEntryLocalService.getEntry(
				Product.class.getName(), productId);

			return assetEntry.getCategoryIds();
		}
		catch (PortalException portalException) {
			return new long[0];
		}
	}

	private String[] _getAssetTagNames(long productId) {
		try {
			AssetEntry assetEntry = assetEntryLocalService.getEntry(
				Product.class.getName(), productId);

			return assetEntry.getTagNames();
		}
		catch (PortalException portalException) {
			return new String[0];
		}
	}

	private ServiceContext _getServiceContext(
		ServiceContext serviceContext, long userId, long groupId) {

		if (serviceContext != null) {
			return serviceContext;
		}

		ServiceContext newServiceContext = new ServiceContext();

		newServiceContext.setScopeGroupId(groupId);
		newServiceContext.setUserId(userId);

		return newServiceContext;
	}

	private String _normalize(String value) {
		if (value == null) {
			return null;
		}

		return value.trim();
	}

	private String[] _resolveTagNames(long groupId, long[] tagIds)
		throws PortalException {

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

		return tagNames;
	}

	private void _updateAsset(
			Product product, long[] categoryIds, String[] tagNames,
			ServiceContext serviceContext)
		throws PortalException {

		boolean visible = product.getStatus() == ProductStatusConstants.PUBLISHED;

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

		return Arrays.copyOf(categoryIds, categoryIds.length);
	}

	private void _validate(
			int status, int stockQuantity, String name, String description,
			double price, long[] categoryIds)
		throws PortalException {

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
	}

	private void _validatePublishedProduct(
			String name, String description, double price, long[] categoryIds)
		throws PortalException {

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
	}

	private void _validateTransition(int currentStatus, int newStatus)
		throws PortalException {

		if (currentStatus == newStatus) {
			return;
		}

		if ((currentStatus == ProductStatusConstants.DRAFT) &&
			((newStatus == ProductStatusConstants.PUBLISHED) ||
			 (newStatus == ProductStatusConstants.INACTIVE))) {

			return;
		}

		if ((currentStatus == ProductStatusConstants.PUBLISHED) &&
			(newStatus == ProductStatusConstants.INACTIVE)) {

			return;
		}

		if ((currentStatus == ProductStatusConstants.INACTIVE) &&
			((newStatus == ProductStatusConstants.DRAFT) ||
			 (newStatus == ProductStatusConstants.PUBLISHED))) {

			return;
		}

		throw new ProductStatusException(
			"Invalid status transition from " + currentStatus + " to " +
				newStatus);
	}

	@Reference
	private AssetCategoryLocalService assetCategoryLocalService;

}
