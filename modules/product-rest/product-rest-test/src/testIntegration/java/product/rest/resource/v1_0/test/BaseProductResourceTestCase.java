package product.rest.resource.v1_0.test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import com.liferay.petra.function.UnsafeTriConsumer;
import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONDeserializer;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.FastDateFormatFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.odata.entity.EntityField;
import com.liferay.portal.odata.entity.EntityModel;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.vulcan.resource.EntityModelResource;
import com.liferay.portal.vulcan.util.TransformUtil;

import java.lang.reflect.Method;

import java.text.Format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Generated;

import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import product.rest.client.dto.v1_0.Product;
import product.rest.client.dto.v1_0.ProductCategories;
import product.rest.client.dto.v1_0.ProductTags;
import product.rest.client.http.HttpInvoker;
import product.rest.client.pagination.Page;
import product.rest.client.pagination.Pagination;
import product.rest.client.resource.v1_0.ProductResource;
import product.rest.client.serdes.v1_0.ProductSerDes;

/**
 * @author naves
 * @generated
 */
@Generated("")
public abstract class BaseProductResourceTestCase {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@BeforeClass
	public static void setUpClass() throws Exception {
		_format = FastDateFormatFactoryUtil.getSimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");
	}

	@Before
	public void setUp() throws Exception {
		irrelevantGroup = GroupTestUtil.addGroup();
		testGroup = GroupTestUtil.addGroup();

		testCompany = CompanyLocalServiceUtil.getCompany(
			testGroup.getCompanyId());

		_productResource.setContextCompany(testCompany);

		_testCompanyAdminUser = UserTestUtil.getAdminUser(
			testCompany.getCompanyId());

		productResource = ProductResource.builder(
		).authentication(
			_testCompanyAdminUser.getEmailAddress(),
			PropsValues.DEFAULT_ADMIN_PASSWORD
		).endpoint(
			testCompany.getVirtualHostname(), 8080, "http"
		).locale(
			LocaleUtil.getDefault()
		).build();
	}

	@After
	public void tearDown() throws Exception {
		GroupTestUtil.deleteGroup(irrelevantGroup);
		GroupTestUtil.deleteGroup(testGroup);
	}

	@Test
	public void testClientSerDesToDTO() throws Exception {
		ObjectMapper objectMapper = getClientSerDesObjectMapper();

		Product product1 = randomProduct();

		String json = objectMapper.writeValueAsString(product1);

		Product product2 = ProductSerDes.toDTO(json);

		Assert.assertTrue(equals(product1, product2));
	}

	@Test
	public void testClientSerDesToJSON() throws Exception {
		ObjectMapper objectMapper = getClientSerDesObjectMapper();

		Product product = randomProduct();

		String json1 = objectMapper.writeValueAsString(product);
		String json2 = ProductSerDes.toJSON(product);

		Assert.assertEquals(
			objectMapper.readTree(json1), objectMapper.readTree(json2));
	}

	protected ObjectMapper getClientSerDesObjectMapper() {
		return new ObjectMapper() {
			{
				configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
				configure(
					SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
				enable(SerializationFeature.INDENT_OUTPUT);
				setDateFormat(new ISO8601DateFormat());
				setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
				setSerializationInclusion(JsonInclude.Include.NON_NULL);
				setVisibility(
					PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
				setVisibility(
					PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
			}
		};
	}

	@Test
	public void testEscapeRegexInStringFields() throws Exception {
		String regex = "^[0-9]+(\\.[0-9]{1,2})\"?";

		Product product = randomProduct();

		product.setDescription(regex);
		product.setName(regex);

		String json = ProductSerDes.toJSON(product);

		Assert.assertFalse(json.contains(regex));

		product = ProductSerDes.toDTO(json);

		Assert.assertEquals(regex, product.getDescription());
		Assert.assertEquals(regex, product.getName());
	}

	@Test
	public void testDeleteSiteProduct() throws Exception {
		@SuppressWarnings("PMD.UnusedLocalVariable")
		Product product = testDeleteSiteProduct_addProduct();

		assertHttpResponseStatusCode(
			204,
			productResource.deleteSiteProductHttpResponse(
				testDeleteSiteProduct_getSiteId(), product.getId()));

		assertHttpResponseStatusCode(
			404,
			productResource.getSiteProductHttpResponse(
				testDeleteSiteProduct_getSiteId(), product.getId()));
		assertHttpResponseStatusCode(
			404,
			productResource.getSiteProductHttpResponse(
				testDeleteSiteProduct_getSiteId(), 0L));
	}

	protected Product testDeleteSiteProduct_addProduct() throws Exception {
		return productResource.postSiteProduct(
			testGroup.getGroupId(), randomProduct());
	}

	protected Long testDeleteSiteProduct_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	@Test
	public void testGraphQLDeleteSiteProduct() throws Exception {

		// No namespace

		Product product1 = testGraphQLDeleteSiteProduct_addProduct();

		Assert.assertTrue(
			JSONUtil.getValueAsBoolean(
				invokeGraphQLMutation(
					new GraphQLField(
						"deleteSiteProduct",
						new HashMap<String, Object>() {
							{
								put(
									"siteKey",
									"\"" +
										testGraphQLDeleteSiteProduct_getSiteId() +
											"\"");
								put("productId", product1.getId());
							}
						})),
				"JSONObject/data", "Object/deleteSiteProduct"));

		JSONArray errorsJSONArray1 = JSONUtil.getValueAsJSONArray(
			invokeGraphQLQuery(
				new GraphQLField(
					"product",
					new HashMap<String, Object>() {
						{
							put(
								"siteKey",
								"\"" +
									testGraphQLDeleteSiteProduct_getSiteId() +
										"\"");
							put("productId", product1.getId());
						}
					},
					getGraphQLFields())),
			"JSONArray/errors");

		Assert.assertTrue(errorsJSONArray1.length() > 0);
	}

	protected Long testGraphQLDeleteSiteProduct_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	protected Product testGraphQLDeleteSiteProduct_addProduct()
		throws Exception {

		return testGraphQLSiteProduct_addProduct();
	}

	@Test
	public void testGetSiteProduct() throws Exception {
		Product postProduct = testGetSiteProduct_addProduct();

		Product getProduct = productResource.getSiteProduct(
			testGetSiteProduct_getSiteId(), postProduct.getId());

		assertEquals(postProduct, getProduct);
		assertValid(getProduct);
	}

	protected Product testGetSiteProduct_addProduct() throws Exception {
		return productResource.postSiteProduct(
			testGroup.getGroupId(), randomProduct());
	}

	protected Long testGetSiteProduct_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	@Test
	public void testGraphQLGetSiteProduct() throws Exception {
		Product product = testGraphQLGetSiteProduct_addProduct();

		// No namespace

		Assert.assertTrue(
			equals(
				product,
				ProductSerDes.toDTO(
					JSONUtil.getValueAsString(
						invokeGraphQLQuery(
							new GraphQLField(
								"product",
								new HashMap<String, Object>() {
									{
										put(
											"siteKey",
											"\"" +
												testGraphQLGetSiteProduct_getSiteId() +
													"\"");
										put("productId", product.getId());
									}
								},
								getGraphQLFields())),
						"JSONObject/data", "Object/product"))));
	}

	protected Long testGraphQLGetSiteProduct_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	@Test
	public void testGraphQLGetSiteProductNotFound() throws Exception {
		Long irrelevantProductId = RandomTestUtil.randomLong();

		// No namespace

		Assert.assertEquals(
			"Not Found",
			JSONUtil.getValueAsString(
				invokeGraphQLQuery(
					new GraphQLField(
						"product",
						new HashMap<String, Object>() {
							{
								put(
									"siteKey",
									"\"" + irrelevantGroup.getGroupId() + "\"");
								put("productId", irrelevantProductId);
							}
						},
						getGraphQLFields())),
				"JSONArray/errors", "Object/0", "JSONObject/extensions",
				"Object/code"));
	}

	protected Product testGraphQLGetSiteProduct_addProduct() throws Exception {
		return testGraphQLSiteProduct_addProduct();
	}

	@Test
	public void testGetSiteProductsPage() throws Exception {
		Long siteId = testGetSiteProductsPage_getSiteId();
		Long irrelevantSiteId = testGetSiteProductsPage_getIrrelevantSiteId();

		Page<Product> page = productResource.getSiteProductsPage(
			siteId, null, RandomTestUtil.randomString(), null, null, null,
			Pagination.of(1, 10), null);

		long totalCount = page.getTotalCount();

		if (irrelevantSiteId != null) {
			Product irrelevantProduct = testGetSiteProductsPage_addProduct(
				irrelevantSiteId, randomIrrelevantProduct());

			page = productResource.getSiteProductsPage(
				irrelevantSiteId, null, null, null, null, null,
				Pagination.of(1, (int)totalCount + 1), null);

			Assert.assertEquals(totalCount + 1, page.getTotalCount());

			assertContains(irrelevantProduct, (List<Product>)page.getItems());
			assertValid(
				page,
				testGetSiteProductsPage_getExpectedActions(irrelevantSiteId));
		}

		Product product1 = testGetSiteProductsPage_addProduct(
			siteId, randomProduct());

		Product product2 = testGetSiteProductsPage_addProduct(
			siteId, randomProduct());

		page = productResource.getSiteProductsPage(
			siteId, null, null, null, null, null, Pagination.of(1, 10), null);

		Assert.assertEquals(totalCount + 2, page.getTotalCount());

		assertContains(product1, (List<Product>)page.getItems());
		assertContains(product2, (List<Product>)page.getItems());
		assertValid(page, testGetSiteProductsPage_getExpectedActions(siteId));
	}

	protected Map<String, Map<String, String>>
			testGetSiteProductsPage_getExpectedActions(Long siteId)
		throws Exception {

		Map<String, Map<String, String>> expectedActions = new HashMap<>();

		Map createBatchAction = new HashMap<>();
		createBatchAction.put("method", "POST");
		createBatchAction.put(
			"href",
			"http://localhost:8080/o/product-rest/v1.0/sites/{siteId}/products/batch".
				replace("{siteId}", String.valueOf(siteId)));

		expectedActions.put("createBatch", createBatchAction);

		return expectedActions;
	}

	@Test
	public void testGetSiteProductsPageWithPagination() throws Exception {
		Long siteId = testGetSiteProductsPage_getSiteId();

		Page<Product> productsPage = productResource.getSiteProductsPage(
			siteId, null, null, null, null, null, null, null);

		int totalCount = GetterUtil.getInteger(productsPage.getTotalCount());

		Product product1 = testGetSiteProductsPage_addProduct(
			siteId, randomProduct());

		Product product2 = testGetSiteProductsPage_addProduct(
			siteId, randomProduct());

		Product product3 = testGetSiteProductsPage_addProduct(
			siteId, randomProduct());

		// See com.liferay.portal.vulcan.internal.configuration.HeadlessAPICompanyConfiguration#pageSizeLimit

		int pageSizeLimit = 500;

		if (totalCount >= (pageSizeLimit - 2)) {
			Page<Product> page1 = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(
					(int)Math.ceil((totalCount + 1.0) / pageSizeLimit),
					pageSizeLimit),
				null);

			Assert.assertEquals(totalCount + 3, page1.getTotalCount());

			assertContains(product1, (List<Product>)page1.getItems());

			Page<Product> page2 = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(
					(int)Math.ceil((totalCount + 2.0) / pageSizeLimit),
					pageSizeLimit),
				null);

			assertContains(product2, (List<Product>)page2.getItems());

			Page<Product> page3 = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(
					(int)Math.ceil((totalCount + 3.0) / pageSizeLimit),
					pageSizeLimit),
				null);

			assertContains(product3, (List<Product>)page3.getItems());
		}
		else {
			Page<Product> page1 = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(1, totalCount + 2), null);

			List<Product> products1 = (List<Product>)page1.getItems();

			Assert.assertEquals(
				products1.toString(), totalCount + 2, products1.size());

			Page<Product> page2 = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(2, totalCount + 2), null);

			Assert.assertEquals(totalCount + 3, page2.getTotalCount());

			List<Product> products2 = (List<Product>)page2.getItems();

			Assert.assertEquals(products2.toString(), 1, products2.size());

			Page<Product> page3 = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(1, (int)totalCount + 3), null);

			assertContains(product1, (List<Product>)page3.getItems());
			assertContains(product2, (List<Product>)page3.getItems());
			assertContains(product3, (List<Product>)page3.getItems());
		}
	}

	@Test
	public void testGetSiteProductsPageWithSortDateTime() throws Exception {
		testGetSiteProductsPageWithSort(
			EntityField.Type.DATE_TIME,
			(entityField, product1, product2) -> {
				BeanTestUtil.setProperty(
					product1, entityField.getName(),
					new Date(System.currentTimeMillis() - (2 * Time.MINUTE)));
			});
	}

	@Test
	public void testGetSiteProductsPageWithSortDouble() throws Exception {
		testGetSiteProductsPageWithSort(
			EntityField.Type.DOUBLE,
			(entityField, product1, product2) -> {
				BeanTestUtil.setProperty(product1, entityField.getName(), 0.1);
				BeanTestUtil.setProperty(product2, entityField.getName(), 0.5);
			});
	}

	@Test
	public void testGetSiteProductsPageWithSortInteger() throws Exception {
		testGetSiteProductsPageWithSort(
			EntityField.Type.INTEGER,
			(entityField, product1, product2) -> {
				BeanTestUtil.setProperty(product1, entityField.getName(), 0);
				BeanTestUtil.setProperty(product2, entityField.getName(), 1);
			});
	}

	@Test
	public void testGetSiteProductsPageWithSortString() throws Exception {
		testGetSiteProductsPageWithSort(
			EntityField.Type.STRING,
			(entityField, product1, product2) -> {
				Class<?> clazz = product1.getClass();

				String entityFieldName = entityField.getName();

				Method method = clazz.getMethod(
					"get" + StringUtil.upperCaseFirstLetter(entityFieldName));

				Class<?> returnType = method.getReturnType();

				if (returnType.isAssignableFrom(Map.class)) {
					BeanTestUtil.setProperty(
						product1, entityFieldName,
						Collections.singletonMap("Aaa", "Aaa"));
					BeanTestUtil.setProperty(
						product2, entityFieldName,
						Collections.singletonMap("Bbb", "Bbb"));
				}
				else if (entityFieldName.contains("email")) {
					BeanTestUtil.setProperty(
						product1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
					BeanTestUtil.setProperty(
						product2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()) +
									"@liferay.com");
				}
				else {
					BeanTestUtil.setProperty(
						product1, entityFieldName,
						"aaa" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
					BeanTestUtil.setProperty(
						product2, entityFieldName,
						"bbb" +
							StringUtil.toLowerCase(
								RandomTestUtil.randomString()));
				}
			});
	}

	protected void testGetSiteProductsPageWithSort(
			EntityField.Type type,
			UnsafeTriConsumer<EntityField, Product, Product, Exception>
				unsafeTriConsumer)
		throws Exception {

		List<EntityField> entityFields = getEntityFields(type);

		if (entityFields.isEmpty()) {
			return;
		}

		Long siteId = testGetSiteProductsPage_getSiteId();

		Product product1 = randomProduct();
		Product product2 = randomProduct();

		for (EntityField entityField : entityFields) {
			unsafeTriConsumer.accept(entityField, product1, product2);
		}

		product1 = testGetSiteProductsPage_addProduct(siteId, product1);

		product2 = testGetSiteProductsPage_addProduct(siteId, product2);

		Page<Product> page = productResource.getSiteProductsPage(
			siteId, null, null, null, null, null, null, null);

		for (EntityField entityField : entityFields) {
			Page<Product> ascPage = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(1, (int)page.getTotalCount() + 1),
				entityField.getName() + ":asc");

			assertContains(product1, (List<Product>)ascPage.getItems());
			assertContains(product2, (List<Product>)ascPage.getItems());

			Page<Product> descPage = productResource.getSiteProductsPage(
				siteId, null, null, null, null, null,
				Pagination.of(1, (int)page.getTotalCount() + 1),
				entityField.getName() + ":desc");

			assertContains(product2, (List<Product>)descPage.getItems());
			assertContains(product1, (List<Product>)descPage.getItems());
		}
	}

	protected Product testGetSiteProductsPage_addProduct(
			Long siteId, Product product)
		throws Exception {

		return productResource.postSiteProduct(siteId, product);
	}

	protected Long testGetSiteProductsPage_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	protected Long testGetSiteProductsPage_getIrrelevantSiteId()
		throws Exception {

		return irrelevantGroup.getGroupId();
	}

	@Test
	public void testGraphQLGetSiteProductsPage() throws Exception {
		Long siteId = testGetSiteProductsPage_getSiteId();

		GraphQLField graphQLField = new GraphQLField(
			"products",
			new HashMap<String, Object>() {
				{
					put("siteKey", "\"" + siteId + "\"");
					put("search", null);
					put(
						"status",
						getGraphQLValue(RandomTestUtil.randomString()));
					put("page", 1);
					put("pageSize", 10);
				}
			},
			new GraphQLField("items", getGraphQLFields()),
			new GraphQLField("page"), new GraphQLField("totalCount"));

		// No namespace

		JSONObject productsJSONObject = JSONUtil.getValueAsJSONObject(
			invokeGraphQLQuery(graphQLField), "JSONObject/data",
			"JSONObject/products");

		long totalCount = productsJSONObject.getLong("totalCount");

		Product product1 = testGraphQLSiteProduct_addProduct(
			siteId, randomProduct());

		Product product2 = testGraphQLSiteProduct_addProduct(
			siteId, randomProduct());

		productsJSONObject = JSONUtil.getValueAsJSONObject(
			invokeGraphQLQuery(graphQLField), "JSONObject/data",
			"JSONObject/products");

		Assert.assertEquals(
			totalCount + 2, productsJSONObject.getLong("totalCount"));

		assertContains(
			product1,
			Arrays.asList(
				ProductSerDes.toDTOs(productsJSONObject.getString("items"))));
		assertContains(
			product2,
			Arrays.asList(
				ProductSerDes.toDTOs(productsJSONObject.getString("items"))));
	}

	@Test
	public void testPostSiteProduct() throws Exception {
		Product randomProduct = randomProduct();

		Product postProduct = testPostSiteProduct_addProduct(randomProduct);

		assertEquals(randomProduct, postProduct);
		assertValid(postProduct);
	}

	protected Product testPostSiteProduct_addProduct(Product product)
		throws Exception {

		return productResource.postSiteProduct(
			testGetSiteProductsPage_getSiteId(), product);
	}

	@Test
	public void testGraphQLPostSiteProduct() throws Exception {
		Product randomProduct = randomProduct();

		Product product = testGraphQLSiteProduct_addProduct(
			testGroup.getGroupId(), randomProduct);

		Assert.assertTrue(equals(randomProduct, product));
	}

	@Test
	public void testPutSiteProduct() throws Exception {
		Product postProduct = testPutSiteProduct_addProduct();

		Product randomProduct = randomProduct();

		Product putProduct = productResource.putSiteProduct(
			testPutSiteProduct_getSiteId(), postProduct.getId(), randomProduct);

		assertEquals(randomProduct, putProduct);
		assertValid(putProduct);

		Product getProduct = productResource.getSiteProduct(
			testPutSiteProduct_getSiteId(), putProduct.getId());

		assertEquals(randomProduct, getProduct);
		assertValid(getProduct);
	}

	protected Product testPutSiteProduct_addProduct() throws Exception {
		return productResource.postSiteProduct(
			testGroup.getGroupId(), randomProduct());
	}

	protected Long testPutSiteProduct_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	@Test
	public void testPutSiteProductCategories() throws Exception {
		Product postProduct = testPutSiteProductCategories_addProduct();

		Product randomProduct = randomProduct();

		Product putProduct = productResource.putSiteProductCategories(
			testPutSiteProductCategories_getSiteId(), postProduct.getId(),
			testPutSiteProductCategories_getProductCategories());

		assertEquals(randomProduct, putProduct);
		assertValid(putProduct);

		Product getProduct = testPutSiteProductCategories_getProduct(
			testPutSiteProductCategories_getSiteId(), putProduct.getId());

		assertEquals(randomProduct, getProduct);
		assertValid(getProduct);
	}

	protected Product testPutSiteProductCategories_getProduct(
		Long siteId, Long productId) {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	protected Product testPutSiteProductCategories_addProduct()
		throws Exception {

		return productResource.postSiteProduct(
			testGroup.getGroupId(), randomProduct());
	}

	protected Long testPutSiteProductCategories_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	protected product.rest.dto.v1_0.ProductCategories
			testPutSiteProductCategories_getProductCategories()
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	@Test
	public void testPutSiteProductTags() throws Exception {
		Product postProduct = testPutSiteProductTags_addProduct();

		Product randomProduct = randomProduct();

		Product putProduct = productResource.putSiteProductTags(
			testPutSiteProductTags_getSiteId(), postProduct.getId(),
			testPutSiteProductTags_getProductTags());

		assertEquals(randomProduct, putProduct);
		assertValid(putProduct);

		Product getProduct = testPutSiteProductTags_getProduct(
			testPutSiteProductTags_getSiteId(), putProduct.getId());

		assertEquals(randomProduct, getProduct);
		assertValid(getProduct);
	}

	protected Product testPutSiteProductTags_getProduct(
		Long siteId, Long productId) {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	protected Product testPutSiteProductTags_addProduct() throws Exception {
		return productResource.postSiteProduct(
			testGroup.getGroupId(), randomProduct());
	}

	protected Long testPutSiteProductTags_getSiteId() throws Exception {
		return testGroup.getGroupId();
	}

	protected product.rest.dto.v1_0.ProductTags
			testPutSiteProductTags_getProductTags()
		throws Exception {

		throw new UnsupportedOperationException(
			"This method needs to be implemented");
	}

	protected Product testGraphQLSiteProduct_addProduct() throws Exception {
		return testGraphQLSiteProduct_addProduct(
			testGroup.getGroupId(), randomProduct());
	}

	protected Product testGraphQLSiteProduct_addProduct(
			Long siteId, Product product)
		throws Exception {

		JSONDeserializer<Product> jsonDeserializer =
			JSONFactoryUtil.createJSONDeserializer();

		StringBuilder sb = new StringBuilder("{");

		for (java.lang.reflect.Field field : getDeclaredFields(Product.class)) {
			if (getGraphQLValue(field.get(product)) != null) {
				if (sb.length() > 1) {
					sb.append(", ");
				}

				sb.append(field.getName());
				sb.append(": ");
				sb.append(getGraphQLValue(field.get(product)));
			}
		}

		sb.append("}");

		List<GraphQLField> graphQLFields = getGraphQLFields();

		return jsonDeserializer.deserialize(
			JSONUtil.getValueAsString(
				invokeGraphQLMutation(
					new GraphQLField(
						"createSiteProduct",
						new HashMap<String, Object>() {
							{
								put("siteKey", "\"" + siteId + "\"");
								put("product", sb.toString());
							}
						},
						graphQLFields)),
				"JSONObject/data", "JSONObject/createSiteProduct"),
			Product.class);
	}

	protected String getGraphQLValue(Object value) throws Exception {
		if (value == null) {
			return null;
		}
		else if (value instanceof Boolean || value instanceof Number) {
			return value.toString();
		}
		else if (value instanceof Date date) {
			return "\"" +
				DateUtil.getDate(
					date, "yyyy-MM-dd'T'HH:mm:ss'Z'", LocaleUtil.getDefault(),
					TimeZone.getTimeZone("UTC")) + "\"";
		}
		else if (value instanceof Enum<?> enm) {
			return enm.name();
		}
		else if (value instanceof Map<?, ?> map) {
			List<String> entries = new ArrayList<>();

			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String graphQLValue = getGraphQLValue(entry.getValue());

				if (graphQLValue != null) {
					entries.add(entry.getKey() + ": " + graphQLValue);
				}
			}

			return "{" + String.join(", ", entries) + "}";
		}
		else if (value instanceof Object[] array) {
			List<String> entries = new ArrayList<>();

			for (Object entry : array) {
				String graphQLValue = getGraphQLValue(entry);

				if (graphQLValue != null) {
					entries.add(graphQLValue);
				}
			}

			return "[" + String.join(", ", entries) + "]";
		}
		else if (value instanceof String) {
			return "\"" + value + "\"";
		}
		else {
			List<String> entries = new ArrayList<>();

			Class<?> clazz = value.getClass();
			java.lang.reflect.Field[] declaredFields = getDeclaredFields(clazz);

			if (declaredFields.length == 0) {
				declaredFields = getDeclaredFields(clazz.getSuperclass());
			}

			for (java.lang.reflect.Field field : declaredFields) {
				String graphQLValue = getGraphQLValue(field.get(value));

				if (graphQLValue != null) {
					entries.add(field.getName() + ": " + graphQLValue);
				}
			}

			return "{" + String.join(", ", entries) + "}";
		}
	}

	protected void assertContains(Product product, List<Product> products) {
		boolean contains = false;

		for (Product item : products) {
			if (equals(product, item)) {
				contains = true;

				break;
			}
		}

		Assert.assertTrue(products + " does not contain " + product, contains);
	}

	protected void assertHttpResponseStatusCode(
		int expectedHttpResponseStatusCode,
		HttpInvoker.HttpResponse actualHttpResponse) {

		Assert.assertEquals(
			expectedHttpResponseStatusCode, actualHttpResponse.getStatusCode());
	}

	protected void assertEquals(Product product1, Product product2) {
		Assert.assertTrue(
			product1 + " does not equal " + product2,
			equals(product1, product2));
	}

	protected void assertEquals(
		List<Product> products1, List<Product> products2) {

		Assert.assertEquals(products1.size(), products2.size());

		for (int i = 0; i < products1.size(); i++) {
			Product product1 = products1.get(i);
			Product product2 = products2.get(i);

			assertEquals(product1, product2);
		}
	}

	protected void assertEqualsIgnoringOrder(
		List<Product> products1, List<Product> products2) {

		Assert.assertEquals(products1.size(), products2.size());

		for (Product product1 : products1) {
			boolean contains = false;

			for (Product product2 : products2) {
				if (equals(product1, product2)) {
					contains = true;

					break;
				}
			}

			Assert.assertTrue(
				products2 + " does not contain " + product1, contains);
		}
	}

	protected void assertValid(Product product) throws Exception {
		boolean valid = true;

		if (product.getId() == null) {
			valid = false;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			if (Objects.equals("categoryIds", additionalAssertFieldName)) {
				if (product.getCategoryIds() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("description", additionalAssertFieldName)) {
				if (product.getDescription() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("name", additionalAssertFieldName)) {
				if (product.getName() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("price", additionalAssertFieldName)) {
				if (product.getPrice() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("status", additionalAssertFieldName)) {
				if (product.getStatus() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("stockQuantity", additionalAssertFieldName)) {
				if (product.getStockQuantity() == null) {
					valid = false;
				}

				continue;
			}

			if (Objects.equals("tagIds", additionalAssertFieldName)) {
				if (product.getTagIds() == null) {
					valid = false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		Assert.assertTrue(valid);
	}

	protected void assertValid(Page<Product> page) {
		assertValid(page, Collections.emptyMap());
	}

	protected void assertValid(
		Page<Product> page, Map<String, Map<String, String>> expectedActions) {

		boolean valid = false;

		java.util.Collection<Product> products = page.getItems();

		int size = products.size();

		if ((page.getLastPage() > 0) && (page.getPage() > 0) &&
			(page.getPageSize() > 0) && (page.getTotalCount() > 0) &&
			(size > 0)) {

			valid = true;
		}

		Assert.assertTrue(valid);

		assertValid(page.getActions(), expectedActions);
	}

	protected void assertValid(
		Map<String, Map<String, String>> actions1,
		Map<String, Map<String, String>> actions2) {

		for (String key : actions2.keySet()) {
			Map action = actions1.get(key);

			Assert.assertNotNull(key + " does not contain an action", action);

			Map<String, String> expectedAction = actions2.get(key);

			Assert.assertEquals(
				expectedAction.get("method"), action.get("method"));
			Assert.assertEquals(expectedAction.get("href"), action.get("href"));
		}
	}

	protected String[] getAdditionalAssertFieldNames() {
		return new String[0];
	}

	protected List<GraphQLField> getGraphQLFields() throws Exception {
		List<GraphQLField> graphQLFields = new ArrayList<>();

		graphQLFields.add(new GraphQLField("id"));

		for (java.lang.reflect.Field field :
				getDeclaredFields(product.rest.dto.v1_0.Product.class)) {

			if (!ArrayUtil.contains(
					getAdditionalAssertFieldNames(), field.getName())) {

				continue;
			}

			graphQLFields.addAll(getGraphQLFields(field));
		}

		return graphQLFields;
	}

	protected List<GraphQLField> getGraphQLFields(
			java.lang.reflect.Field... fields)
		throws Exception {

		List<GraphQLField> graphQLFields = new ArrayList<>();

		for (java.lang.reflect.Field field : fields) {
			com.liferay.portal.vulcan.graphql.annotation.GraphQLField
				vulcanGraphQLField = field.getAnnotation(
					com.liferay.portal.vulcan.graphql.annotation.GraphQLField.
						class);

			if (vulcanGraphQLField != null) {
				Class<?> clazz = field.getType();

				if (clazz.isArray()) {
					clazz = clazz.getComponentType();
				}

				List<GraphQLField> childrenGraphQLFields = getGraphQLFields(
					getDeclaredFields(clazz));

				graphQLFields.add(
					new GraphQLField(field.getName(), childrenGraphQLFields));
			}
		}

		return graphQLFields;
	}

	protected String[] getIgnoredEntityFieldNames() {
		return new String[0];
	}

	protected boolean equals(Product product1, Product product2) {
		if (product1 == product2) {
			return true;
		}

		for (String additionalAssertFieldName :
				getAdditionalAssertFieldNames()) {

			if (Objects.equals("categoryIds", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getCategoryIds(), product2.getCategoryIds())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("description", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getDescription(), product2.getDescription())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("id", additionalAssertFieldName)) {
				if (!Objects.deepEquals(product1.getId(), product2.getId())) {
					return false;
				}

				continue;
			}

			if (Objects.equals("name", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getName(), product2.getName())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("price", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getPrice(), product2.getPrice())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("status", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getStatus(), product2.getStatus())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("stockQuantity", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getStockQuantity(),
						product2.getStockQuantity())) {

					return false;
				}

				continue;
			}

			if (Objects.equals("tagIds", additionalAssertFieldName)) {
				if (!Objects.deepEquals(
						product1.getTagIds(), product2.getTagIds())) {

					return false;
				}

				continue;
			}

			throw new IllegalArgumentException(
				"Invalid additional assert field name " +
					additionalAssertFieldName);
		}

		return true;
	}

	protected boolean equals(
		Map<String, Object> map1, Map<String, Object> map2) {

		if (Objects.equals(map1.keySet(), map2.keySet())) {
			for (Map.Entry<String, Object> entry : map1.entrySet()) {
				if (entry.getValue() instanceof Map) {
					if (!equals(
							(Map)entry.getValue(),
							(Map)map2.get(entry.getKey()))) {

						return false;
					}
				}
				else if (!Objects.deepEquals(
							entry.getValue(), map2.get(entry.getKey()))) {

					return false;
				}
			}

			return true;
		}

		return false;
	}

	protected java.lang.reflect.Field[] getDeclaredFields(Class clazz)
		throws Exception {

		if (clazz.getClassLoader() == null) {
			return new java.lang.reflect.Field[0];
		}

		return TransformUtil.transform(
			ReflectionUtil.getDeclaredFields(clazz),
			field -> {
				if (field.isSynthetic()) {
					return null;
				}

				return field;
			},
			java.lang.reflect.Field.class);
	}

	protected java.util.Collection<EntityField> getEntityFields()
		throws Exception {

		if (!(_productResource instanceof EntityModelResource)) {
			throw new UnsupportedOperationException(
				"Resource is not an instance of EntityModelResource");
		}

		EntityModelResource entityModelResource =
			(EntityModelResource)_productResource;

		EntityModel entityModel = entityModelResource.getEntityModel(
			new MultivaluedHashMap());

		if (entityModel == null) {
			return Collections.emptyList();
		}

		Map<String, EntityField> entityFieldsMap =
			entityModel.getEntityFieldsMap();

		return entityFieldsMap.values();
	}

	protected List<EntityField> getEntityFields(EntityField.Type type)
		throws Exception {

		return TransformUtil.transform(
			getEntityFields(),
			entityField -> {
				if (!Objects.equals(entityField.getType(), type) ||
					ArrayUtil.contains(
						getIgnoredEntityFieldNames(), entityField.getName())) {

					return null;
				}

				return entityField;
			});
	}

	protected String getFilterString(
		EntityField entityField, String operator, Product product) {

		StringBundler sb = new StringBundler();

		String entityFieldName = entityField.getName();

		sb.append(entityFieldName);

		sb.append(" ");
		sb.append(operator);
		sb.append(" ");

		if (entityFieldName.equals("categoryIds")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		if (entityFieldName.equals("description")) {
			Object object = product.getDescription();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("id")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		if (entityFieldName.equals("name")) {
			Object object = product.getName();

			String value = String.valueOf(object);

			if (operator.equals("contains")) {
				sb = new StringBundler();

				sb.append("contains(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 2)) {
					sb.append(value.substring(1, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else if (operator.equals("startswith")) {
				sb = new StringBundler();

				sb.append("startswith(");
				sb.append(entityFieldName);
				sb.append(",'");

				if ((object != null) && (value.length() > 1)) {
					sb.append(value.substring(0, value.length() - 1));
				}
				else {
					sb.append(value);
				}

				sb.append("')");
			}
			else {
				sb.append("'");
				sb.append(value);
				sb.append("'");
			}

			return sb.toString();
		}

		if (entityFieldName.equals("price")) {
			sb.append(String.valueOf(product.getPrice()));

			return sb.toString();
		}

		if (entityFieldName.equals("status")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		if (entityFieldName.equals("stockQuantity")) {
			sb.append(String.valueOf(product.getStockQuantity()));

			return sb.toString();
		}

		if (entityFieldName.equals("tagIds")) {
			throw new IllegalArgumentException(
				"Invalid entity field " + entityFieldName);
		}

		throw new IllegalArgumentException(
			"Invalid entity field " + entityFieldName);
	}

	protected String invoke(String query) throws Exception {
		HttpInvoker httpInvoker = HttpInvoker.newHttpInvoker();

		httpInvoker.body(
			JSONUtil.put(
				"query", query
			).toString(),
			"application/json");
		httpInvoker.httpMethod(HttpInvoker.HttpMethod.POST);
		httpInvoker.path("http://localhost:8080/o/graphql");
		httpInvoker.userNameAndPassword(
			"test@liferay.com:" + PropsValues.DEFAULT_ADMIN_PASSWORD);

		HttpInvoker.HttpResponse httpResponse = httpInvoker.invoke();

		return httpResponse.getContent();
	}

	protected JSONObject invokeGraphQLMutation(GraphQLField graphQLField)
		throws Exception {

		GraphQLField mutationGraphQLField = new GraphQLField(
			"mutation", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(mutationGraphQLField.toString()));
	}

	protected JSONObject invokeGraphQLQuery(GraphQLField graphQLField)
		throws Exception {

		GraphQLField queryGraphQLField = new GraphQLField(
			"query", graphQLField);

		return JSONFactoryUtil.createJSONObject(
			invoke(queryGraphQLField.toString()));
	}

	protected Product randomProduct() throws Exception {
		return new Product() {
			{
				description = StringUtil.toLowerCase(
					RandomTestUtil.randomString());
				id = RandomTestUtil.randomLong();
				name = StringUtil.toLowerCase(RandomTestUtil.randomString());
				price = RandomTestUtil.randomDouble();
				stockQuantity = RandomTestUtil.randomInt();
			}
		};
	}

	protected Product randomIrrelevantProduct() throws Exception {
		Product randomIrrelevantProduct = randomProduct();

		return randomIrrelevantProduct;
	}

	protected Product randomPatchProduct() throws Exception {
		return randomProduct();
	}

	protected ProductResource productResource;
	protected com.liferay.portal.kernel.model.Group irrelevantGroup;
	protected com.liferay.portal.kernel.model.Company testCompany;
	protected com.liferay.portal.kernel.model.Group testGroup;

	protected static class BeanTestUtil {

		public static void copyProperties(Object source, Object target)
			throws Exception {

			Class<?> sourceClass = source.getClass();

			Class<?> targetClass = target.getClass();

			for (java.lang.reflect.Field field :
					_getAllDeclaredFields(sourceClass)) {

				if (field.isSynthetic()) {
					continue;
				}

				Method getMethod = _getMethod(
					sourceClass, field.getName(), "get");

				try {
					Method setMethod = _getMethod(
						targetClass, field.getName(), "set",
						getMethod.getReturnType());

					setMethod.invoke(target, getMethod.invoke(source));
				}
				catch (Exception e) {
					continue;
				}
			}
		}

		public static boolean hasProperty(Object bean, String name) {
			Method setMethod = _getMethod(
				bean.getClass(), "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod != null) {
				return true;
			}

			return false;
		}

		public static void setProperty(Object bean, String name, Object value)
			throws Exception {

			Class<?> clazz = bean.getClass();

			Method setMethod = _getMethod(
				clazz, "set" + StringUtil.upperCaseFirstLetter(name));

			if (setMethod == null) {
				throw new NoSuchMethodException();
			}

			Class<?>[] parameterTypes = setMethod.getParameterTypes();

			setMethod.invoke(bean, _translateValue(parameterTypes[0], value));
		}

		private static List<java.lang.reflect.Field> _getAllDeclaredFields(
			Class<?> clazz) {

			List<java.lang.reflect.Field> fields = new ArrayList<>();

			while ((clazz != null) && (clazz != Object.class)) {
				for (java.lang.reflect.Field field :
						clazz.getDeclaredFields()) {

					fields.add(field);
				}

				clazz = clazz.getSuperclass();
			}

			return fields;
		}

		private static Method _getMethod(Class<?> clazz, String name) {
			for (Method method : clazz.getMethods()) {
				if (name.equals(method.getName()) &&
					(method.getParameterCount() == 1) &&
					_parameterTypes.contains(method.getParameterTypes()[0])) {

					return method;
				}
			}

			return null;
		}

		private static Method _getMethod(
				Class<?> clazz, String fieldName, String prefix,
				Class<?>... parameterTypes)
			throws Exception {

			return clazz.getMethod(
				prefix + StringUtil.upperCaseFirstLetter(fieldName),
				parameterTypes);
		}

		private static Object _translateValue(
			Class<?> parameterType, Object value) {

			if ((value instanceof Integer) &&
				parameterType.equals(Long.class)) {

				Integer intValue = (Integer)value;

				return intValue.longValue();
			}

			return value;
		}

		private static final Set<Class<?>> _parameterTypes = new HashSet<>(
			Arrays.asList(
				Boolean.class, Date.class, Double.class, Integer.class,
				Long.class, Map.class, String.class));

	}

	protected class GraphQLField {

		public GraphQLField(String key, GraphQLField... graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(String key, List<GraphQLField> graphQLFields) {
			this(key, new HashMap<>(), graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			GraphQLField... graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = Arrays.asList(graphQLFields);
		}

		public GraphQLField(
			String key, Map<String, Object> parameterMap,
			List<GraphQLField> graphQLFields) {

			_key = key;
			_parameterMap = parameterMap;
			_graphQLFields = graphQLFields;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(_key);

			if (!_parameterMap.isEmpty()) {
				sb.append("(");

				for (Map.Entry<String, Object> entry :
						_parameterMap.entrySet()) {

					sb.append(entry.getKey());
					sb.append(": ");
					sb.append(entry.getValue());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append(")");
			}

			if (!_graphQLFields.isEmpty()) {
				sb.append("{");

				for (GraphQLField graphQLField : _graphQLFields) {
					sb.append(graphQLField.toString());
					sb.append(", ");
				}

				sb.setLength(sb.length() - 2);

				sb.append("}");
			}

			return sb.toString();
		}

		private final List<GraphQLField> _graphQLFields;
		private final String _key;
		private final Map<String, Object> _parameterMap;

	}

	private static final com.liferay.portal.kernel.log.Log _log =
		LogFactoryUtil.getLog(BaseProductResourceTestCase.class);

	private static Format _format;

	private com.liferay.portal.kernel.model.User _testCompanyAdminUser;

	@Inject
	private product.rest.resource.v1_0.ProductResource _productResource;

}