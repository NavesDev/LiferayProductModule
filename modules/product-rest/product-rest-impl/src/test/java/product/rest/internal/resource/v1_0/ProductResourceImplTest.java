package product.rest.internal.resource.v1_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.service.AssetEntryLocalService;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.oauth2.provider.scope.ScopeChecker;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.vulcan.pagination.Page;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import product.rest.dto.v1_0.Product;
import product.rest.dto.v1_0.ProductCategories;
import product.rest.dto.v1_0.ProductTags;
import product.service.model.impl.ProductImpl;
import product.service.service.ProductLocalService;

@DisplayName("ProductResourceImpl")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductResourceImplTest {

	// ── Constantes ────────────────────────────────────────────────────────────

	private static final long COMPANY_ID = 10001L;
	private static final long GROUP_ID = 30001L;
	private static final long PRODUCT_ID = 40001L;
	private static final long SITE_ID = 30001L;
	private static final long USER_ID = 20001L;

	// ── Mocks ─────────────────────────────────────────────────────────────────

	@Mock
	private AssetEntry assetEntry;

	@Mock
	private AssetEntryLocalService _assetEntryLocalService;

	@Mock
	private ScopeChecker contextScopeChecker;

	@Mock
	private AssetTag assetTag;

	@Mock
	private AssetTagLocalService _assetTagLocalService;

	@Mock
	private Company company;

	@Mock
	private ProductLocalService _productLocalService;

	@Mock
	private User user;

	// ── Captors ───────────────────────────────────────────────────────────────

	@Captor
	private ArgumentCaptor<ServiceContext> serviceContextCaptor;

	// ── SUT (sempre último) ───────────────────────────────────────────────────

	@InjectMocks
	private ProductResourceImpl productResource;

	// ── Infraestrutura JAX-RS ─────────────────────────────────────────────────

	@BeforeAll
	static void setUpJaxRs() {
		RuntimeDelegate.setInstance(new _StubRuntimeDelegate());
	}

	// ── Setup ─────────────────────────────────────────────────────────────────

	@BeforeEach
	void setUp() {
		productResource.setContextCompany(company);
		productResource.setContextUser(user);

		assertThat(productResource).isNotNull();

		lenient().when(company.getCompanyId()).thenReturn(COMPANY_ID);
		lenient().when(user.getUserId()).thenReturn(USER_ID);
		lenient().when(user.getGroupId()).thenReturn(GROUP_ID);
	}

	// ── Testes ────────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Criar produto")
	class PostProduct {

		@Test
		@DisplayName("Dado payload valido, quando criar, entao delega ao local service e retorna DTO")
		void dado_payloadValido_quando_criar_entao_delegaAoLocalServiceERetornaDto()
			throws Exception {

			// Arrange
			Product payload = new Product();

			payload.setName("Produto A");
			payload.setDescription("Descricao A");
			payload.setPrice(19.9D);
			payload.setStatus(Product.Status.DRAFT);
			payload.setStockQuantity(7);
			payload.setCategoryIds(new Long[] {11L});
			payload.setTagIds(new Long[] {22L});

			ProductImpl created = _product(PRODUCT_ID, "Produto A", 7);

			when(
				_productLocalService.addProduct(
					anyLong(), anyLong(), anyString(), anyString(), anyDouble(),
					anyInt(), anyInt(), any(long[].class), any(long[].class),
					any(ServiceContext.class))
			).thenReturn(created);
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), PRODUCT_ID)
			).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[] {11L});
			when(assetEntry.getTagNames()).thenReturn(new String[] {"tag-22"});
			when(_assetTagLocalService.fetchTag(GROUP_ID, "tag-22")).thenReturn(assetTag);
			when(assetTag.getTagId()).thenReturn(22L);

			// Act
			Product response = productResource.postSiteProduct(SITE_ID, payload);

			// Assert
			assertThat(response.getId()).isEqualTo(PRODUCT_ID);
			assertThat(response.getName()).isEqualTo("Produto A");
			assertThat(response.getCategoryIds()).containsExactly(11L);
			assertThat(response.getTagIds()).containsExactly(22L);

			verify(_productLocalService).addProduct(
				anyLong(), anyLong(), anyString(), anyString(), anyDouble(),
				anyInt(), anyInt(), any(long[].class), any(long[].class),
				serviceContextCaptor.capture());

			assertThat(serviceContextCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
			assertThat(serviceContextCaptor.getValue().getUserId()).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("Dado payload nulo, quando criar, entao lanca BadRequestException")
		void dado_payloadNulo_quando_criar_entao_lancaBadRequestException()
			throws Exception {

			// Act / Assert
			assertThatThrownBy(() -> productResource.postSiteProduct(SITE_ID, null))
				.isInstanceOf(BadRequestException.class);
		}
	}

	@Nested
	@DisplayName("Consultar produtos")
	class GetProducts {

		@Test
		@DisplayName("Dado filtro status, quando listar, entao retorna apenas produtos publicados")
		void dado_filtroStatus_quando_listar_entao_retornaApenasProdutosPublicados()
			throws Exception {

			// Arrange
			ProductImpl publishedInStock = _product(1L, "Notebook", 5);

			publishedInStock.setStatus(1);

			ProductImpl draftOutStock = _product(2L, "Mouse", 0);

			draftOutStock.setStatus(0);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(publishedInStock, draftOutStock));
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 2L)
			).thenReturn(assetEntry);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, "published", null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getId()).isEqualTo(1L);
		}
	}

	@Nested
	@DisplayName("Consultar produto do site")
	class GetSiteProduct {

		@Test
		@DisplayName("Dado produto inexistente no site, quando buscar, entao lanca BadRequestException")
		void dado_produtoInexistenteNoSite_quando_buscar_entao_lancaBadRequestException()
			throws Exception {

			// Arrange
			ProductImpl produtoOutroSite = _product(PRODUCT_ID, "Produto X", 1);

			produtoOutroSite.setGroupId(99999L);

			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(produtoOutroSite);

			// Act / Assert
			assertThatThrownBy(() -> productResource.getSiteProduct(SITE_ID, PRODUCT_ID))
				.isInstanceOf(BadRequestException.class);
		}
	}

	@Nested
	@DisplayName("Atualizar classificacao")
	class UpdateClassification {

		@Test
		@DisplayName("Dado categorias, quando atualizar categorias, entao delega e retorna produto")
		void dado_categorias_quando_atualizarCategorias_entao_delegaERetornaProduto()
			throws Exception {

			// Arrange
			ProductCategories productCategories = new ProductCategories();

			productCategories.setCategoryIds(new Long[] {9L, 10L});

			ProductImpl updated = _product(PRODUCT_ID, "Produto A", 3);

			when(
				_productLocalService.updateProductCategories(
					anyLong(), any(), any(ServiceContext.class))
			).thenReturn(updated);
			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(updated);

			// Act
			Product response = productResource.putSiteProductCategories(
				SITE_ID, PRODUCT_ID, productCategories);

			// Assert
			assertThat(response.getId()).isEqualTo(PRODUCT_ID);

			verify(_productLocalService).updateProductCategories(
				anyLong(), any(), serviceContextCaptor.capture());

			assertThat(serviceContextCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
			assertThat(serviceContextCaptor.getValue().getUserId()).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("Dado tags, quando atualizar tags, entao delega e retorna produto")
		void dado_tags_quando_atualizarTags_entao_delegaERetornaProduto()
			throws Exception {

			// Arrange
			ProductTags productTags = new ProductTags();

			productTags.setTagIds(new Long[] {7L});

			ProductImpl updated = _product(PRODUCT_ID, "Produto A", 3);

			when(
				_productLocalService.updateProductTags(
					anyLong(), any(), any(ServiceContext.class))
			).thenReturn(updated);
			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(updated);

			// Act
			Product response = productResource.putSiteProductTags(
				SITE_ID, PRODUCT_ID, productTags);

			// Assert
			assertThat(response.getId()).isEqualTo(PRODUCT_ID);

			verify(_productLocalService).updateProductTags(
				anyLong(), any(), serviceContextCaptor.capture());

			assertThat(serviceContextCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
			assertThat(serviceContextCaptor.getValue().getUserId()).isEqualTo(USER_ID);
		}

		@Test
		@DisplayName("Dado payload nulo em categorias, quando atualizar, entao lanca BadRequestException")
		void dado_payloadNuloEmCategorias_quando_atualizar_entao_lancaBadRequestException()
			throws Exception {

			// Act / Assert
			assertThatThrownBy(
				() -> productResource.putSiteProductCategories(SITE_ID, PRODUCT_ID, null))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("Dado payload nulo em tags, quando atualizar, entao lanca BadRequestException")
		void dado_payloadNuloEmTags_quando_atualizar_entao_lancaBadRequestException()
			throws Exception {

			// Act / Assert
			assertThatThrownBy(
				() -> productResource.putSiteProductTags(SITE_ID, PRODUCT_ID, null))
				.isInstanceOf(BadRequestException.class);
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private ProductImpl _product(long id, String name, int stockQuantity) {
		ProductImpl product = new ProductImpl();

		product.setProductId(id);
		product.setDescription("Descricao");
		product.setGroupId(SITE_ID);
		product.setName(name);
		product.setPrice(10.5D);
		product.setStatus(0);
		product.setStockQuantity(stockQuantity);

		return product;
	}

	// ── JAX-RS stub ───────────────────────────────────────────────────────────

	private static class _StubRuntimeDelegate extends RuntimeDelegate {

		@Override
		public ResponseBuilder createResponseBuilder() {
			return new _StubResponseBuilder();
		}

		@Override
		public UriBuilder createUriBuilder() {
			return mock(UriBuilder.class);
		}

		@Override
		public Variant.VariantListBuilder createVariantListBuilder() {
			return mock(Variant.VariantListBuilder.class);
		}

		@Override
		public <T> T createEndpoint(
			Application application, Class<T> endpointType) {

			return null;
		}

		@Override
		public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
			return null;
		}

		@Override
		public Link.Builder createLinkBuilder() {
			return mock(Link.Builder.class);
		}

	}

	private static class _StubResponseBuilder extends ResponseBuilder {

		private int _status;

		@Override
		public Response build() {
			return new _StubResponse(_status);
		}

		@Override
		public ResponseBuilder clone() {
			_StubResponseBuilder builder = new _StubResponseBuilder();

			builder._status = _status;

			return builder;
		}

		@Override
		public ResponseBuilder status(int status) {
			_status = status;

			return this;
		}

		@Override
		public ResponseBuilder status(int status, String reasonPhrase) {
			_status = status;

			return this;
		}

		@Override
		public ResponseBuilder entity(Object entity) {
			return this;
		}

		@Override
		public ResponseBuilder entity(Object entity, Annotation[] annotations) {
			return this;
		}

		@Override
		public ResponseBuilder allow(String... methods) {
			return this;
		}

		@Override
		public ResponseBuilder allow(Set<String> methods) {
			return this;
		}

		@Override
		public ResponseBuilder cacheControl(
			javax.ws.rs.core.CacheControl cacheControl) {

			return this;
		}

		@Override
		public ResponseBuilder encoding(String encoding) {
			return this;
		}

		@Override
		public ResponseBuilder header(String name, Object value) {
			return this;
		}

		@Override
		public ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
			return this;
		}

		@Override
		public ResponseBuilder language(String language) {
			return this;
		}

		@Override
		public ResponseBuilder language(Locale language) {
			return this;
		}

		@Override
		public ResponseBuilder type(MediaType type) {
			return this;
		}

		@Override
		public ResponseBuilder type(String type) {
			return this;
		}

		@Override
		public ResponseBuilder variant(Variant variant) {
			return this;
		}

		@Override
		public ResponseBuilder contentLocation(URI location) {
			return this;
		}

		@Override
		public ResponseBuilder cookie(NewCookie... cookies) {
			return this;
		}

		@Override
		public ResponseBuilder expires(Date expires) {
			return this;
		}

		@Override
		public ResponseBuilder lastModified(Date lastModified) {
			return this;
		}

		@Override
		public ResponseBuilder location(URI location) {
			return this;
		}

		@Override
		public ResponseBuilder tag(EntityTag tag) {
			return this;
		}

		@Override
		public ResponseBuilder tag(String tag) {
			return this;
		}

		@Override
		public ResponseBuilder variants(Variant... variants) {
			return this;
		}

		@Override
		public ResponseBuilder variants(List<Variant> variants) {
			return this;
		}

		@Override
		public ResponseBuilder links(Link... links) {
			return this;
		}

		@Override
		public ResponseBuilder link(URI uri, String rel) {
			return this;
		}

		@Override
		public ResponseBuilder link(String uri, String rel) {
			return this;
		}

	}

	private static class _StubResponse extends Response {

		private final int _status;

		_StubResponse(int status) {
			_status = status;
		}

		@Override
		public int getStatus() {
			return _status;
		}

		@Override
		public StatusType getStatusInfo() {
			return Status.fromStatusCode(_status);
		}

		@Override
		public Object getEntity() {
			return null;
		}

		@Override
		public <T> T readEntity(Class<T> entityType) {
			return null;
		}

		@Override
		public <T> T readEntity(javax.ws.rs.core.GenericType<T> entityType) {
			return null;
		}

		@Override
		public <T> T readEntity(
			Class<T> entityType, Annotation[] annotations) {

			return null;
		}

		@Override
		public <T> T readEntity(
			javax.ws.rs.core.GenericType<T> entityType,
			Annotation[] annotations) {

			return null;
		}

		@Override
		public boolean hasEntity() {
			return false;
		}

		@Override
		public boolean bufferEntity() {
			return false;
		}

		@Override
		public void close() {
		}

		@Override
		public MediaType getMediaType() {
			return null;
		}

		@Override
		public Locale getLanguage() {
			return null;
		}

		@Override
		public int getLength() {
			return -1;
		}

		@Override
		public Set<String> getAllowedMethods() {
			return null;
		}

		@Override
		public Map<String, NewCookie> getCookies() {
			return null;
		}

		@Override
		public EntityTag getEntityTag() {
			return null;
		}

		@Override
		public Date getDate() {
			return null;
		}

		@Override
		public Date getLastModified() {
			return null;
		}

		@Override
		public URI getLocation() {
			return null;
		}

		@Override
		public Set<Link> getLinks() {
			return null;
		}

		@Override
		public boolean hasLink(String relation) {
			return false;
		}

		@Override
		public Link getLink(String relation) {
			return null;
		}

		@Override
		public Link.Builder getLinkBuilder(String relation) {
			return null;
		}

		@Override
		public MultivaluedMap<String, Object> getMetadata() {
			return null;
		}

		@Override
		public MultivaluedMap<String, String> getStringHeaders() {
			return null;
		}

		@Override
		public String getHeaderString(String name) {
			return null;
		}

	}

}
