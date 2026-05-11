/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package product.service.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetTagLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetVocabularyLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.test.rule.PersistenceTestRule;
import com.liferay.portal.test.rule.TransactionalTestRule;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import product.service.exception.ProductStatusException;
import product.service.exception.ProductValidationException;
import product.service.model.Product;
import product.service.model.ProductStatusConstants;
import product.service.service.ProductLocalServiceUtil;

/**
 * @author naves
 */
@RunWith(Arquillian.class)
public class ProductLocalServiceIntegrationTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			PersistenceTestRule.INSTANCE,
			new TransactionalTestRule(
				Propagation.REQUIRED, "product.service.service"));

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
		_irrelevantGroup = GroupTestUtil.addGroup();
		_user = UserTestUtil.getAdminUser(_group.getCompanyId());
	}

	@After
	public void tearDown() throws Exception {
		GroupTestUtil.deleteGroup(_irrelevantGroup);
		GroupTestUtil.deleteGroup(_group);
	}

	@Test
	public void testAddProductCreatesDraftProductAndAssetEntry()
		throws Exception {

		AssetCategory assetCategory = _addAssetCategory(_group);
		AssetTag assetTag = _addAssetTag(_group);

		Product product = ProductLocalServiceUtil.addProduct(
			_user.getUserId(), _group.getGroupId(), "  Produto draft  ",
			"  Descricao draft  ", 19.9D, ProductStatusConstants.DRAFT, 7,
			new long[] {assetCategory.getCategoryId()},
			new long[] {assetTag.getTagId()}, _serviceContext(_group));

		Assert.assertEquals("Produto draft", product.getName());
		Assert.assertEquals("Descricao draft", product.getDescription());
		Assert.assertEquals(ProductStatusConstants.DRAFT, product.getStatus());
		Assert.assertEquals(7, product.getStockQuantity());

		AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(
			Product.class.getName(), product.getProductId());

		Assert.assertFalse(assetEntry.isVisible());
		Assert.assertFalse(assetEntry.isListable());
		Assert.assertEquals(product.getName(), assetEntry.getTitle());
		Assert.assertEquals(product.getDescription(), assetEntry.getSummary());
		Assert.assertTrue(
			ArrayUtil.contains(
				assetEntry.getCategoryIds(), assetCategory.getCategoryId()));
		Assert.assertTrue(
			Arrays.asList(
				assetEntry.getTagNames()
			).contains(
				assetTag.getName()
			));
	}

	@Test
	public void testDeleteProductRemovesProductAndAssetEntry() throws Exception {
		Product product = _addProduct(
			_group, "Produto removivel", ProductStatusConstants.DRAFT);

		ProductLocalServiceUtil.deleteProduct(product.getProductId());

		Assert.assertNull(
			ProductLocalServiceUtil.fetchProduct(product.getProductId()));
		Assert.assertNull(
			AssetEntryLocalServiceUtil.fetchEntry(
				Product.class.getName(), product.getProductId()));
	}

	@Test
	public void testGetProductsByGroupIdFiltersAndOrdersByName()
		throws Exception {

		Product betaProduct = _addProduct(
			_group, "Produto B", ProductStatusConstants.DRAFT);
		Product alphaProduct = _addProduct(
			_group, "Produto A", ProductStatusConstants.DRAFT);

		_addProduct(
			_irrelevantGroup, "Produto fora do grupo",
			ProductStatusConstants.DRAFT);

		List<Long> productIds = ProductLocalServiceUtil.getProductsByGroupId(
			_group.getGroupId()
		).stream(
		).map(
			Product::getProductId
		).collect(
			Collectors.toList()
		);

		Assert.assertEquals(
			Arrays.asList(
				alphaProduct.getProductId(), betaProduct.getProductId()),
			productIds);
	}

	@Test
	public void testUpdateProductCategoriesRejectsPublishedProductWithoutCategory()
		throws Exception {

		Product product = _addProduct(
			_group, "Produto publicado", ProductStatusConstants.PUBLISHED);

		try {
			ProductLocalServiceUtil.updateProductCategories(
				product.getProductId(), new long[0], _serviceContext(_group));

			Assert.fail("ProductValidationException expected");
		}
		catch (ProductValidationException productValidationException) {
			Assert.assertTrue(
				productValidationException.getMessage(
				).contains(
					"At least one category"
				));
		}
	}

	@Test
	public void testUpdateProductStatusPublishesDraftAndUpdatesAssetVisibility()
		throws Exception {

		Product product = _addProduct(
			_group, "Produto publicavel", ProductStatusConstants.DRAFT);

		Product updatedProduct = ProductLocalServiceUtil.updateProductStatus(
			product.getProductId(), ProductStatusConstants.PUBLISHED,
			_serviceContext(_group));

		Assert.assertEquals(
			ProductStatusConstants.PUBLISHED, updatedProduct.getStatus());

		AssetEntry assetEntry = AssetEntryLocalServiceUtil.getEntry(
			Product.class.getName(), product.getProductId());

		Assert.assertTrue(assetEntry.isVisible());
		Assert.assertTrue(assetEntry.isListable());
	}

	@Test
	public void testUpdateProductStatusRejectsInvalidTransition()
		throws Exception {

		Product product = _addProduct(
			_group, "Produto publicado", ProductStatusConstants.PUBLISHED);

		try {
			ProductLocalServiceUtil.updateProductStatus(
				product.getProductId(), ProductStatusConstants.DRAFT,
				_serviceContext(_group));

			Assert.fail("ProductStatusException expected");
		}
		catch (ProductStatusException productStatusException) {
			Assert.assertTrue(
				productStatusException.getMessage(
				).contains(
					"Invalid status transition"
				));
		}
	}

	@Test
	public void testUpdateProductTagsReplacesAssetTags() throws Exception {
		AssetTag initialAssetTag = _addAssetTag(_group);
		AssetTag updatedAssetTag = _addAssetTag(_group);

		Product product = _addProduct(
			_group, "Produto com tags", ProductStatusConstants.DRAFT,
			new long[] {initialAssetTag.getTagId()});

		ProductLocalServiceUtil.updateProductTags(
			product.getProductId(), new long[] {updatedAssetTag.getTagId()},
			_serviceContext(_group));

		List<String> tagNames = Arrays.asList(
			AssetEntryLocalServiceUtil.getEntry(
				Product.class.getName(), product.getProductId()
			).getTagNames());

		Assert.assertFalse(tagNames.contains(initialAssetTag.getName()));
		Assert.assertTrue(tagNames.contains(updatedAssetTag.getName()));
	}

	private AssetCategory _addAssetCategory(Group group) throws Exception {
		AssetVocabulary assetVocabulary =
			AssetVocabularyLocalServiceUtil.addVocabulary(
				_user.getUserId(), group.getGroupId(),
				RandomTestUtil.randomString(), _serviceContext(group));

		return AssetCategoryLocalServiceUtil.addCategory(
			_user.getUserId(), group.getGroupId(),
			RandomTestUtil.randomString(), assetVocabulary.getVocabularyId(),
			_serviceContext(group));
	}

	private AssetTag _addAssetTag(Group group) throws Exception {
		return AssetTagLocalServiceUtil.addTag(
			RandomTestUtil.randomString(), _user.getUserId(), group.getGroupId(),
			RandomTestUtil.randomString(), _serviceContext(group));
	}

	private Product _addProduct(Group group, String name, int status)
		throws Exception {

		return _addProduct(group, name, status, new long[0]);
	}

	private Product _addProduct(
			Group group, String name, int status, long[] tagIds)
		throws Exception {

		AssetCategory assetCategory = _addAssetCategory(group);

		return ProductLocalServiceUtil.addProduct(
			_user.getUserId(), group.getGroupId(), name,
			"Descricao " + name, 10D, status, 5,
			new long[] {assetCategory.getCategoryId()}, tagIds,
			_serviceContext(group));
	}

	private ServiceContext _serviceContext(Group group) throws PortalException {
		return ServiceContextTestUtil.getServiceContext(
			group.getGroupId(), _user.getUserId());
	}

	private Group _group;
	private Group _irrelevantGroup;
	private User _user;

}
