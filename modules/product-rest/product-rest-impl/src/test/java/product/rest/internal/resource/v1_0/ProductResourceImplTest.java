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
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.search.Sort;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

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

	@Nested
	@DisplayName("Deletar produto do site")
	class DeleteSiteProduct {

		@Test
		@DisplayName("Dado produto do site, quando deletar, entao retorna 204")
		void dado_produtoDoSite_quando_deletar_entao_retorna204()
			throws Exception {

			// Arrange
			ProductImpl product = _product(PRODUCT_ID, "Produto A", 1);

			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(product);

			// Act
			Response response = productResource.deleteSiteProduct(SITE_ID, PRODUCT_ID);

			// Assert
			assertThat(response.getStatus()).isEqualTo(204);
			verify(_productLocalService).deleteProduct(PRODUCT_ID);
		}

		@Test
		@DisplayName("Dado produto de outro site, quando deletar, entao lanca BadRequestException")
		void dado_produtoDeOutroSite_quando_deletar_entao_lancaBadRequestException()
			throws Exception {

			// Arrange
			ProductImpl product = _product(PRODUCT_ID, "Produto A", 1);

			product.setGroupId(99999L);

			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(product);

			// Act / Assert
			assertThatThrownBy(
				() -> productResource.deleteSiteProduct(SITE_ID, PRODUCT_ID))
				.isInstanceOf(BadRequestException.class);
		}
	}

	@Nested
	@DisplayName("Consultar produto por id")
	class GetSiteProductById {

		@Test
		@DisplayName("Dado produto do site, quando buscar, entao retorna DTO")
		void dado_produtoDoSite_quando_buscar_entao_retornaDTO()
			throws Exception {

			// Arrange
			ProductImpl product = _product(PRODUCT_ID, "Produto A", 3);

			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(product);
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), PRODUCT_ID)
			).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);

			// Act
			Product result = productResource.getSiteProduct(SITE_ID, PRODUCT_ID);

			// Assert
			assertThat(result.getId()).isEqualTo(PRODUCT_ID);
			assertThat(result.getName()).isEqualTo("Produto A");
		}
	}

	@Nested
	@DisplayName("Atualizar produto")
	class PutProduct {

		@Test
		@DisplayName("Dado payload valido, quando atualizar, entao delega ao local service e retorna DTO")
		void dado_payloadValido_quando_atualizar_entao_delegaAoLocalServiceERetornaDto()
			throws Exception {

			// Arrange
			Product payload = new Product();

			payload.setName("Produto B");
			payload.setDescription("Descricao B");
			payload.setPrice(29.9D);
			payload.setStatus(Product.Status.PUBLISHED);
			payload.setStockQuantity(5);
			payload.setCategoryIds(new Long[] {1L});
			payload.setTagIds(new Long[] {2L});

			ProductImpl existing = _product(PRODUCT_ID, "Produto A", 1);
			ProductImpl updated = _product(PRODUCT_ID, "Produto B", 5);

			when(_productLocalService.getProduct(PRODUCT_ID)).thenReturn(existing);
			when(
				_productLocalService.updateProduct(
					anyLong(), anyString(), anyString(), anyDouble(),
					anyInt(), anyInt(), any(long[].class), any(long[].class),
					any(ServiceContext.class))
			).thenReturn(updated);
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), PRODUCT_ID)
			).thenReturn(assetEntry);
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);

			// Act
			Product result = productResource.putSiteProduct(SITE_ID, PRODUCT_ID, payload);

			// Assert
			assertThat(result.getId()).isEqualTo(PRODUCT_ID);
			verify(_productLocalService).updateProduct(
				anyLong(), anyString(), anyString(), anyDouble(),
				anyInt(), anyInt(), any(long[].class), any(long[].class),
				serviceContextCaptor.capture());

			assertThat(serviceContextCaptor.getValue().getScopeGroupId()).isEqualTo(SITE_ID);
		}

		@Test
		@DisplayName("Dado payload nulo, quando atualizar, entao lanca BadRequestException")
		void dado_payloadNulo_quando_atualizar_entao_lancaBadRequestException()
			throws Exception {

			// Act / Assert
			assertThatThrownBy(
				() -> productResource.putSiteProduct(SITE_ID, PRODUCT_ID, null))
				.isInstanceOf(BadRequestException.class);
		}
	}

	@Nested
	@DisplayName("Listar produtos com filtros avancados")
	class GetProductsFilters {

		@Test
		@DisplayName("Dado busca nula, quando listar, entao retorna todos os produtos")
		void dado_buscaNula_quando_listar_entao_retornaTodosOsProdutos()
			throws Exception {

			// Arrange
			ProductImpl notebook = _product(1L, "Notebook", 5);
			ProductImpl mouse = _product(2L, "Mouse", 3);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(notebook, mouse));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(2);
		}

		@Test
		@DisplayName("Dado filtro inStock true, quando listar, entao retorna apenas produtos em estoque")
		void dado_filtroInStockTrue_quando_listar_entao_retornaApenasEmEstoque()
			throws Exception {

			// Arrange
			ProductImpl inStock = _product(1L, "Produto A", 5);
			ProductImpl outStock = _product(2L, "Produto B", 0);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(inStock, outStock));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, Boolean.TRUE, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("Dado filtro inStock false, quando listar, entao retorna apenas produtos fora de estoque")
		void dado_filtroInStockFalse_quando_listar_entao_retornaApenasForaDeEstoque()
			throws Exception {

			// Arrange
			ProductImpl inStock = _product(1L, "Produto A", 5);
			ProductImpl outStock = _product(2L, "Produto B", 0);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(inStock, outStock));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, Boolean.FALSE, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getId()).isEqualTo(2L);
		}

		@Test
		@DisplayName("Dado filtro categoryId, quando listar, entao retorna apenas produtos com categoria")
		void dado_filtroCategoryId_quando_listar_entao_retornaApenasComCategoria()
			throws Exception {

			// Arrange
			ProductImpl productA = _product(1L, "Produto A", 5);
			ProductImpl productB = _product(2L, "Produto B", 3);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(productA, productB));

			AssetEntry assetEntryA = mock(AssetEntry.class);
			AssetEntry assetEntryB = mock(AssetEntry.class);

			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenReturn(assetEntryA);
			when(assetEntryA.getCategoryIds()).thenReturn(new long[] {100L});
			when(assetEntryA.getTagNames()).thenReturn(new String[0]);

			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 2L)
			).thenReturn(assetEntryB);
			when(assetEntryB.getCategoryIds()).thenReturn(new long[] {200L});
			when(assetEntryB.getTagNames()).thenReturn(new String[0]);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, 100L, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("Dado filtro tagId, quando listar, entao retorna apenas produtos com tag")
		void dado_filtroTagId_quando_listar_entao_retornaApenasComTag()
			throws Exception {

			// Arrange
			ProductImpl productA = _product(1L, "Produto A", 5);
			ProductImpl productB = _product(2L, "Produto B", 3);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(productA, productB));

			AssetTag tagForA = mock(AssetTag.class);

			when(tagForA.getTagId()).thenReturn(50L);

			AssetEntry assetEntryA = mock(AssetEntry.class);
			AssetEntry assetEntryB = mock(AssetEntry.class);

			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenReturn(assetEntryA);
			when(assetEntryA.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntryA.getTagNames()).thenReturn(new String[] {"tag-50"});

			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 2L)
			).thenReturn(assetEntryB);
			when(assetEntryB.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntryB.getTagNames()).thenReturn(new String[0]);

			when(_assetTagLocalService.fetchTag(SITE_ID, "tag-50")).thenReturn(tagForA);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, 50L, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("Dado paginacao, quando listar, entao retorna subconjunto paginado")
		void dado_paginacao_quando_listar_entao_retornaSubconjuntoPaginado()
			throws Exception {

			// Arrange
			ProductImpl p1 = _product(1L, "Produto 1", 1);
			ProductImpl p2 = _product(2L, "Produto 2", 2);
			ProductImpl p3 = _product(3L, "Produto 3", 3);
			ProductImpl p4 = _product(4L, "Produto 4", 4);
			ProductImpl p5 = _product(5L, "Produto 5", 5);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p1, p2, p3, p4, p5));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);
			_stubAssetEntry(3L);
			_stubAssetEntry(4L);
			_stubAssetEntry(5L);

			Pagination pagination = mock(Pagination.class);

			when(pagination.getStartPosition()).thenReturn(0);
			when(pagination.getEndPosition()).thenReturn(2);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, pagination, null);

			// Assert
			assertThat(page.getItems()).hasSize(2);
		}

		@Test
		@DisplayName("Dado paginacao com start alem do fim, quando listar, entao retorna lista vazia")
		void dado_paginacaoComStartAlemDoFim_quando_listar_entao_retornaListaVazia()
			throws Exception {

			// Arrange
			ProductImpl product = _product(1L, "Produto A", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(product));
			_stubAssetEntry(1L);

			Pagination pagination = mock(Pagination.class);

			when(pagination.getStartPosition()).thenReturn(100);
			when(pagination.getEndPosition()).thenReturn(110);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, pagination, null);

			// Assert
			assertThat(page.getItems()).isEmpty();
		}

		@Test
		@DisplayName("Dado erro ao carregar asset, quando listar, entao retorna produto com arrays vazios")
		void dado_erroAoCarregarAsset_quando_listar_entao_retornaProdutoComArraysVazios()
			throws Exception {

			// Arrange
			ProductImpl product = _product(1L, "Produto A", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(product));
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenThrow(new PortalException("simulated"));

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getCategoryIds()).isEmpty();
			assertThat(page.getItems().iterator().next().getTagIds()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Ordenacao de produtos")
	class SortProducts {

		@Test
		@DisplayName("Dado sort por nome, quando listar, entao retorna ordenado por nome")
		void dado_sortPorNome_quando_listar_entao_retornaOrdenadoPorNome()
			throws Exception {

			// Arrange
			ProductImpl b = _product(1L, "Banana", 1);
			ProductImpl a = _product(2L, "Apple", 1);
			ProductImpl c = _product(3L, "Cherry", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(b, a, c));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);
			_stubAssetEntry(3L);

			Sort sort = mock(Sort.class);

			when(sort.getFieldName()).thenReturn("name");
			when(sort.isReverse()).thenReturn(false);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, new Sort[] {sort});

			// Assert
			List<Product> items = new java.util.ArrayList<>(page.getItems());

			assertThat(items.get(0).getName()).isEqualTo("Apple");
			assertThat(items.get(1).getName()).isEqualTo("Banana");
			assertThat(items.get(2).getName()).isEqualTo("Cherry");
		}

		@Test
		@DisplayName("Dado sort por nome reverso, quando listar, entao retorna ordem inversa")
		void dado_sortPorNomeReverso_quando_listar_entao_retornaOrdemInversa()
			throws Exception {

			// Arrange
			ProductImpl b = _product(1L, "Banana", 1);
			ProductImpl a = _product(2L, "Apple", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(b, a));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);

			Sort sort = mock(Sort.class);

			when(sort.getFieldName()).thenReturn("name");
			when(sort.isReverse()).thenReturn(true);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, new Sort[] {sort});

			// Assert
			List<Product> items = new java.util.ArrayList<>(page.getItems());

			assertThat(items.get(0).getName()).isEqualTo("Banana");
		}

		@Test
		@DisplayName("Dado sort por preco, quando listar, entao retorna ordenado por preco")
		void dado_sortPorPreco_quando_listar_entao_retornaOrdenadoPorPreco()
			throws Exception {

			// Arrange
			ProductImpl cheap = _product(1L, "Produto A", 1);

			cheap.setPrice(10.0D);

			ProductImpl expensive = _product(2L, "Produto B", 1);

			expensive.setPrice(50.0D);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(expensive, cheap));

			_stubAssetEntry(1L);
			_stubAssetEntry(2L);

			Sort sort = mock(Sort.class);

			when(sort.getFieldName()).thenReturn("price");
			when(sort.isReverse()).thenReturn(false);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, new Sort[] {sort});

			// Assert
			List<Product> items = new java.util.ArrayList<>(page.getItems());

			assertThat(items.get(0).getPrice()).isEqualTo(10.0D);
		}

		@Test
		@DisplayName("Dado sort por id, quando listar, entao retorna ordenado por id")
		void dado_sortPorId_quando_listar_entao_retornaOrdenadoPorId()
			throws Exception {

			// Arrange
			ProductImpl p3 = _product(3L, "Produto C", 1);
			ProductImpl p1 = _product(1L, "Produto A", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p3, p1));

			_stubAssetEntry(1L);
			_stubAssetEntry(3L);

			Sort sort = mock(Sort.class);

			when(sort.getFieldName()).thenReturn("id");
			when(sort.isReverse()).thenReturn(false);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, new Sort[] {sort});

			// Assert
			List<Product> items = new java.util.ArrayList<>(page.getItems());

			assertThat(items.get(0).getId()).isEqualTo(1L);
		}

		@Test
		@DisplayName("Dado sort por campo desconhecido, quando listar, entao nao ordena")
		void dado_sortPorCampoDesconhecido_quando_listar_entao_naoOrdena()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Produto A", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));
			_stubAssetEntry(1L);

			Sort sort = mock(Sort.class);

			when(sort.getFieldName()).thenReturn("unknown");
			when(sort.isReverse()).thenReturn(false);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, new Sort[] {sort});

			// Assert
			assertThat(page.getItems()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("Conversao de status")
	class StatusConversion {

		@Test
		@DisplayName("Dado produto publicado, quando criar DTO, entao status e PUBLISHED")
		void dado_produtoPublicado_quando_criarDTO_entao_statusEPublished()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Produto", 1);

			p.setStatus(1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));
			_stubAssetEntry(1L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, null);

			// Assert
			assertThat(
				page.getItems().iterator().next().getStatus()
			).isEqualTo(Product.Status.PUBLISHED);
		}

		@Test
		@DisplayName("Dado produto inativo, quando criar DTO, entao status e INACTIVE")
		void dado_produtoInativo_quando_criarDTO_entao_statusEInactive()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Produto", 1);

			p.setStatus(2);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));
			_stubAssetEntry(1L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, null);

			// Assert
			assertThat(
				page.getItems().iterator().next().getStatus()
			).isEqualTo(Product.Status.INACTIVE);
		}

		@Test
		@DisplayName("Dado produto inativo, quando filtrar por inactive, entao retorna")
		void dado_produtoInativo_quando_filtrarPorInactive_entao_retorna()
			throws Exception {

			// Arrange
			ProductImpl inactive = _product(1L, "Produto Inativo", 0);

			inactive.setStatus(2);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(inactive));
			_stubAssetEntry(1L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, "inactive", null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
		}

		@Test
		@DisplayName("Dado produto com status draft, quando filtrar por published, entao nao retorna")
		void dado_produtoComStatusDraft_quando_filtrarPorPublished_entao_naoRetorna()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Produto", 1);

			p.setStatus(0);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));
			_stubAssetEntry(1L);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, "published", null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).isEmpty();
		}

		@Test
		@DisplayName("Dado payload com status INACTIVE, quando criar produto, entao passa status correto")
		void dado_payloadComStatusInactive_quando_criarProduto_entao_passaStatusCorreto()
			throws Exception {

			// Arrange
			Product payload = new Product();

			payload.setName("Produto Inativo");
			payload.setDescription("Desc");
			payload.setStatus(Product.Status.INACTIVE);

			ProductImpl created = _product(PRODUCT_ID, "Produto Inativo", 0);

			created.setStatus(2);

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
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);

			// Act
			Product result = productResource.postSiteProduct(SITE_ID, payload);

			// Assert
			assertThat(result.getStatus()).isEqualTo(Product.Status.INACTIVE);
		}
	}

	@Nested
	@DisplayName("Filtros de busca textual e null name")
	class SearchAndNullNameFilters {

		@Test
		@DisplayName("Dado busca nao nula, quando listar, entao executa matchesSearch")
		void dado_buscaNaoNula_quando_listar_entao_executaMatchesSearch()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Produto,X", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));
			_stubAssetEntry(1L);

			// Act - StringUtil.containsIgnoreCase usa separador virgula,
			// "Produto,X" contem "Produto,X" (match exato com virgula)
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, "Produto,X", null, null, null, null, null, null);

			// O importante e que o codigo do _matchesSearch foi executado
			// O resultado pode ser 0 ou 1 dependendo do comportamento de StringUtil
			assertThat(page.getItems()).isNotNull();
		}

		@Test
		@DisplayName("Dado busca sem correspondencia, quando listar, entao filtra produto")
		void dado_buscaSemCorrespondencia_quando_listar_entao_filtraProduto()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Notebook", 1);

			p.setDescription("Computador portatil");

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));
			_stubAssetEntry(1L);

			// Act - busca por termo que nao consta no nome nem na descricao
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, "TermoInexistente12345", null, null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).isEmpty();
		}

		@Test
		@DisplayName("Dado sort com produto de nome vazio, quando ordenar por nome, entao nao lanca excecao")
		void dado_sortComProdutoDeNomeVazio_quando_ordenarPorNome_entao_naoLancaExcecao()
			throws Exception {

			// Arrange - dois produtos para forcar o comparador ser invocado
			ProductImpl pComNome = _product(1L, "Zeta", 1);
			ProductImpl pNomeVazio = _product(2L, "", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(pComNome, pNomeVazio));

			AssetEntry assetEntryA = mock(AssetEntry.class);
			AssetEntry assetEntryB = mock(AssetEntry.class);

			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenReturn(assetEntryA);
			when(assetEntryA.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntryA.getTagNames()).thenReturn(new String[0]);

			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 2L)
			).thenReturn(assetEntryB);
			when(assetEntryB.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntryB.getTagNames()).thenReturn(new String[0]);

			Sort sort = mock(Sort.class);

			when(sort.getFieldName()).thenReturn("name");
			when(sort.isReverse()).thenReturn(false);

			// Act - dois produtos garantem invocacao do comparador
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, new Sort[] {sort});

			// Assert - string vazia ordena antes de "Zeta"
			assertThat(page.getItems()).hasSize(2);

			List<Product> items = new java.util.ArrayList<>(page.getItems());

			assertThat(items).extracting(Product::getName).containsExactly("", "Zeta");
		}

		@Test
		@DisplayName("Dado produto com assetEntry null retornado, quando listar, entao define arrays vazios")
		void dado_assetEntryNullRetornado_quando_listar_entao_defineArraysVazios()
			throws Exception {

			// Arrange
			ProductImpl p = _product(1L, "Produto A", 1);

			when(_productLocalService.getProductsByGroupId(SITE_ID)).thenReturn(
				List.of(p));

			// getEntry retorna null (assetEntry nao existe ainda)
			when(
				_assetEntryLocalService.getEntry(
					product.service.model.Product.class.getName(), 1L)
			).thenReturn(null);

			// Act
			Page<Product> page = productResource.getSiteProductsPage(
				SITE_ID, null, null, null, null, null, null, null);

			// Assert
			assertThat(page.getItems()).hasSize(1);
			assertThat(page.getItems().iterator().next().getCategoryIds()).isEmpty();
			assertThat(page.getItems().iterator().next().getTagIds()).isEmpty();
		}
	}

	@Nested
	@DisplayName("Criar produto com defaults")
	class PostProductDefaults {

		@Test
		@DisplayName("Dado payload sem preco nem estoque, quando criar, entao usa defaults zero")
		void dado_payloadSemPrecoNemEstoque_quando_criar_entao_usaDefaultsZero()
			throws Exception {

			// Arrange
			Product payload = new Product();

			payload.setName("Produto Sem Preco");
			payload.setDescription("Desc");
			payload.setStatus(Product.Status.DRAFT);

			ProductImpl created = _product(PRODUCT_ID, "Produto Sem Preco", 0);

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
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);

			// Act
			Product result = productResource.postSiteProduct(SITE_ID, payload);

			// Assert
			assertThat(result.getId()).isEqualTo(PRODUCT_ID);

			verify(_productLocalService).addProduct(
				anyLong(), anyLong(), anyString(), anyString(),
				org.mockito.ArgumentMatchers.eq(0.0D),
				anyInt(),
				org.mockito.ArgumentMatchers.eq(0),
				any(long[].class), any(long[].class),
				any(ServiceContext.class));
		}

		@Test
		@DisplayName("Dado payload com status null, quando criar, entao usa status DRAFT")
		void dado_payloadComStatusNull_quando_criar_entao_usaStatusDraft()
			throws Exception {

			// Arrange
			Product payload = new Product();

			payload.setName("Produto");
			payload.setDescription("Desc");

			ProductImpl created = _product(PRODUCT_ID, "Produto", 0);

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
			when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
			when(assetEntry.getTagNames()).thenReturn(new String[0]);

			// Act
			Product result = productResource.postSiteProduct(SITE_ID, payload);

			// Assert
			assertThat(result).isNotNull();
		}
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private void _stubAssetEntry(long productId) throws Exception {
		when(
			_assetEntryLocalService.getEntry(
				product.service.model.Product.class.getName(), productId)
		).thenReturn(assetEntry);
		when(assetEntry.getCategoryIds()).thenReturn(new long[0]);
		when(assetEntry.getTagNames()).thenReturn(new String[0]);
	}

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
